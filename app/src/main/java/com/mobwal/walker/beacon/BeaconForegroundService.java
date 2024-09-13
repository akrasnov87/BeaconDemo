package com.mobwal.walker.beacon;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.mobwal.walker.beautil.v1.BeaconDataProvider;
import com.mobwal.walker.beautil.v1.BeaconUtil;
import com.mobwal.walker.beautil.v1.DateUtil;
import com.mobwal.walker.beautil.v1.LocalSave;
import com.mobwal.walker.beautil.v1.OnBeaconListener;
import com.mobwal.walker.beautil.v1.ServerBeaconDataProvider;
import com.mobwal.walker.beautil.v1.wBeaconManager;
import com.mobwal.walker.beautil.v1.BeaconSetting;
import com.mobwal.walker.beautil.v1.wBeacon;

import org.altbeacon.beacon.BeaconParser;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

public class BeaconForegroundService extends Service
        implements OnBeaconListener {

    public class MyBinder extends Binder {
        BeaconForegroundService getService() {
            return BeaconForegroundService.this;
        }
    }

    private BeaconDataProvider beaconDataProvider;
    private LocalSave localSave;

    private final IBinder myBinder = new MyBinder();

    public static final int NOTIFICATION_ID = 3;

    private static final String TAG = "BEACON_FOREGROUND_SERVICE";
    public static final String CHANNEL_ID = "BEACON_FOREGROUND_SERVICE_CHANNEL";

    public static String CHANNEL_NAME = "Data sync";
    private NotificationManagerCompat notificationManager;

    public String getDefaultContent() {
        return "...";
    }

    private wBeaconManager wBeaconManager;
    private double[] mCurrentPosition;

    // https://stacktuts.com/how-to-get-data-from-service-to-activity-in-android
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        wBeacon[] beacons = (wBeacon[]) intent.getSerializableExtra("beacons");
        if(beacons == null) {
            Log.d(TAG, "beacons not found");
            return Service.START_STICKY;
        }

        beaconDataProvider = new ServerBeaconDataProvider(this, Names.getConnectUrl(), "0.0.0.0");
        localSave = new LocalSave(this, "tracker.csv");

        wBeaconManager = new wBeaconManager(this, beacons);
        wBeaconManager.addRegion(BeaconUtil.getRegions(beacons, 1).get(0));
        wBeaconManager.setBeaconListener(this);

        BeaconSetting setting = new BeaconSetting();

        wBeaconManager.setBeaconSetting(setting);
        wBeaconManager.setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT);

        wBeaconManager.startMonitor();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                stopSelf();
                return Service.START_STICKY;
            }
        }

        if (notificationManager == null) {
            notificationManager = NotificationManagerCompat.from(this);
        }

        notificationManager.createNotificationChannel(Objects.requireNonNull(ForegroundServiceUtil.getNotificationChannel(this, CHANNEL_ID, CHANNEL_NAME)));

        try {
            Notification notification = ForegroundServiceUtil.getNotification(this, CHANNEL_ID, R.mipmap.ic_launcher_round, CHANNEL_NAME, getDefaultContent());
            notificationManager.notify(NOTIFICATION_ID, notification);

            int type = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            }

            ServiceCompat.startForeground(
                    /* service = */ this,
                    /* id = */ NOTIFICATION_ID, // Cannot be 0
                    /* notification = */ notification,
                    /* foregroundServiceType = */ type
            );
        } catch (Exception e) {
            Log.e(TAG, String.valueOf(e));
        }

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        wBeaconManager.destroy();
        super.onDestroy();
    }

    public double[] getData() {
        Log.d(TAG, "Передача информации о beacon");

        return this.mCurrentPosition;
    }

    @Override
    public void onCalculatePosition(double[] position) {
        if(position != null) {
            mCurrentPosition = position;

            try {
                String line = position[0] + ";" + position[1] + ";" + DateUtil.convertDateToUserString(new Date(), "yyyy-MM-dd") + ";" + DateUtil.convertDateToUserString(new Date(), "HH:mm:ss.SSS");
                localSave.writeLine(line.getBytes());

                String token = beaconDataProvider.auth("a-krasnov@it-serv.ru", "12345");
                beaconDataProvider.push(token);

                Log.d(TAG, "current position x=" + position[0] + " y=" + position[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.d(TAG, "current position unknown");
        }
    }
}
