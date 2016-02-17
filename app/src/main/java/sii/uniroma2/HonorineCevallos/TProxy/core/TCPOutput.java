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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import sii.uniroma2.HonorineCevallos.TProxy.utils.ByteBufferPool;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.Packet;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.Packet.TCPHeader;
import sii.uniroma2.HonorineCevallos.TProxy.utils.TCB;
import sii.uniroma2.HonorineCevallos.TProxy.utils.TCB.TCBStatus;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.LogManager;

public class TCPOutput implements Runnable
{
    private static final String TAG = TCPOutput.class.getSimpleName();

    private LocalProxyServer vpnService;
    private ConcurrentLinkedQueue<Packet> inputQueue;
    private ConcurrentLinkedQueue<ByteBuffer> delivertoAppsQueue;
    private Selector selector;
    private Random random = new Random();

    /*Modified by Honorne and Cevallos:
    * Logging support*/
    private LogManager logManager;

    public TCPOutput(ConcurrentLinkedQueue<Packet> inputQueue, ConcurrentLinkedQueue<ByteBuffer> DelivertoAppsQueue,
                     Selector selector, LocalProxyServer vpnService, LogManager _logManager)
    {
        this.inputQueue = inputQueue;
        this.delivertoAppsQueue = DelivertoAppsQueue;
        this.selector = selector;
        this.vpnService = vpnService;
        this.logManager = _logManager;
    }

    @Override
    public void run()
    {
        Log.i(TAG, "Started");
        try
        {

            Thread currentThread = Thread.currentThread();
            while (true)
            {
                Packet currentPacket;
                // TODO: Block when not connected
                do
                {
                    currentPacket = inputQueue.poll();
                    if (currentPacket != null){
                        break;
                    }
                    Thread.sleep(10);
                } while (!currentThread.isInterrupted());

                if (currentThread.isInterrupted())
                    break;

                ByteBuffer payloadBuffer = currentPacket.backingBuffer;
                currentPacket.backingBuffer = null;
                ByteBuffer responseBuffer = ByteBufferPool.acquire();

                InetAddress destinationAddress = currentPacket.ip4Header.destinationAddress;

                TCPHeader tcpHeader = currentPacket.tcpHeader;
                int destinationPort = tcpHeader.destinationPort;
                int sourcePort = tcpHeader.sourcePort;

                String ipAndPort = destinationAddress.getHostAddress() + ":" +
                        destinationPort + ":" + sourcePort;

                TCB tcb = TCB.getTCB(ipAndPort);

                if (tcb == null)
                    initializeConnection(ipAndPort, destinationAddress, destinationPort,
                            currentPacket, tcpHeader, responseBuffer);
                else if (tcpHeader.isSYN())
                    /*In questo caso stiamo inviando un SYN ma la connessione era già aperta*/
                    processDuplicateSYN(tcb, tcpHeader, responseBuffer);
                else if (tcpHeader.isRST())
                    /*Il client manda un RST*/
                    closeCleanly(tcb, responseBuffer);
                else if (tcpHeader.isFIN())
                    /**/
                    processFIN(tcb, tcpHeader, responseBuffer);
                else if (tcpHeader.isACK())
                    /**/
                    processACK(tcb, tcpHeader, payloadBuffer, responseBuffer);

                // XXX: cleanup later
                if (responseBuffer.position() == 0)
                    ByteBufferPool.release(responseBuffer);
                ByteBufferPool.release(payloadBuffer);
            }
        }
        catch (InterruptedException e)
        {
            Log.i(TAG, "Stopping");
        }
        catch (IOException e)
        {
            Log.e(TAG, e.toString(), e);
        }
        finally
        {
            TCB.closeAll();
        }
    }

