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

import android.app.PendingIntent;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Selector;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import sii.uniroma2.HonorineCevallos.TProxy.logManaging.Packet;
import sii.uniroma2.HonorineCevallos.TProxy.R;
import sii.uniroma2.HonorineCevallos.TProxy.utils.GlobalAppState;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.LogManager;
import sii.uniroma2.HonorineCevallos.TProxy.utils.ByteBufferPool;

/**
 * Called when users accepts to start the VPN.
 */
public class LocalProxyServer extends VpnService
{
    private static final String TAG = LocalProxyServer.class.getSimpleName();
    private static final String INTERCEPT_ROUTE = "0.0.0.0"; // Vogliamo intercettare ogni pacchetto
    private static final String TUN_IP_ADDR = "192.168.0.2";

    public static final String BROADCAST_VPN_STATE = "uniroma2.sii.proxyitm.VPN_STATE";

    private static boolean isRunning = false;

    private ParcelFileDescriptor vpnInterface = null;

    private PendingIntent pendingIntent;

    private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
    private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
    private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;
    private ExecutorService executorService;
    private Selector udpSelector;
    private Selector tcpSelector;
    private LogManager logmanager;



    @Override
    public void onCreate()
    {
        super.onCreate();
        isRunning = true;
        createTUN();
        try
        {

            initialize();

            startThreads();

            LocalBroadcastManager bmanager = LocalBroadcastManager.getInstance(this);
            bmanager.sendBroadcast(new Intent(BROADCAST_VPN_STATE).putExtra("running", true));
            Log.i(TAG, "Started");
        }
        catch (IOException e)
        {
            Log.e(TAG, "Errore durante la creazione dei thread dell'applicazione", e);
            cleanup();
        }
    }

    private void startThreads(){
        executorService = Executors.newFixedThreadPool(5);
        executorService.submit(new UDPInput(networkToDeviceQueue, udpSelector, logmanager));
        executorService.submit(new UDPOutput(deviceToNetworkUDPQueue, udpSelector, this, logmanager));
        executorService.submit(new TCPInput(networkToDeviceQueue, tcpSelector, logmanager));
        executorService.submit(new TCPOutput(deviceToNetworkTCPQueue, networkToDeviceQueue, tcpSelector, this, logmanager));
        executorService.submit(new TUNManager(vpnInterface.getFileDescriptor(),
                deviceToNetworkUDPQueue, deviceToNetworkTCPQueue, networkToDeviceQueue));
    }

    private void initialize() throws IOException {
        logmanager = new LogManager(GlobalAppState.appContext);
        udpSelector = Selector.open();
        tcpSelector = Selector.open();
        deviceToNetworkUDPQueue = new ConcurrentLinkedQueue<>();
        deviceToNetworkTCPQueue = new ConcurrentLinkedQueue<>();
        networkToDeviceQueue = new ConcurrentLinkedQueue<>();
    }


