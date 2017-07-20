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

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {

    class Point {
        static final int MIN_VALUE = 7;

        float x;
        float y;
        float z;

        Point() {}

        Point(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Point getDistance(Point p) {
            return new Point(
                    Math.abs(x - p.x),
                    Math.abs(y - p.y),
                    Math.abs(z - p.z));
        }

        int getCount() {
            int sum = 0;
            sum += x > MIN_VALUE ? 1 : 0;
            sum += y > MIN_VALUE ? 1 : 0;
            sum += z > MIN_VALUE ? 1 : 0;
            return sum;
        }
    }

    public static final int SENSITIVITY_MEDIUM = 3;

    private TextView mMessageText;
    private Button mStartButton;
    private Button mFinishButton;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long mLastUpdateTime = -1;
    private Point mLastPoint;
    private List<Integer> mCounterList;
    private boolean mDetectNod = false;
    private boolean mDetectShake = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessageText = (TextView) findViewById(R.id.message);
        mStartButton = (Button) findViewById(R.id.start);
        mFinishButton = (Button) findViewById(R.id.finish);

        mStartButton.setOnClickListener(this);
        mFinishButton.setOnClickListener(this);

        mCounterList = new ArrayList<>();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mLastUpdateTime == -1) {
            mLastUpdateTime = event.timestamp;
            return;
        }

        if (event.timestamp - mLastUpdateTime > 5e7) {
            Point point = new Point(event.values[0], event.values[1], event.values[2]);

            if (mCounterList.size() == 5) {
                int sum = 0;
                for (int i : mCounterList) {
                    sum += i;
                }
                if (sum > 1) {
                    mDetectNod = true;
                }
                if (sum > SENSITIVITY_MEDIUM) {
                    mDetectShake = true;
                }
                mCounterList.clear();
            }

            if (mLastPoint != null) {
                mCounterList.add(mLastPoint.getDistance(point).getCount());
            }

            mLastUpdateTime = event.timestamp;
            mLastPoint = point;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start: {
                mLastPoint = null;
                mLastUpdateTime = -1;
                mDetectNod = false;
                mDetectShake = false;
                mCounterList.clear();

                if (mSensorManager == null) {
                    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                }

                if (mAccelerometer == null) {
                    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                }

                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
                mMessageText.setText("監測動作是點還是晃...");
                break;
            }
            case R.id.finish: {
                if (mSensorManager != null && mAccelerometer != null) {
                    mSensorManager.unregisterListener(this, mAccelerometer);
                    mSensorManager = null;
                    mAccelerometer = null;
                }
                String message = mDetectNod ? ( mDetectShake ? "晃" : "點") : "無動作";
                mMessageText.setText(message);
                break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.unregisterListener(this, mAccelerometer);
            mSensorManager = null;
            mAccelerometer = null;
        }
        super.onDestroy();
    }
}
