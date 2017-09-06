package cc.easyandroid.easyvpn;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


//import com.squareup.leakcanary.LeakCanary;

/**
 * app
 */
public class EasyVpnApplication extends Application {
    DatabaseReference appinfoDatabaseReference;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);
        appinfoDatabaseReference = firebaseDatabase.getReference("appinfo");
    }

    public DatabaseReference getDatabaseReference() {
        return appinfoDatabaseReference;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
