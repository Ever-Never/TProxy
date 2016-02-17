package sii.uniroma2.HonorineCevallos.TProxy.core;

/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import sii.uniroma2.HonorineCevallos.TProxy.logManaging.Packet;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.LogManager;
import sii.uniroma2.HonorineCevallos.TProxy.utils.ByteBufferPool;
import sii.uniroma2.HonorineCevallos.TProxy.utils.TCB;

public class TCPInput implements Runnable
{
    private static final String TAG = TCPInput.class.getSimpleName();
    private static final int HEADER_SIZE = Packet.IP4_HEADER_SIZE + Packet.TCP_HEADER_SIZE;

    private ConcurrentLinkedQueue<ByteBuffer> deliverToAppsQueue;
    private Selector selector;
    private LogManager logManager;

    public TCPInput(ConcurrentLinkedQueue<ByteBuffer> outputQueue, Selector selector, LogManager _logManager)
    {
        this.deliverToAppsQueue = outputQueue;
        this.selector = selector;
        this.logManager = _logManager;

    }

    @Override
    public void run()
    {
        try
        {
            Log.d(TAG, "Started");
            while (!Thread.interrupted())
            {

                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    Thread.sleep(10);
                    continue;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();


                while (keyIterator.hasNext() && !Thread.interrupted())
                {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid())
                    {
                        /*Per le socket che non hanno finito di fare la connect*/
                        if (key.isConnectable())  processConnect(key, keyIterator);
                        else if (key.isReadable()) processInput(key, keyIterator);
                    }
                }
            }
        }
        catch (InterruptedException e)
        {
            Log.i(TAG, "Stopping");
        }
        catch (IOException e)
        {
            Log.w(TAG, e.toString(), e);
        }
    }


    /** Implementazione del meccanismo SLIDING WINDOW DEL TCP.
     * @param tcb
     * @param referencePacket
     * @return
     */
    private int getreadableBytes(TCB tcb, Packet referencePacket){

        int mss = tcb.currentSendingMss;
        int sentButNotAcked = (int) (tcb.mySequenceNum-tcb.theirAcknowledgementNum);
        int readableBytes;

        if(referencePacket.tcpHeader.hasOptions){
            if(referencePacket.tcpHeader.optionsAndPadding.hasMss){
                mss = referencePacket.tcpHeader.optionsAndPadding.mss;
            }
        }
        readableBytes= mss-sentButNotAcked;
        return readableBytes;
    }

    private  void manageSliddingWindow(TCB tcb){
                 /*Di solito nei pacchetti SYN, il client ci invia il valore della MSS*/
        if(tcb.referencePacket.tcpHeader.hasOptions){
            if(tcb.referencePacket.tcpHeader.optionsAndPadding.hasMss){
                tcb.currentSendingMss = tcb.referencePacket.tcpHeader.optionsAndPadding.mss;

            }
        }
    }


    private void processHandShake(SelectionKey key, Iterator<SelectionKey> keyIterator, TCB tcb, Packet referencePacket){
        keyIterator.remove();
        tcb.status = TCB.TCBStatus.SYN_RECEIVED;
        manageSliddingWindow(tcb);

        ByteBuffer responseBuffer = ByteBufferPool.acquire();
        //sending a fake SYN/ACK to the client app...
        referencePacket.updateTCPIPBuffer(responseBuffer, (byte) (Packet.TCPHeader.SYN | Packet.TCPHeader.ACK),
                tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
        referencePacket.setIncomming(true);
        logManager.writePacketInfo(referencePacket);
        deliverToAppsQueue.offer(responseBuffer);
        tcb.mySequenceNum++; // SYN counts as a byte
        key.interestOps(SelectionKey.OP_READ);
    }
    /**
     * Invocchiamo questo metodo quando abbiamo un canale la cui operazione per cui è pronto è la connect()
     * Questo succede quando creiamo un socket verso un server remoto e invocchiamo la connect() ma l'invocazione
     * della funzione finishConnect() non ha avuto esito positivo.
     *
     * @param key
     * @param keyIterator
     */
    private void processConnect(SelectionKey key, Iterator<SelectionKey> keyIterator)
    {
        TCB tcb = (TCB) key.attachment();
        Packet referencePacket = tcb.referencePacket;

        try
        {
            /*è passato del tempo dall'ultima invocazione della finischConnect,
            * diamo un'altra opportunità alla connessione per stabilirsi*/
            if (tcb.channel.finishConnect()) processHandShake(key, keyIterator, tcb, referencePacket);
        }
        catch (IOException e)
        {
            Log.e(TAG, "Connection error: " + tcb.ipAndPort, e);
            ByteBuffer responseBuffer = ByteBufferPool.acquire();
            referencePacket.updateTCPIPBuffer(responseBuffer, (byte) Packet.TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
            deliverToAppsQueue.offer(responseBuffer);
            TCB.closeTCB(tcb);
        }
    }

    /**
     * @param key
     * @param keyIterator
     */
    private void processInput(SelectionKey key, Iterator<SelectionKey> keyIterator)
    {
        keyIterator.remove();
        ByteBuffer receiveBuffer = null;

        TCB tcb = (TCB) key.attachment();
        synchronized (tcb)
        {
            Packet referencePacket = tcb.referencePacket;

            int readableBytes = getreadableBytes(tcb, referencePacket);
            receiveBuffer = ByteBufferPool.acquire(readableBytes+ Packet.TCP_HEADER_SIZE+ Packet.IP4_HEADER_SIZE);
            // Si lascia lo spazio per poi scrivere gli HEADER
             receiveBuffer.position(HEADER_SIZE);

            SocketChannel inputChannel = (SocketChannel) key.channel();
            int readBytes;
            try
            {
                readBytes = inputChannel.read(receiveBuffer);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Network read error: " + tcb.ipAndPort, e);
                /*Niente grave solo risettiamo la connessione*/
                referencePacket.updateTCPIPBuffer(receiveBuffer, (byte) Packet.TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
                deliverToAppsQueue.offer(receiveBuffer);
                TCB.closeTCB(tcb);
                return;
            }

            if (readBytes == -1)
            {
                /*Per il momento non mi interessa a questo canale selezinato. Si aspetta ad avere più informazione
                da consegnare all'applicazione client.*/
                key.interestOps(0);
                tcb.waitingForNetworkData = false;

                if (tcb.status != TCB.TCBStatus.CLOSE_WAIT)/*se lo stato non è CLOSE_WAIT, si ritorna alla selezione dei canali*/
                {
                    ByteBufferPool.release(receiveBuffer);
                    return;
                }

                /*Se lo stato è proprio CLOSE_WAIT, allora si invia
                 la FIN al client e ci mettiamo nello stato LAST_ACK*/
                tcb.status = TCB.TCBStatus.LAST_ACK;
                referencePacket.updateTCPIPBuffer(receiveBuffer, (byte) Packet.TCPHeader.FIN, tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                tcb.mySequenceNum++; // FIN counts as a byte
            }
            else
            {/*Abbiamo dell'informazione da consegnare al client mettiamo il flag PSH perché se ci è stato consegnata
            dalla socket una quantità x di dati, allora anche noi dobbiamo consegnarla al client, si noti comunque
            che abbiamo già effettuato lo split d'accordo con la current mss impostata dal client*/
                referencePacket.updateTCPIPBuffer(receiveBuffer, (byte) (Packet.TCPHeader.PSH | Packet.TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, readBytes);
                tcb.mySequenceNum += readBytes; // Next sequence number
                /*Importante: Siccome abbiamo costruito il pacchetto */
                receiveBuffer.position(HEADER_SIZE + readBytes);
            }
        }
        deliverToAppsQueue.offer(receiveBuffer);
    }
}
