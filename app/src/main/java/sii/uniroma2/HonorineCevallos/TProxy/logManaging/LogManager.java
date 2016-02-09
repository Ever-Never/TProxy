package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import sii.uniroma2.HonorineCevallos.TProxy.PacketManager.Packet;

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

            if(packet.isIncomming()) {
                out.write("IN,");
            }else {
                out.write("OUT,");
            }

            if(packet.isTCP()){
                out.write("TCP,");

            }else if(packet.isUDP()){
                out.write("UDP,");

            }else{
                out.write("OTHER_TP,");
            }
            out.write(sdf.format(new Date())+",");

           // out.write("Layer3 Header: "+packet.payload.headers[0].toString());

            out.write(GlobalAppState.connectivityHelper.getStringConnType()+",");
            out.write(packet.ip4Header.destinationAddress+",");

            out.newLine();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /*public String readTxt(){
        String str;
        StringBuilder strBuilder = new StringBuilder();
        FileReader logReader;
        BufferedReader inputStream;
        try {

            logReader = new FileReader(GlobalAppState.logFile);
            inputStream = new BufferedReader(logReader);
            str = inputStream.readLine();
            do{
                if(str !=null ){
                strBuilder.append(str);
                strBuilder.append("\n");
                }
            }while(str !=null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return strBuilder.toString();
    }*/






    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }




}
