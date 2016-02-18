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
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import sii.uniroma2.HonorineCevallos.TProxy.logManaging.GlobalAppState;
import sii.uniroma2.HonorineCevallos.TProxy.utils.ByteBufferPool;
import sii.uniroma2.HonorineCevallos.TProxy.Connectivity.ConnectivityHelper;
import sii.uniroma2.HonorineCevallos.TProxy.utils.LRUCache;
import sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Packet;
import sii.uniroma2.HonorineCevallos.TProxy.exceptions.AddressHelperException;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.LogManager;

public class UDPOutput implements Runnable
{
    private static final String TAG = UDPOutput.class.getSimpleName();

    private LocalProxyServer vpnService;
    private ConcurrentLinkedQueue<Packet> inputQueue;
    private LogManager logManager;
    private Selector selector;
    private static final int MAX_CACHE_SIZE = 50;
    private LRUCache<String, DatagramChannel> channelCache =
            new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<String, DatagramChannel>()
            {
                @Override
                public void cleanup(Map.Entry<String, DatagramChannel> eldest)
                {
                    closeChannel(eldest.getValue());
                }
            });

    public UDPOutput(ConcurrentLinkedQueue<Packet> inputQueue, Selector selector, LocalProxyServer vpnService, LogManager _logManager)
    {
        this.inputQueue = inputQueue;
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
                /*Se il modulo dovrebbe venir usato per prolongamenti di tempo grandi, allora sarebbe
                * utile bloccare i thread quando non ci sia connessione internet attiva. */
                do
                {
                    currentPacket = inputQueue.poll();
                    if (currentPacket != null){
                        currentPacket.setIncomming(false);
                        break;
                    }
                    Thread.sleep(10);
                } while (!currentThread.isInterrupted());

                if (currentThread.isInterrupted())
                    break;

                InetAddress destinationAddress = currentPacket.ip4Header.destinationAddress;
                int destinationPort = currentPacket.udpHeader.destinationPort;
                int sourcePort = currentPacket.udpHeader.sourcePort;
                /*Stiamo per inviare il pacchetto intercettato alla sua destinazione originale.
               */
                String ipAndPort = destinationAddress.getHostAddress() + ":" + destinationPort + ":" + sourcePort;
                /*Prendiamo il riferimento al canale corrispondente, se esiste,  dalla cache dei canali.*/
                DatagramChannel outputChannel = channelCache.get(ipAndPort);
                /*SE non esiste tale canale, ne dobbiamo creare uno nuovo*/
                if (outputChannel == null) {

                    /*Si è scelto di scrivere sul log soltanto i pacchetti correspondenti alle nuove sessioni.*/
                    logManager.writePacketInfo(currentPacket);

                    outputChannel = DatagramChannel.open();
                    /* modifica proposta da imhotepisinvisible
                     ( https://code.google.com/p/android/issues/detail?id=64819)
                    Le risposte DNS non arrivano al dispositivo se non viene prima fatto il bind ESPLICITO
                    della datagramsocket con l'indirizzo ip corrente della scheda di rete vera del dispositivo.
                    * */
                    InetSocketAddress sa = null;
                    ConnectivityHelper ah = GlobalAppState.connectivityHelper;
                    InetAddress ia= null;
                    try {
                        ia = ah.getIPAddress();
                    } catch (AddressHelperException e) {
                        e.printStackTrace();
                    }
                    sa = new InetSocketAddress( ia , sourcePort);

                    try {
                            outputChannel.socket().setReuseAddress(true);
                            outputChannel.socket().bind(sa);
                        } catch (BindException e) {
                            Log.d(TAG, sa.toString() + " " + e.toString(), e);
                        }
                    try
                    {
                        outputChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
                    }
                    catch (IOException e)
                    {
                        Log.e(TAG, "Connection error: " + ipAndPort, e);
                        closeChannel(outputChannel);
                        ByteBufferPool.release(currentPacket.backingBuffer);
                        continue;
                    }

                    /*Si noti anche che un channel viene ustao sia per scrivere che per leggere*/
                    outputChannel.configureBlocking(false);
                    currentPacket.swapSourceAndDestination();

                    selector.wakeup();
                    outputChannel.register(selector, SelectionKey.OP_READ, currentPacket);

                    vpnService.protect(outputChannel.socket());

                    channelCache.put(ipAndPort, outputChannel);
                }

                try
                {
                    ByteBuffer payloadBuffer = currentPacket.backingBuffer;
                    while (payloadBuffer.hasRemaining())
                        outputChannel.write(payloadBuffer);
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Network write error: " + ipAndPort, e);
                    channelCache.remove(ipAndPort);
                    closeChannel(outputChannel);
                }
                ByteBufferPool.release(currentPacket.backingBuffer);
            }
        }
        catch (InterruptedException e)
        {
            Log.i(TAG, "Stopping");
        }
        catch (IOException e)
        {
            Log.i(TAG, e.toString(), e);
        }
        finally
        {
            closeAll();
        }
    }

    private void closeAll()
    {
        Iterator<Map.Entry<String, DatagramChannel>> it = channelCache.entrySet().iterator();
        while (it.hasNext())
        {
            closeChannel(it.next().getValue());
            it.remove();
        }
    }

    private void closeChannel(DatagramChannel channel)
    {
        try
        {
            channel.close();
        }
        catch (IOException e)
        {
            // Ignore
        }
    }

}
