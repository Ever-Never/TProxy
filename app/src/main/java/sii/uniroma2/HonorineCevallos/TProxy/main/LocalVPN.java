package sii.uniroma2.HonorineCevallos.TProxy.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import sii.uniroma2.HonorineCevallos.TProxy.R;
import sii.uniroma2.HonorineCevallos.TProxy.core.LocalProxyServer;
import sii.uniroma2.HonorineCevallos.TProxy.logManaging.GlobalAppState;

/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
public class LocalVPN extends AppCompatActivity
{
    private static final int VPN_REQUEST_CODE = 0x0F;
    private boolean waitingForVPNStart;
    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (LocalProxyServer.BROADCAST_VPN_STATE.equals(intent.getAction()))
            {

                if (intent.getBooleanExtra("running", false))
                    waitingForVPNStart = false;
            }
        }
    };

    /** Registers the broadcast receiver to be vpnStateReceiver
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_vpn);
        settingViewComponents();
        waitingForVPNStart = false;
        LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
                new IntentFilter(LocalProxyServer.BROADCAST_VPN_STATE));
        GlobalAppState.setAppContext(this);
    }

    private void settingViewComponents(){
        final Button vpnButton = (Button)findViewById(R.id.vpn);
        final Button gotoCapturesButton = (Button)findViewById(R.id.gotoCapturesbutton);
        showCapturesButton(false);
        vpnButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startVPN();
            }
        });
        gotoCapturesButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                switchtoCaptures();
            }
        });
    }

    /**
     * Called when user clicks Start VPN Button
     * Creates an Intent and asigns it the value of the prepare() method invocation result.
     * The prepare() method returns when user accepts to start the vpn, after that,
     */
    private void startVPN()
    {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null)
            //start popup requesting user concensous,
            // after user acconsents it invokes automatically onActivityResult(VPN_REQUEST_CODE,RESULT_OK,null)
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        else
        //Already prepared or acconsented by user.
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
    }

    private void switchtoCaptures()
    {
        Intent intent = new Intent(this, CapturesActivity.class);
        intent.putExtra("LOG_FILE_NAME", GlobalAppState.logFilename);
        startActivity(intent);

    }

    /** If requestCode is VPN_REQUEST_CODE and resultCode is RESULT_OK then set
     * waitingForVPNStart to true, and start the service LocalProxyServer with this class context.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK)
        {
            waitingForVPNStart = true;

            startService(new Intent(this, LocalProxyServer.class));

            enableVpnButton(false);
            showCapturesButton(true);
        }
    }

    /**
     * Manage button enable state.
     */
    @Override
    protected void onResume() {
        super.onResume();
        enableVpnButton(!waitingForVPNStart && !LocalProxyServer.isRunning());
    }

    /**
     * @param enable
     */
    private void enableVpnButton(boolean enable)
    {
        final Button vpnButton = (Button) findViewById(R.id.vpn);
        if (enable)
        {
            vpnButton.setEnabled(true);
            vpnButton.setText(R.string.start_vpn);
        }
        else
        {
            vpnButton.setEnabled(false);
            vpnButton.setText(R.string.stop_vpn);
        }
    }

    private void showCapturesButton(boolean enable)
    {
        final Button gotoCapturesButton = (Button) findViewById(R.id.gotoCapturesbutton);
        if (enable)
        {
            gotoCapturesButton.setVisibility(View.VISIBLE);
        }
        else
        {
            gotoCapturesButton.setVisibility(View.GONE);

        }
    }
}