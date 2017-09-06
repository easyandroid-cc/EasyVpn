package cc.easyandroid.easyvpn.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import cc.easyandroid.easyrecyclerview.core.progress.EasyProgressRelativeLayout;
import cc.easyandroid.easyvpn.EasyVpnApplication;
import cc.easyandroid.easyvpn.R;
import cc.easyandroid.easyvpn.core.AppProxyManager;
import cc.easyandroid.easyvpn.core.LocalVpnService;
import cc.easyandroid.easyvpn.pojo.AppInfo;


public class EasyMainActivity extends BaseActivity implements CompoundButton.OnCheckedChangeListener, LocalVpnService.onStatusChangedListener {
    EasyProgressRelativeLayout content_main;
    CheckBox on_off;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_easy_main);
        initTitleBar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        content_main = (EasyProgressRelativeLayout) findViewById(R.id.content_main);
        on_off = (CheckBox) findViewById(R.id.on_off);
        on_off.setChecked(LocalVpnService.IsRunning);
        on_off.setOnCheckedChangeListener(this);
//        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
//        firebaseDatabase.setPersistenceEnabled(true);
        EasyVpnApplication vpnApplication = (EasyVpnApplication) getApplication();
        vpnApplication.getDatabaseReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    AppInfo responseInfo = dataSnapshot.getValue(AppInfo.class);
                    setProxyUrl(responseInfo.getProxyUrl());
                    content_main.showContentView();
                } catch (Exception e) {
                    onCancelled(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                content_main.showErrorView();
            }
        });
        //Pre-App Proxy
        if (AppProxyManager.isLollipopOrAbove) {
            new AppProxyManager(this);
        } else {
//            ((ViewGroup) findViewById(cc.easyandroid.easyvpn.R.id.AppSelectLayout).getParent()).removeView(findViewById(cc.easyandroid.easyvpn.R.id.AppSelectLayout));
//            ((ViewGroup) findViewById(cc.easyandroid.easyvpn.R.id.textViewAppSelectLine).getParent()).removeView(findViewById(cc.easyandroid.easyvpn.R.id.textViewAppSelectLine));
        }
        LocalVpnService.addOnStatusChangedListener(this);
    }


    private void startVPNService() {
        String ProxyUrl = readProxyUrl();
        System.out.println("cgp=" + ProxyUrl);
        if (!isValidUrl(ProxyUrl)) {
            Toast.makeText(this, "網絡異常", Toast.LENGTH_SHORT).show();
            on_off.post(new Runnable() {
                @Override
                public void run() {
                    on_off.setChecked(false);
                    on_off.setEnabled(true);
                }
            });
            return;
        }
        LocalVpnService.ProxyUrl = ProxyUrl;
        startService(new Intent(this, LocalVpnService.class));
    }

    boolean isValidUrl(String url) {
        try {
            if (url == null || url.isEmpty())
                return false;

            if (url.startsWith("ss://")) {//file path
                return true;
            } else { //url
                Uri uri = Uri.parse(url);
                if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))
                    return false;
                if (uri.getHost() == null)
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    String readProxyUrl() {
        SharedPreferences preferences = getSharedPreferences("shadowsocksProxyUrl", MODE_PRIVATE);
        return preferences.getString(CONFIG_URL_KEY, "");
    }

    void setProxyUrl(String ProxyUrl) {
        SharedPreferences preferences = getSharedPreferences("shadowsocksProxyUrl", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CONFIG_URL_KEY, ProxyUrl);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private static final String CONFIG_URL_KEY = "CONFIG_URL_KEY";

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//        if (isChecked) {
//            startVPNService();
//        } else {
//            LocalVpnService.IsRunning = false;
//        }

        if (LocalVpnService.IsRunning != isChecked) {
            on_off.setEnabled(false);
            if (isChecked) {
                Intent intent = LocalVpnService.prepare(this);
                if (intent == null) {
                    startVPNService();
                } else {
                    startActivityForResult(intent, START_VPN_SERVICE_REQUEST_CODE);
                }
            } else {
                LocalVpnService.IsRunning = false;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == START_VPN_SERVICE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                startVPNService();
            } else {
                on_off.setChecked(false);
                on_off.setEnabled(true);
//                onLogReceived("canceled.");
            }
            return;
        }

        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            String ProxyUrl = scanResult.getContents();
            if (isValidUrl(ProxyUrl)) {
                setProxyUrl(ProxyUrl);
//                textViewProxyUrl.setText(ProxyUrl);
            } else {
                Toast.makeText(EasyMainActivity.this, cc.easyandroid.easyvpn.R.string.err_invalid_url, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    private static final int START_VPN_SERVICE_REQUEST_CODE = 1985;

    @Override
    public void onStatusChanged(String status, Boolean isRunning) {
        on_off.setEnabled(true);
        on_off.setChecked(isRunning);
        onLogReceived(status);
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLogReceived(String logString) {

    }
}
