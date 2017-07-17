package com.viovie.sensortest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.LinkedList;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    public static final int SENSITIVITY_MEDIUM = 13;

    private TextView mMessageText;
    private Button mStartButton;
    private Button mFinishButton;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long mLastUpdateTime = -1;
    private long mStartTime = -1;

    private LinkedList<Long> xTimeList;
    private LinkedList<Long> yTimeList;
    private LinkedList<Long> zTimeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessageText = (TextView) findViewById(R.id.message);
        mStartButton = (Button) findViewById(R.id.start);
        mFinishButton = (Button) findViewById(R.id.finish);

        mStartButton.setOnClickListener(this);
        mFinishButton.setOnClickListener(this);

        xTimeList = new LinkedList<>();
        yTimeList = new LinkedList<>();
        zTimeList = new LinkedList<>();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mLastUpdateTime == -1) {
            mLastUpdateTime = event.timestamp;
            mStartTime = event.timestamp;
            return;
        }

        if (event.timestamp - mLastUpdateTime > 1e8) {
            float ax = event.values[0];
            float ay = event.values[1];
            float az = event.values[2];

            if (Math.abs(ax) > SENSITIVITY_MEDIUM && (xTimeList.isEmpty() || event.timestamp - xTimeList.getLast() > 3e8)) {
                xTimeList.add(event.timestamp);
            }

            if (Math.abs(ay) > SENSITIVITY_MEDIUM && (yTimeList.isEmpty() || event.timestamp - yTimeList.getLast() > 3e8)) {
                yTimeList.add(event.timestamp);
            }

            if (Math.abs(az) > SENSITIVITY_MEDIUM && (zTimeList.isEmpty() || event.timestamp - zTimeList.getLast() > 3e8)) {
                zTimeList.add(event.timestamp);
            }

            // Log.e("Test", String.format("x:%f, y:%f, z:%f", ax, ay, az));

            mLastUpdateTime = event.timestamp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start: {
                mLastUpdateTime = -1;
                xTimeList.clear();
                yTimeList.clear();
                zTimeList.clear();

                if (mSensorManager == null) {
                    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                }

                if (mAccelerometer == null) {
                    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                }

                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
                mMessageText.setText("監測動作中...");
                break;
            }
            case R.id.finish: {
                if (mSensorManager != null && mAccelerometer != null) {
                    mSensorManager.unregisterListener(this, mAccelerometer);
                    mSensorManager = null;
                    mAccelerometer = null;
                }
                String message = "點:" + zTimeList.size() + ", 晃:" + xTimeList.size();
                mMessageText.setText(message);
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager = null;
        mAccelerometer = null;
        super.onDestroy();
    }
}
