package sii.uniroma2.HonorineCevallos.TProxy.utils;

import android.app.Application;
import android.content.Context;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import sii.uniroma2.HonorineCevallos.TProxy.exceptions.NotWritableStorageException;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.LogManager;

/**
 * Created by Jesus on 03/02/2016.
 */
public class GlobalAppState extends Application{

    public static Context appContext;
    public static ConnectivityHelper connectivityHelper;
    public static String logFilename;
    public static long capturesCount = 0;
    private static SimpleDateFormat sdf;
    public static File logFile;

    public GlobalAppState(){
        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        logFilename = "InterLog_"+sdf.format(new Date());
    }

   public static void setAppContext(Context ctx){
       appContext = ctx;
       connectivityHelper = new ConnectivityHelper(appContext);
       try {
           createLogFile();
       } catch (NotWritableStorageException e) {
           e.printStackTrace();
       }

   }

    private static void createLogFile() throws NotWritableStorageException {
        File directory = appContext.getExternalFilesDir(null);

            if(LogManager.isExternalStorageWritable()){
            logFile = new File(directory, logFilename);
            }else{
                throw new NotWritableStorageException();
            }

    }


    public static void closeResources(Closeable... resources)
    {
        for (Closeable resource : resources)
        {
            try
            {
                resource.close();
            }
            catch (IOException e)
            {
                // Ignore
            }
        }
    }

}
