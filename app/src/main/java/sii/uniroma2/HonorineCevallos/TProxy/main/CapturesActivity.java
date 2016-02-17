package sii.uniroma2.HonorineCevallos.TProxy.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import sii.uniroma2.HonorineCevallos.TProxy.R;
import sii.uniroma2.HonorineCevallos.TProxy.utils.GlobalAppState;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.LogManager;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.Message;

public class CapturesActivity extends AppCompatActivity {

    private ListView packetsListView;
    private LogManager logManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        logManager = new LogManager(GlobalAppState.appContext);

        setContentView(R.layout.activity_captures);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        packetsListView = (ListView) findViewById(R.id.CapturedPackets_listView);
        fillListView();
        //showRawLogFile();


    }


  /*  public void showRawLogFile()
    {
        TextView helloTxt = (TextView)findViewById(R.id.LogtextView);
        FileReader logReader;
        BufferedReader reader;

        try {
            logReader = new FileReader(GlobalAppState.logFile);
            reader = new BufferedReader(logReader);

            String i;

            i = reader.readLine();
            while (i != null)
            {
                helloTxt.setText( helloTxt.getText()+"\n"+i );
                i = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
        }
    }*/


    public void fillListView(){

        List<Message> packets = new ArrayList<Message>();
        Message message = null;
        String str;
        String[] strArr;
        FileReader logReader;
        BufferedReader inputStream;
        try {

            logReader = new FileReader(GlobalAppState.logFile);
            inputStream = new BufferedReader(logReader);
            do {
                message = new Message();
                str = inputStream.readLine();
                if (str != null) {
                    strArr = str.split(",");
                    message.packetNumber = Integer.parseInt(strArr[0]);
                    if (strArr[1].equals("IN")) {
                        message.isIncomming = true;
                    } else if (strArr[1].equals("OUT")) {
                        message.isIncomming = false;
                    }
                    message.transportProtocol = strArr[2];
                    message.Timestamp = strArr[3];
                    message.connectivityType = strArr[4];
                    message.destinationAddr = strArr[5];
                    message.sourceAddr = strArr[6];
                    if(message.transportProtocol == "TCP")
                        message.TCPSpecialPacket = strArr[7];
                    packets.add(message);
                }
            }while(str != null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayAdapter<Message> itemsAdapter =
                new ArrayAdapter(this, R.layout.message_item, packets);
        packetsListView.setAdapter(itemsAdapter);

    }


}
