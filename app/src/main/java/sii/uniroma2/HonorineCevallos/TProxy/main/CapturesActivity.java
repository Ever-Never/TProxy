package sii.uniroma2.HonorineCevallos.TProxy.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import sii.uniroma2.HonorineCevallos.TProxy.R;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.GlobalAppState;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.LogManager;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.Message;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.PacketListAdapter;

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


      //  packetsListView = (ListView) findViewById(R.id.CapturedPackets_listView);
       // fillListView();

        TextView helloTxt = (TextView)findViewById(R.id.LogtextView);
        helloTxt.setText(logManager.readTxt());
    }

    private void fillListView(){

        List<Message> packets = new ArrayList<Message>();
        Message curr_packet;
        do{
            curr_packet = logManager.readPacketInfo();
            packets.add(logManager.readPacketInfo());
        }while(curr_packet!=null);


        PacketListAdapter itemsAdapter =
                new PacketListAdapter(this, packets);
        packetsListView.setAdapter(itemsAdapter);


    }


}
