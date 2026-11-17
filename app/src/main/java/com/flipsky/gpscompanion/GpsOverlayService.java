package com.flipsky.gpscompanion;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class GpsOverlayService extends Service {

    private WindowManager windowManager;
    private GaugeView gauge;
    private LocationManager locationManager;
    private LocationListener listener;
    private float speed = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotification();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        gauge = new GaugeView(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            220, 220,
            Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 40;
        params.y = 180;

        gauge.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = params.x;
                        startY = params.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = startX + (int)(event.getRawX() - touchX);
                        params.y = startY + (int)(event.getRawY() - touchY);
                        windowManager.updateViewLayout(gauge, params);
                        return true;
                }
                return true;
            }
        });

        windowManager.addView(gauge, params);
        startGps();
    }

    private void createNotification() {
        String channel = "gps_companion";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel nc = new NotificationChannel(
                channel, "GPS-nopeusmittari",
                NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(nc);
        }

        Notification notification = new Notification.Builder(this, channel)
            .setContentTitle("Flipsky GPS Companion")
            .setContentText("GPS-nopeusmittari on käynnissä")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .build();

        startForeground(1, notification);
    }

    private void startGps() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                speed = location.hasSpeed()
                    ? location.getSpeed() * 3.6f : 0;
                gauge.invalidate();
            }
        };

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500,
                0,
                listener
            );
        } catch (SecurityException e) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (locationManager != null && listener != null) {
            locationManager.removeUpdates(listener);
        }

        if (windowManager != null && gauge != null) {
            windowManager.removeView(gauge);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class GaugeView extends View {
        private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GaugeView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(cx, cy) - 12;

            paint.setColor(Color.argb(220, 0, 35, 80));
            canvas.drawCircle(cx, cy, radius, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.rgb(0, 170, 255));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.FILL);

            for (int i = 0; i <= 20; i++) {
                double angle = Math.toRadians(135 + i * 13.5);
                float x1 = cx + (float)Math.cos(angle) * (radius - 15);
                float y1 = cy + (float)Math.sin(angle) * (radius - 15);
                float x2 = cx + (float)Math.cos(angle) * (radius - 5);
                float y2 = cy + (float)Math.sin(angle) * (radius - 5);

                paint.setColor(Color.WHITE);
                paint.setStrokeWidth(2);
                canvas.drawLine(x1, y1, x2, y2, paint);
            }

            float limited = Math.min(speed, 100);
            double needleAngle = Math.toRadians(135 + limited * 2.7);

            paint.setColor(Color.RED);
            paint.setStrokeWidth(4);
            canvas.drawLine(
                cx, cy,
                cx + (float)Math.cos(needleAngle) * (radius - 28),
                cy + (float)Math.sin(needleAngle) * (radius - 28),
                paint
            );

            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(30);
            canvas.drawText(String.format(java.util.Locale.US, "%.1f", speed),
                cx, cy + 12, paint);

            paint.setTextSize(12);
            canvas.drawText("km/h", cx, cy + 32, paint);
        }
    }
        }