    /**
     * Invocata dalla fnuzinoe Oncreate()
     * Crea l'interfaccia TUN e aggiunge la routa nella routing table.
     * Si usa a classe Builder per ottenere un File Descriptor della
     la TUN interface. L'indirizzo IP, il DNS, e la tablle adi routing possono essere
     configurati con il Builder. Dobbiamo assicurarci che l'invocazione del metodo
     addRoute sia effettuata con corretteza, date che da questo dipende
     quali pacchetti verrano reindirizzati verso la TUN.
     */
    private void createTUN()
    {
        if (vpnInterface == null)
        {
            Builder builder = new Builder();
            builder.addAddress(TUN_IP_ADDR, 32);
            builder.addRoute(INTERCEPT_ROUTE, 0);
            builder.setSession(getString(R.string.app_name));
            builder.setConfigureIntent(pendingIntent);
            vpnInterface =builder.establish();

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    public static boolean isRunning()
    {
        return isRunning;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        isRunning = false;
        executorService.shutdownNow();
        cleanup();
        Log.i(TAG, "Stopped");
    }

    private void cleanup()
    {
        deviceToNetworkTCPQueue = null;
        deviceToNetworkUDPQueue = null;
        networkToDeviceQueue = null;
        ByteBufferPool.clear();
        GlobalAppState.closeResources(udpSelector, tcpSelector, vpnInterface);
    }


    /**
     *  Thread gestore dell'interfaccia TUN.
     *  Si incarica di gestire tutta l’informazione che viene dirottata
     *  verso l’interfaccia TUN, cioè tutta l’informazione che le applicazioni
     *  inviano verso la rete.
     *  Inoltre questo thread, nel suo main loop, riceve i pacchetti
     *  che li arrivano dalle socket protette che create che costituiscono
     *  i diversi tunnel verso la rete internet e inoltra queste informazioni
     *  alle applicazioni attraverso la medesima interfaccia TUN.
     */
    private static class TUNManager implements Runnable
    {
        private static final String TAG = TUNManager.class.getSimpleName();

        private FileDescriptor vpnFileDescriptor;

        private ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue;
        private ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue;
        private ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue;

        public TUNManager(FileDescriptor vpnFileDescriptor,
                          ConcurrentLinkedQueue<Packet> deviceToNetworkUDPQueue,
                          ConcurrentLinkedQueue<Packet> deviceToNetworkTCPQueue,
                          ConcurrentLinkedQueue<ByteBuffer> networkToDeviceQueue)
        {
            this.vpnFileDescriptor = vpnFileDescriptor;
            this.deviceToNetworkUDPQueue = deviceToNetworkUDPQueue;
            this.deviceToNetworkTCPQueue = deviceToNetworkTCPQueue;
            this.networkToDeviceQueue = networkToDeviceQueue;
        }


        private void forwardPacket(boolean dataSent, ByteBuffer bufferToNetwork) throws UnknownHostException {
            dataSent = true;
            bufferToNetwork.flip();
            Packet packet = new Packet(bufferToNetwork);
            packet.setIncomming(false);
            if (packet.isUDP())
            {
                deviceToNetworkUDPQueue.offer(packet);
            }
            else if (packet.isTCP())
            {
                deviceToNetworkTCPQueue.offer(packet);
            }
            else
            {
                Log.w(TAG, "Unknown packet type");
                Log.w(TAG, packet.ip4Header.toString());
                dataSent = false;
            }

        }

        private void deliverPacket(Boolean dataReceived, FileChannel vpnOutput) throws IOException {
            ByteBuffer bufferFromNetwork = networkToDeviceQueue.poll();

            if (bufferFromNetwork != null)
            {
                bufferFromNetwork.flip();

                while (bufferFromNetwork.hasRemaining())

                    vpnOutput.write(bufferFromNetwork);

                dataReceived = true;

                ByteBufferPool.release(bufferFromNetwork);
            }
            else
            {
                dataReceived = false;
            }
        }

        @Override
        public void run()
        {
            Log.i(TAG, "Started");
            /*I pacchetti inviati dalle App vengono sccodati in questo stream */
            FileChannel vpnInput = new FileInputStream(vpnFileDescriptor).getChannel();
            /*I pacchetti che si vuole consegnare alle app debbono venir accodati su questo stream*/
            FileChannel vpnOutput = new FileOutputStream(vpnFileDescriptor).getChannel();

            try
            {
                ByteBuffer bufferToNetwork = null;
                boolean dataSent = true;
                boolean dataReceived = false;
                while (!Thread.interrupted())
                {
                    if (dataSent) bufferToNetwork = ByteBufferPool.acquire();
                    else   bufferToNetwork.clear();

                    // TODO: Block when not connected
                    int readBytes = vpnInput.read(bufferToNetwork);

                    if (readBytes > 0) forwardPacket(dataSent, bufferToNetwork);
                    else dataSent = false;

                    deliverPacket(dataReceived, vpnOutput);

                    // TODO: Sleep-looping is not very battery-friendly, consider blocking instead
                    if (!dataSent && !dataReceived) Thread.sleep(10);
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
            finally
            {
               GlobalAppState.closeResources(vpnInput, vpnOutput);
            }

        }
    }
}