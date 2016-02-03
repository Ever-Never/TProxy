package sii.uniroma2.HonorineCevallos.TProxy.Connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.widget.Toast;

import sii.uniroma2.HonorineCevallos.TProxy.exceptions.AddressHelperException;
import sii.uniroma2.HonorineCevallos.TProxy.exceptions.ConnectivityManagerException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Created by Jesus on 15/12/2015.
 */
public class AddressHelper {

    private InetAddress myLocalIpAdress;
    private ConnectivityManager connMgr ;
    private Context superContext;
    private static AddressHelper singleton_Instance;

    private AddressHelper(Context ctx ){
        superContext = ctx;
        connMgr = (ConnectivityManager) superContext.getSystemService(superContext.CONNECTIVITY_SERVICE);
    }

    public static AddressHelper setInstance(Context ctx){
        if(singleton_Instance == null){
            singleton_Instance = new AddressHelper(ctx);
        }
        return singleton_Instance;
    }

    public static AddressHelper getInstance() throws AddressHelperException {
        if(singleton_Instance == null){
            throw new AddressHelperException();
        }
        return singleton_Instance;
    }

    public String getStringConnType() throws ConnectivityManagerException {
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                return "Wifi";
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return "Mobile";
            }else{
                throw new ConnectivityManagerException();
            }
        } else {
            return "Not connected to Internet";
        }
    }

    public InetAddress getIPAddress() throws AddressHelperException, IOException {
        NetworkInfo activeNetwork = connMgr.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                return getWifiPublicIpAddress();
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                return getMobileIpAddress();
            }else{
                throw new AddressHelperException();
            }
        } else {
            // not connected to the internet
            Toast.makeText(superContext, "Not connected to Internet", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private InetAddress getMobileIpAddress()throws IOException {

            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            //return inetAddress.getHostAddress().toString();
                            return inetAddress;
                        }
                    }
                }
            } catch (SocketException ex) {
            }

            return null;

        }





    private InetAddress getWifiPublicIpAddress() throws UnknownHostException {
        WifiManager wm = (WifiManager) superContext.getSystemService(superContext.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        InetAddress publicIPAddress = InetAddress.getByName(ip);
        return publicIPAddress;
    }


}
