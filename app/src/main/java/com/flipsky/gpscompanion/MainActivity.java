package com.flipsky.gpscompanion;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int LOCATION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("Flipsky GPS Companion");
        title.setTextSize(24);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("GPS-nopeusmittari\n\nSovellus käyttää puhelimen GPS:ää eikä muodosta Bluetooth-yhteyttä Flipsky-ohjaimeen.");
        info.setTextSize(16);
        layout.addView(info);

        Button start = new Button(this);
        start.setText("Käynnistä nopeusmittari");
        layout.addView(start);

        start.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_REQUEST
                );
                return;
            }

            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
                return;
            }

            startForegroundService(new Intent(this, GpsOverlayService.class));
        });

        setContentView(layout);
    }
}
