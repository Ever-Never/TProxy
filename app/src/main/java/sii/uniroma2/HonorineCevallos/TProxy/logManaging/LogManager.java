package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import android.content.Context;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import sii.uniroma2.HonorineCevallos.TProxy.Connectivity.AddressHelper;
import sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Packet;
import sii.uniroma2.HonorineCevallos.TProxy.exceptions.AddressHelperException;

/**
 * Created by Jesus on 19/01/2016.
 * Manages the log.
 */
public class LogManager {

    private String filename;
    private FileOutputStream outputStream;
    private AddressHelper connManager;
    private Context appContext;
    private SimpleDateFormat sdf;

    public LogManager(Context ctx){
        this.appContext = ctx;
        this.filename = LogFileInfo.filename;
        this.sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        try {
            this.connManager = AddressHelper.getInstance();
        } catch (AddressHelperException e) {
            e.printStackTrace();
        }
    }



    public void writetoLog(String tag, String string){
        try {
            this.outputStream = appContext.openFileOutput(this.filename, Context.MODE_WORLD_READABLE);
            this.outputStream.write(tag.getBytes());
            this.outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public  void writePacketInfo(Packet packet){
        try {
            this.outputStream = appContext.openFileOutput(this.filename, Context.MODE_WORLD_READABLE);
            this.outputStream.write(("Intercepted Packet: ").getBytes());
            this.outputStream.write(("Time: "+sdf.format(new Date())).getBytes());

            this.outputStream.write(("Layer2 Header: "+packet.ip4Header.toString()).getBytes());
           // this.outputStream.write(("Layer3 Header: "+packet.payload.headers[0].toString()).getBytes());

           // this.outputStream.write(("Connectivity: "+ connManager.getStringConnType() ).getBytes());

            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }





}
