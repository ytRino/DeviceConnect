/*
DataLayerListenerService.java
Copyright (c) 2014 NTT DOCOMO,INC.
Released under the MIT license
http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.wear;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Vibrator;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * DataLayerListenerService.
 * 
 * @author NTT DOCOMO, INC.
 */
public class DataLayerListenerService extends WearableListenerService implements SensorEventListener {

    /** Tag. */
    private static final String TAG = "WEAR";

    /** Google API Client. */
    private static GoogleApiClient mGoogleApiClient;

    /** SensorManager. */
    private static SensorManager mSensorManager;

    /** 加速度 x. */
    private static float mAccellX;

    /** 加速度 y. */
    private static float mAccellY;

    /** 加速度 z. */
    private static float mAccellZ;

    /** Gyro x. */
    private static float mGyroX;

    /** Gyro y. */
    private static float mGyroY;

    /** Gyro z. */
    private static float mGyroZ;

    /** DeviceのNodeID . */
    private String mId;

    /** GyroSensor. */
    private Sensor myGyroSensor;

    /** AcceleratorSensor. */
    private Sensor mAccelerometer;

    @Override
    public void onCreate() {
        super.onCreate();

        // Define google play service
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();

        // Connect google play service
        mGoogleApiClient.connect();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {

        // get id of wear device
        mId = messageEvent.getSourceNodeId();
        if (messageEvent.getPath().equals(WearConst.DEVICE_TO_WEAR_VIBRATION_RUN)) {

            // get vibration pattern
            String mPattern = new String(messageEvent.getData());

            // Make array of pattern
            String[] mPatternArray = mPattern.split(",", 0);
            long[] mPatternLong = new long[mPatternArray.length + 1];
            mPatternLong[0] = 0;
            for (int i = 1; i < mPatternLong.length; i++) {
                mPatternLong[i] = Integer.parseInt(mPatternArray[i - 1]);
            }

            // vibrate
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(mPatternLong, -1);
            stopSelf();
        } else if (messageEvent.getPath().equals(WearConst.DEVICE_TO_WEAR_VIBRATION_DEL)) {

            // stop vibrate
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.cancel();
            stopSelf();
        } else if (messageEvent.getPath().equals(WearConst.DEVICE_TO_WEAR_DEIVCEORIENTATION_REGISTER)) {

            if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {

                mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
                mGoogleApiClient.connect();

                ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

                if (!connectionResult.isSuccess()) {

                } else {
                }

            }

            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            List<Sensor> accelSensors = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
            if (accelSensors.size() > 0) {
                mAccelerometer = accelSensors.get(0);
                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }

            List<Sensor> gyroSensors = mSensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
            if (gyroSensors.size() > 0) {
                myGyroSensor = gyroSensors.get(0);
                mSensorManager.registerListener(this, myGyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

        } else if (messageEvent.getPath().equals(WearConst.DEVICE_TO_WEAR_DEIVCEORIENTATION_UNREGISTER)) {

            if (mSensorManager != null) {
                mSensorManager.unregisterListener(this, mAccelerometer);
                mSensorManager.unregisterListener(this, myGyroSensor);
                mSensorManager.unregisterListener(this);
                mSensorManager = null;
            }

            mGoogleApiClient.disconnect();
            stopSelf();
        }
    }

    @Override
    public void onPeerConnected(final Node peer) {
    }

    @Override
    public void onPeerDisconnected(final Node peer) {
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            mAccellX = sensorEvent.values[0];
            mAccellY = sensorEvent.values[1];
            mAccellZ = sensorEvent.values[2];
            final String data = mAccellX + "," + mAccellY + "," + mAccellZ 
                           + "," + mGyroX + "," + mGyroY + "," + mGyroZ;

            // メッセージの送信
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(final Void... params) {
                    if (!mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.connect();
                    } else {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, mId,
                                WearConst.WERA_TO_DEVICE_DEIVCEORIENTATION_DATA, data.getBytes()).await();
                        if (!result.getStatus().isSuccess()) {

                        } else {
                        }
                    }
                    return null;
                }
            }.execute();

        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            mGyroX = sensorEvent.values[0];
            mGyroY = sensorEvent.values[1];
            mGyroZ = sensorEvent.values[2];
        }
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    }
}