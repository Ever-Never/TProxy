package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import sii.uniroma2.HonorineCevallos.TProxy.utils.GlobalAppState;

/**
 * Created by Jesus on 19/01/2016.
 * Manages the log.
 */
public class LogManager {
    private String TAG = "LogManager";
    private String filename;
    private FileOutputStream outputStream;
    private Context appContext;
    private SimpleDateFormat sdf;

    public LogManager(Context ctx){
        this.appContext = ctx;
        this.filename = GlobalAppState.logFilename;
        this.sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    }

    public synchronized void writePacketInfo(Packet packet){
        try {

            GlobalAppState.capturesCount++;
            Log.d("w_p",GlobalAppState.capturesCount+"");

            FileWriter logWriter = new FileWriter(GlobalAppState.logFile, true);
            BufferedWriter out = new BufferedWriter(logWriter);

            out.write(GlobalAppState.capturesCount+",");

            if(packet.isIncomming())  out.write("IN,");
            else out.write("OUT,");

            if(packet.isTCP()) out.write("TCP,");
            else if(packet.isUDP()) out.write("UDP,");
            else out.write("OTHER_TP,");

            out.write(sdf.format(new Date())+",");

            out.write(GlobalAppState.connectivityHelper.getStringConnType()+",");
            out.write(packet.ip4Header.destinationAddress+",");
            out.write(packet.ip4Header.sourceAddress+",");
            if(packet.isTCP()){
                if(packet.tcpHeader.isSYN()){out.write("SYN,");}
                if(packet.tcpHeader.isACK()){out.write("ACK,");}
                if(packet.tcpHeader.isFIN()){out.write("FIN,");}
                if(packet.tcpHeader.isRST()){out.write("RST,");}
            }
            out.newLine();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}
