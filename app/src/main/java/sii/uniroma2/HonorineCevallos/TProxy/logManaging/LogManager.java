package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
                strArr = str.split(",");
                if(strArr[2]=="IN" ){
                    message.isIncomming = true;
                }else if(strArr[2]=="OUT"){
                    message.isIncomming = false;
                }
                message.transportProtocol = strArr[3];
                message.Timestamp = strArr[4];
                message.connectivityType = strArr[5];
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
            GlobalAppState.capturesCount++;
            Log.d("w_p",GlobalAppState.capturesCount+"");

            FileWriter logWriter = new FileWriter(GlobalAppState.logFile);
            BufferedWriter out = new BufferedWriter(logWriter);

            out.write(",PAQ,");

            if(packet.isIncomming()) {
                out.write("IN,");
            }else {
                out.write("OUT,");
            }

            if(packet.isTCP()){
                out.write("TCP,");
                out.write("DestIP:"+packet.tcpHeader.toString()+",");

            }else if(packet.isUDP()){
                out.write("UDP,");
                out.write("DestIP:"+packet.udpHeader.toString()+",");

            }else{
                out.write("OTHER_TP,");
            }
            out.write(sdf.format(new Date())+",");

           // out.write("Layer3 Header: "+packet.payload.headers[0].toString());

           out.write(GlobalAppState.connectivityHelper.getStringConnType()+",");
            out.write("DestIP:"+packet.ip4Header.destinationAddress+",");

            out.newLine();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String readTxt(){
        String str;
        StringBuilder strBuilder = new StringBuilder();
        FileReader logReader;
        BufferedReader inputStream;
        try {

            logReader = new FileReader(GlobalAppState.logFile);
            inputStream = new BufferedReader(logReader);
            str = inputStream.readLine();
            if(str !=null ){
                strBuilder.append(str);
                strBuilder.append("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        retru
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
