package sii.uniroma2.HonorineCevallos.TProxy.logManaging;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import sii.uniroma2.HonorineCevallos.TProxy.R;

/**
 * Created by Jesus on 04/02/2016.
 */
public class PacketListAdapter extends ArrayAdapter<Message> {
    private String fileName;


    public PacketListAdapter(Context context, int packet_Layout, List<Message> packets) {
        super(context, packet_Layout, packets);
        this.fileName = GlobalAppState.logFilename;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position

        Message packet = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.closed_packet_layout , parent, false);
        }

        //ImageView incommingMessage = (ImageView)convertView.findViewById(R.id.ivIncommingMessage);
        //ImageView outgoingMessage = (ImageView)convertView.findViewById(R.id.ivOutgoingMessage);
        TextView tvbody = (TextView)convertView.findViewById(R.id.tvBodyMessage);

        /*
        if (packet.isIncomming) {
            incommingMessage.setVisibility(View.VISIBLE);
            outgoingMessage.setVisibility(View.GONE);
            tvbody.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        } else {
            outgoingMessage.setVisibility(View.VISIBLE);
            incommingMessage.setVisibility(View.GONE);
            tvbody.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        }*/

        Resources res = GlobalAppState.appContext.getResources();
        //    <string name="packet_info"> Proto: %1$s Timestamp: %2$s ConnMode: %3$s </string>
        String text = String.format(res.getString(R.string.packet_info), packet.transportProtocol,
                packet.Timestamp, packet.connectivityType, packet.destinationAddr);
        tvbody.setText(text);
        return convertView;

    }

}
