package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import android.content.Context;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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





    /** a Message object containing intercepted message information.
     * @return null if there are no more packets.
     */
    public Message readPacketInfo(){
        Message message = null;
        String str;
        String[] strArr;
        FileReader logReader;
        BufferedReader inputStream;
        try {

            logReader = new FileReader(GlobalAppState.logFile);
            inputStream = new BufferedReader(logReader);
            message = new Message();
            str = inputStream.readLine();
            if(str !=null ){
                strArr = str.split("|");
                if(strArr[1]=="IN" ){
                    message.isIncomming = true;
                }else if(strArr[1]=="OUT"){
                    message.isIncomming = false;
                }
                message.transportProtocol = strArr[2];
                message.Timestamp = strArr[3];
                message.connectivityType = strArr[4];
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return message;

    }

    public synchronized void writePacketInfo(Packet packet){
        try {
            FileWriter logWriter = new FileWriter(GlobalAppState.logFile);
            BufferedWriter out = new BufferedWriter(logWriter);

            out.write("|PAQ|");

            if(packet.isOutgoing()) {
                out.write("OUT|");

            }else if(packet.isIncomming()){
                out.write("IN|");

            }else{
                out.write("NO_DIR|");

            }

            if(packet.isTCP()){
                out.write("TCP|");

            }else if(packet.isUDP()){
                out.write("UDP|");

            }else{
                out.write("OTHER_TP|");

            }
            out.write(sdf.format(new Date())+"|");

           // out.write("Layer3 Header: "+packet.payload.headers[0].toString());

           out.write(GlobalAppState.addressHelper.getStringConnType()+"|");
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

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


}
