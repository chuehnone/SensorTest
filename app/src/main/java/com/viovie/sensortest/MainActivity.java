package com.viovie.sensortest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener {
    static int NOD_MIN = 3; // nod Min
    static int NOD_MAX = 5; // nod Max
    static int SHAKE_M = 7; // shake M
    static int MIN_VALUE = 7; // F
    static int MAX_DISTANCE = 5;
    static final int MAX_ADDITION_COUNT = 3;
    static final int MAX_TEMP_COUNT = 5;

    class Point {
        Long time;
        float x;
        float y;
        float z;

        Point() {}

        Point (float x, float y, float z) {
            this(0l, x, y, z);
        }

        Point(Long time, float x, float y, float z) {
            this.time = time;
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

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
            return String.format("time:%s, x:%f, y:%f, z:%f", sdf.format(new Date(time)), x, y, z);
        }
    }

    static final int STATUS_NOTHING = 0;
    static final int STATUS_NOD = 1;
    static final int STATUS_SHAKE = 2;

    private EditText mMessageText;
    private Button mStartButton;
    private Button mFinishButton;
    private EditText mInputMinNod;
    private EditText mInputMaxNod;
    private EditText mInputMShake;
    private EditText mInputF;
    private EditText mInputBlock;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private long mLastUpdateTime = -1;
    private int mTempCount = 0;
    private List<Point> mPointList;
    private List<Integer> mStatusList;
    private boolean mPrintTemp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessageText = (EditText) findViewById(R.id.message);
        mStartButton = (Button) findViewById(R.id.start);
        mFinishButton = (Button) findViewById(R.id.finish);
        mInputMinNod = (EditText) findViewById(R.id.input_min_nod);
        mInputMaxNod = (EditText) findViewById(R.id.input_max_nod);
        mInputMShake = (EditText) findViewById(R.id.input_m_shake);
        mInputF = (EditText) findViewById(R.id.input_f);
        mInputBlock = (EditText) findViewById(R.id.input_block);

        mStartButton.setOnClickListener(this);
        mFinishButton.setOnClickListener(this);

        mPointList = new ArrayList<>(MAX_DISTANCE + MAX_ADDITION_COUNT);
        mStatusList = new ArrayList<>();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mLastUpdateTime == -1) {
            mLastUpdateTime = event.timestamp;
            return;
        }

        if (event.timestamp - mLastUpdateTime > 5e7) {
            Point point = new Point(Calendar.getInstance().getTimeInMillis(), event.values[0], event.values[1], event.values[2]);

            if (mPrintTemp) {
                mTempCount++;
                if (mTempCount == MAX_TEMP_COUNT) mPrintTemp = false;
                mMessageText.append("\n");
                mMessageText.append(point.toString());
            } else if (mPointList.size() == MAX_DISTANCE + MAX_ADDITION_COUNT) {
                boolean isPrint = false;
                String message = "";
                int status = STATUS_NOTHING;

                for (int max = MAX_DISTANCE ; max <= MAX_DISTANCE + MAX_ADDITION_COUNT ; max++) {
                    int sum = 0;
                    for (int i = max - MAX_DISTANCE + 1 ; i < max ; i++) {
                        sum += mPointList.get(i).getDistance(mPointList.get(i-1)).getCount();
                    }

                    if (sum >= NOD_MIN && sum <= NOD_MAX) {
                        status = STATUS_NOD;
                        message = "點";
                        isPrint = true;
                    }
                    if (sum >= SHAKE_M) {
                        status = STATUS_SHAKE;
                        message = "晃";
                        isPrint = true;
                        break;
                    }
                }

                mStatusList.add(status);
                if (isPrint) {
                    mPrintTemp = true;
                    mMessageText.append("\n");
                    mMessageText.append(message);
                    mMessageText.append(mPointList.toString());
                }
                mPointList.clear();
                mTempCount = 0;
            } else {
                mPointList.add(point);
            }

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
                mTempCount = 0;
                mPrintTemp = false;
                mPointList.clear();
                mStatusList.clear();

                String input = mInputMinNod.getText().toString();
                if (input.matches("\\d+")) {
                    NOD_MIN = Integer.parseInt(input);
                }
                input = mInputMaxNod.getText().toString();
                if (input.matches("\\d+")) {
                    NOD_MAX = Integer.parseInt(input);
                }
                input = mInputMShake.getText().toString();
                if (input.matches("\\d+")) {
                    SHAKE_M = Integer.parseInt(input);
                }
                input = mInputF.getText().toString();
                if (input.matches("\\d+")) {
                    MIN_VALUE = Integer.parseInt(input);
                }
                input = mInputBlock.getText().toString();
                if (input.matches("\\d+")) {
                    MAX_DISTANCE = Integer.parseInt(input);
                }

                if (mSensorManager == null) {
                    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                }

                if (mAccelerometer == null) {
                    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                }

                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
                mMessageText.setText("監測動作點與晃的數量...");
                break;
            }
            case R.id.finish: {
                if (mSensorManager != null && mAccelerometer != null) {
                    mSensorManager.unregisterListener(this, mAccelerometer);
                    mSensorManager = null;
                    mAccelerometer = null;

                    int nod = 0, shake = 0;
                    for (int i = 0 ; i < mStatusList.size() ; i++) {
                        switch (mStatusList.get(i)) {
                            case STATUS_NOD:
                                nod++;
                                break;
                            case STATUS_SHAKE:
                                shake++;
                                break;
                        }
                    }
                    String message = String.format("\n點:%d, 晃:%d", nod, shake);
                    mMessageText.append(message);
                }
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
