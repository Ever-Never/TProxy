package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import android.app.Application;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Jesus on 03/02/2016.
 */
public class LogFileInfo extends Application{


    public static String filename;
    private SimpleDateFormat sdf;
    private static final String logDirName = "logs/";

    public LogFileInfo(){
        this.sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        filename = logDirName+"InterLog_"+sdf.format(new Date());
    }


}
