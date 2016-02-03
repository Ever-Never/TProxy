package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import android.content.Context;

import sii.uniroma2.HonorineCevallos.TProxy.Connectivity.AddressHelper;
import sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Packet;
import sii.uniroma2.HonorineCevallos.TProxy.exceptions.UnInizializedLogException;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jesus on 19/01/2016.
 * Manages the log.
 */
public class LogManager {

    public static LogManager instance;
    private File logFile;
    private   String filename;
    private FileOutputStream outputStream;
    private Context appContext;
    private SimpleDateFormat sdf;
    private AddressHelper connManager;
    private static final String cachedirname = "cache/";

    public static void setInstance(Context _appContext){
        if(instance == null){
            instance = new LogManager(_appContext);
        }
    }

    public static LogManager getInstance() throws UnInizializedLogException {
        if(instance != null){
            return instance;
        }else throw new UnInizializedLogException();
    }


    private LogManager(Context _appContext){

        File cachedir = new File(cachedirname);
        if (!cachedir.exists()){cachedir.mkdir();}
        this.sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        this.filename = "InterLog_"+System.currentTimeMillis();
        this.appContext = _appContext;
        this.logFile = new File(this.cachedirname,this.filename);
    }

    public void writetoLog(String tag, String string){
        try {
            this.outputStream = this.appContext.openFileOutput(this.filename, Context.MODE_PRIVATE);
            this.outputStream.write(tag.getBytes());
            this.outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writePacketInfo(Packet packet){
        try {
            this.outputStream = this.appContext.openFileOutput(this.filename, Context.MODE_PRIVATE);
            this.outputStream.write(("Intercepted Packet: ").getBytes());
            this.outputStream.write(("Time: "+sdf.format(new Date())).getBytes());

            this.outputStream.write(("Layer2 Header: "+packet.ip4Header.toString()).getBytes());
            this.outputStream.write(("Layer3 Header: "+packet.payload.headers[0].toString()).getBytes());

            this.outputStream.write(("Connectivity: "+ connManager.getStringConnType() ).getBytes());

            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }





}