    /**Quando non abbiamo un'istanza di TCB corrispondente all'ip e porta correnti, allora se ne crea
     * uno nuovo.
     * @param ipAndPort
     * @param destinationAddress
     * @param destinationPort
     * @param currentPacket
     * @param tcpHeader
     * @param responseBuffer
     * @throws IOException
     */
    private void initializeConnection(String ipAndPort, InetAddress destinationAddress, int destinationPort,
                                      Packet currentPacket, TCPHeader tcpHeader, ByteBuffer responseBuffer)
            throws IOException
    {
        /*Si è scelto di scrivere sul log soltanto i acchetti correspondenti alle nuove sessioni.*/
        //logManager.writePacketInfo(currentPacket);
        currentPacket.swapSourceAndDestination();
        /*Non sempre avremo un pacchetto SYN: si tenga in mente una connessione stabilita prima
         dell'attivazione dell'interfaccia virtuale*/
        if (tcpHeader.isSYN())
        {
            SocketChannel outputChannel = SocketChannel.open();
            outputChannel.configureBlocking(false);
            vpnService.protect(outputChannel.socket());

            TCB tcb = new TCB(ipAndPort, random.nextInt(Short.MAX_VALUE + 1),
                    tcpHeader.sequenceNumber, tcpHeader.sequenceNumber + 1,
                    tcpHeader.acknowledgementNumber, outputChannel, currentPacket);
            TCB.putTCB(ipAndPort, tcb);

            try
            {
                outputChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
                if (outputChannel.finishConnect())/*Questo metodo può ritornare true o false a sceonda del
                fatto che la connessione sia stata finalizzata o meno, e' il grande vantaggio delle socket non bloccanti*/
                {
                    tcb.status = TCBStatus.SYN_RECEIVED;

                      /*Di solito nei pacchetti SYN, il client ci invia il valore della MSS*/
                    if(tcb.referencePacket.tcpHeader.hasOptions){
                        if(tcb.referencePacket.tcpHeader.optionsAndPadding.hasMss){
                            tcb.currentSendingMss = tcb.referencePacket.tcpHeader.optionsAndPadding.mss;

                        }
                    }

                    /*Se siamo connessi con il server, allora possiamo inviare un SYN/ACK all'applicazione, corrispondetne
                     * al pacchetto  SYN che essa aveva inviato. */
                    //TODO: comunicare alle client APP il mss del proxy.
                    currentPacket.updateTCPIPBuffer(responseBuffer, (byte) (TCPHeader.SYN | TCPHeader.ACK),
                            tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                    tcb.mySequenceNum++; // SYN counts as a byte
                }
                else
                {
                    tcb.status = TCBStatus.SYN_SENT;
                    selector.wakeup();
                    tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_CONNECT, tcb);
                    return;
                }
            }
            catch (IOException e)
            {
                Log.e(TAG, "Connection error: " + ipAndPort, e);
                currentPacket.updateTCPIPBuffer(responseBuffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum, 0);
                TCB.closeTCB(tcb);
            }
        }
        else
        {

            currentPacket.updateTCPIPBuffer(responseBuffer, (byte) TCPHeader.RST,
                    0, tcpHeader.sequenceNumber + 1, 0);
            Log.d(TAG, "intercepted previous connection, generating RST " +ipAndPort );

        }
        delivertoAppsQueue.offer(responseBuffer);
    }

    private void processDuplicateSYN(TCB tcb, TCPHeader tcpHeader, ByteBuffer responseBuffer)
    {
        synchronized (tcb)
        {
            if (tcb.status == TCBStatus.SYN_SENT)
            {
                tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + 1;
                return;
            }
        }
        sendRST(tcb, 1, responseBuffer);
    }

