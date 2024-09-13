package com.mobwal.walker.beacon;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mobwal.walker.beautil.v1.BeaconDataProvider;
import com.mobwal.walker.beautil.v1.ServerBeaconDataProvider;
import com.mobwal.walker.beautil.v1.ui.Draw2D;
import com.mobwal.walker.beautil.v1.ui.SensorActivity;
import com.mobwal.walker.beautil.v1.wBeacon;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Принцип следующий система сначала включает сканирование метки, как только её обнаружили, то нужно вызовать слушатель который по ней вернёт данные
 *
 * https://github.com/AltBeacon/android-beacon-library
 * https://altbeacon.github.io/android-beacon-library/samples-java.html
 *
 */

public class MainActivity extends SensorActivity
    implements View.OnClickListener {

    private BeaconDataProvider beaconDataProvider;

    private Button btnService;
    private Draw2D draw2D;

    private BeaconForegroundService beaconForegroundService;
    private boolean isBound = false;

    private ServiceConnection myConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            BeaconForegroundService.MyBinder binder = (BeaconForegroundService.MyBinder) iBinder;
            beaconForegroundService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        beaconDataProvider = new ServerBeaconDataProvider(this, Names.getConnectUrl(), "0.0.0.0");

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnService = findViewById(R.id.btn_service);
        draw2D = findViewById(R.id.draw_2d);

        btnService.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, BeaconForegroundService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

        // получаем beacons и рисуем на карте
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String token = beaconDataProvider.auth("a-krasnov@it-serv.ru", "12345");
            wBeacon[] beacons = beaconDataProvider.getBeacons(token);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    draw2D.setBeacons(beacons);
                    draw2D.invalidate();
                }
            });
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(myConnection);
            isBound = false;
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btn_service) {
            if (btnService.getText().equals(getString(R.string.start_service))) {
                btnService.setText(R.string.stop_service);

                ForegroundServiceUtil.startForegroundService(this, BeaconForegroundService.class, draw2D.getBeacons());
            } else {
                btnService.setText(R.string.start_service);

                ForegroundServiceUtil.stopForegroundService(this, BeaconForegroundService.class);
            }
        }
    }

    @Override
    public void onDegreesChanged(float azimuthInDegrees) {
        if(Math.abs(Math.abs(draw2D.getAzimuthInDegrees()) - Math.abs(azimuthInDegrees)) > 5) {
            draw2D.setAzimuthInDegrees(azimuthInDegrees);
            updateDataFromService();
            draw2D.invalidate();
        }
    }

    private void updateDataFromService() {
        if (isBound) {
            double[] data = beaconForegroundService.getData();
            draw2D.setIm(new float[] {(float) data[0], (float) data[1]});
        }
    }
}