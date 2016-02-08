package sii.uniroma2.HonorineCevallos.TProxy.main;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;

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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        packetsListView = (ListView) findViewById(R.id.CapturedPackets_listView);

        fillListView();
    }

    private void fillListView(){

        List<Message> packets = null;
        Message curr_packet;
        do{
            curr_packet = logManager.readPacketInfo();
            packets.add(logManager.readPacketInfo());
        }while(curr_packet!=null);


        PacketListAdapter itemsAdapter =
                new PacketListAdapter(this , packets);
        packetsListView.setAdapter(itemsAdapter);


    }


}
