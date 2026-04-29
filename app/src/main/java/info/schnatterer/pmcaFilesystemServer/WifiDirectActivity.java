package info.schnatterer.pmcaFilesystemServer;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;

import com.sony.wifi.direct.DirectConfiguration;
import com.sony.wifi.direct.DirectManager;

import java.io.IOException;
import java.util.List;

public class WifiDirectActivity extends BaseActivity {
    public static final String MY_IP_ADDRESS = "192.168.122.1";

public static final String ASCII_ART =
        " d8b                                       d8,\n" +
        " 88P               d8P     d8P      `8P \n" +
        "d88         d888888Pd888888P     \n" +
        "888   d8888b  ?88'    ?88'    88b\n" +
        "?88  d8b_,dP  88P     88P     88P\n" +
        " 88b 88b         88b     88b    d88 \n" +
        "  88b`?888P'  `?8b    `?8b  d88' \n" + 
        "            [ L E T T I']        \n\n";

    private TextView textView;
    private android.widget.ScrollView scrollView;
    private WifiManager wifiManager;
    private DirectManager wifiDirectManager;
    private BroadcastReceiver wifiStateReceiver;
    private BroadcastReceiver wifiDirectStateReceiver;
    private BroadcastReceiver groupCreateSuccessReceiver;
    private BroadcastReceiver groupCreateFailureReceiver;
    private BroadcastReceiver stationConnectedReceiver;
    private BroadcastReceiver stationDisconnectedReceiver;
    private HttpServer httpServer;

    @Override
    // This seems to be a sony-specific value
    @SuppressLint("WrongConstant")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);

        textView = (TextView) findViewById(R.id.logView);
        scrollView = (android.widget.ScrollView) findViewById(R.id.logScrollView);
        textView.setText(ASCII_ART);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiDirectManager = (DirectManager) getApplicationContext().getSystemService(DirectManager.WIFI_DIRECT_SERVICE);

        wifiStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                wifiStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            }
        };

        wifiDirectStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                wifiDirectStateChanged(intent.getIntExtra(DirectManager.EXTRA_DIRECT_STATE, DirectManager.DIRECT_STATE_UNKNOWN));
            }
        };

        groupCreateSuccessReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                groupCreated((DirectConfiguration) intent.getParcelableExtra(DirectManager.EXTRA_DIRECT_CONFIG));
            }
        };

        groupCreateFailureReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                groupCreateFailed();
            }
        };

        stationConnectedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stationConnected(intent.getStringExtra(DirectManager.EXTRA_STA_ADDR));
            }
        };

        stationDisconnectedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                stationDisconnected(intent.getStringExtra(DirectManager.EXTRA_STA_ADDR));
            }
        };

        httpServer = new HttpServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        registerReceiver(wifiDirectStateReceiver, new IntentFilter(DirectManager.DIRECT_STATE_CHANGED_ACTION));
        registerReceiver(groupCreateSuccessReceiver, new IntentFilter(DirectManager.GROUP_CREATE_SUCCESS_ACTION));
        registerReceiver(groupCreateFailureReceiver, new IntentFilter(DirectManager.GROUP_CREATE_FAILURE_ACTION));
        registerReceiver(stationConnectedReceiver, new IntentFilter(DirectManager.STA_CONNECTED_ACTION));
        registerReceiver(stationDisconnectedReceiver, new IntentFilter(DirectManager.STA_DISCONNECTED_ACTION));
        wifiManager.setWifiEnabled(true);
        wifiDirectManager.setDirectEnabled(true);
        try {
            httpServer.start();
            log("Server started successfully");
        } catch (IOException e) {
            Logger.error("Failed to start HTTP Server: " + e.getMessage());
        }
        setAutoPowerOffMode(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiStateReceiver);
        unregisterReceiver(wifiDirectStateReceiver);
        unregisterReceiver(groupCreateSuccessReceiver);
        unregisterReceiver(groupCreateFailureReceiver);
        unregisterReceiver(stationConnectedReceiver);
        unregisterReceiver(stationDisconnectedReceiver);
        wifiDirectManager.setDirectEnabled(false);
        wifiManager.setWifiEnabled(false);
        httpServer.stop();
        setAutoPowerOffMode(true);
    }

    protected void wifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                log("Enabling wifi");
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                log("Wifi enabled");
                break;
        }
    }

    protected void wifiDirectStateChanged(int state) {
        switch (state) {
            case DirectManager.DIRECT_STATE_ENABLING:
                log("Enabling wifi direct");
                break;
            case DirectManager.DIRECT_STATE_ENABLED:
                wifiDirectEnabled();
                break;
        }
    }

    protected void wifiDirectEnabled() {
        log("Wifi direct enabled");
        List<DirectConfiguration> configurations = wifiDirectManager.getConfigurations();
        if (configurations.isEmpty()) {
            log("Error: No configurations found");
        } else {
            log("Creating Group");
            wifiDirectManager.startGo(configurations.get(configurations.size() - 1).getNetworkId());
        }
    }

    protected void groupCreated(DirectConfiguration configuration) {
        log("Group created");
        log("SSID: " + configuration.getSsid());
        log("Key: " + configuration.getPreSharedKey());
        log("IP: " + MY_IP_ADDRESS);
        log("Port: " + HttpServer.PORT);
        log("Server URL: http://" + MY_IP_ADDRESS + ":" + HttpServer.PORT + "/");
    }

    protected void groupCreateFailed() {
        log("Group create failed");
    }

    protected void stationConnected(String address) {
        log("Station connected: " + address);
    }

    protected void stationDisconnected(String address) {
        log("Station disconnected: " + address);
    }

    protected void log(final String msg) {
        Logger.info(msg);
        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.append(msg + "\n");
                scrollToBottom();
            }
        });
    }

    private void scrollToBottom() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(android.view.View.FOCUS_DOWN);
            }
        });
    }

}
