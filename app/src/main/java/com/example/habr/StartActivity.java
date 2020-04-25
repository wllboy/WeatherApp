package com.example.habr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import java.util.Timer;
import java.util.TimerTask;

public class StartActivity extends AppCompatActivity {

    boolean isGpsNeeded;



    @Override
    protected void onResume() {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                final  LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

                assert locationManager != null;
                if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    isGpsNeeded = false;
                }

                assert wifiManager != null;
                if (wifiManager.isWifiEnabled() && !isGpsNeeded) {
                    startActivity(new Intent(StartActivity.this, MainActivity.class));
                    finish();
                }
            }
        };
        Timer timer = new Timer();
        timer.schedule(timerTask, 1500);

        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        checkIfWifiEnabled();
        statusCheck();
    }

    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        assert manager != null;
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        try {
            DB weatherDB = DBFactory.open(this,"weatherDB");
            if (!weatherDB.exists("city")) {
                isGpsNeeded = true;
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                finish();
                            }
                        });
                final AlertDialog alert = builder.create();
                alert.show();
            }
        } catch (SnappydbException e) {
            e.printStackTrace();
        }

    }

    private void checkIfWifiEnabled() {
        final WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert manager != null;
        if(!manager.isWifiEnabled()) {
            MaterialStyledDialog.Builder builder
                    = new MaterialStyledDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle("WiFi-подключение")
                    .setDescription("На вашем устройстве нет WiFi-соединения. Хотите его включить?")
                    .setPositiveText("Да")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    }).setNegativeText("Нет")
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .setHeaderDrawable(R.drawable.ic_wifi_black_24dp);
            builder.show();
        }
    }
}