    /**
     * @param tcb
     * @param tcpHeader
     * @param responseBuffer
     */
    private void processFIN(TCB tcb, TCPHeader tcpHeader, ByteBuffer responseBuffer)
    {
        synchronized (tcb)
        {
            Packet referencePacket = tcb.referencePacket;
            tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + 1;
            tcb.theirAcknowledgementNum = tcpHeader.acknowledgementNumber;

            if (tcb.waitingForNetworkData)
            {/*Anche se ha ricevuto un pacchetto FIN dal client,
            il proxy server non manda il segnale di FIN/ACK
            finchè non sia vuota la socket di entrata remota.
            */
                tcb.status = TCBStatus.CLOSE_WAIT;
                referencePacket.updateTCPIPBuffer(responseBuffer, (byte) TCPHeader.ACK,
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
            }
            else
            {/*Il Proxy server ha finito di mandare pachetti al client e quindi lo autorizza  a
            chiudere la connessione*/

                tcb.status = TCBStatus.LAST_ACK;
                referencePacket.updateTCPIPBuffer(responseBuffer, (byte) (TCPHeader.FIN | TCPHeader.ACK),
                        tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
                tcb.mySequenceNum++; // FIN counts as a byte
            }
        }
        delivertoAppsQueue.offer(responseBuffer);
    }

    /**
     * @param tcb
     * @param tcpHeader
     * @param payloadBuffer
     * @param responseBuffer
     * @throws IOException
     */
    private void processACK(TCB tcb, TCPHeader tcpHeader, ByteBuffer payloadBuffer, ByteBuffer responseBuffer) throws IOException
    {
        int payloadSize = payloadBuffer.limit() - payloadBuffer.position();

        synchronized (tcb)
        {
            SocketChannel outputChannel = tcb.channel;

            if (tcb.status == TCBStatus.SYN_RECEIVED)/*L'ack corrisponde all'ultimo messaggio dell'handshake*/
            {
                tcb.status = TCBStatus.ESTABLISHED;

                selector.wakeup();
                /*Una volta stabilita la conessione con il server, possiamo immettere OP_READ come  operazione
                * di interesse associata alla chiave del canale */
                tcb.selectionKey = outputChannel.register(selector, SelectionKey.OP_READ, tcb);
                tcb.waitingForNetworkData = true;
            }

            else if (tcb.status == TCBStatus.LAST_ACK)/*Si è già mandato il FIN verso il client*/
            {
                closeCleanly(tcb, responseBuffer);
                return;
            }

            if (payloadSize == 0) return;
            /* un ack vuoto, non dobbiamo modificare quindi i numeri di sequenza*/

            /*se waitingForNetworkData == true, vuol dire che avevamo svuotato l'informazione in entrata dalla
             * socket,  quindi dobbbiamo risettare l'operazione di interesse che si era messa a zero. */
            if (!tcb.waitingForNetworkData)
            {
                selector.wakeup();
                tcb.selectionKey.interestOps(SelectionKey.OP_READ);
                tcb.waitingForNetworkData = true;
            }

            // Mandiamo  tutta l'informazione sul buffer verso l'informazione verso il server remoto:
            try
            {
                while (payloadBuffer.hasRemaining())
                    outputChannel.write(payloadBuffer);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Network write error: " + tcb.ipAndPort, e);
                sendRST(tcb, payloadSize, responseBuffer);
                return;
            }

            // TODO: We don't expect out-of-order packets, but verify
            /*Abbiamo consegnato il pacchetto al server remoto. Ora però il reference packet del TCB
             *deve essere adeguatamente settato (il suo header tcp dev'essere settato per mandare un'ack
             *corretto al client. Si noti che mandiamo un ack vuoto al client. Questo potrebbe rappresentare una
              * mancanza di trasparenza, dato che il server remoto ci potrebbe aver messo dell'informazione sul pacchetto. */
            tcb.myAcknowledgementNum = tcpHeader.sequenceNumber + payloadSize;
            tcb.theirAcknowledgementNum = tcpHeader.acknowledgementNumber;
            Packet referencePacket = tcb.referencePacket;
            referencePacket.updateTCPIPBuffer(responseBuffer, (byte) TCPHeader.ACK, tcb.mySequenceNum, tcb.myAcknowledgementNum, 0);
        }
        delivertoAppsQueue.offer(responseBuffer);
    }

    private void sendRST(TCB tcb, int prevPayloadSize, ByteBuffer buffer)
    {
        tcb.referencePacket.updateTCPIPBuffer(buffer, (byte) TCPHeader.RST, 0, tcb.myAcknowledgementNum + prevPayloadSize, 0);
        delivertoAppsQueue.offer(buffer);
        TCB.closeTCB(tcb);
    }

    private void closeCleanly(TCB tcb, ByteBuffer buffer)
    {
        ByteBufferPool.release(buffer);
        TCB.closeTCB(tcb);
    }
}