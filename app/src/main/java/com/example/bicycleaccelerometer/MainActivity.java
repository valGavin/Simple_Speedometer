package com.example.bicycleaccelerometer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private final long TIME_DURATION = 3600000;   // millisecond
    private final long TIME_INTERVAL = 500;       // millisecond

    float lastX;
    float lastY;
    float lastZ;
    float lastVelocity = 0;

    private BlockingQueue<Float> capturedX = new LinkedBlockingQueue<>(2048);

    private float deltaThreshold = 5.0f;
    private float deltaX;
    private float deltaY;
    private float deltaZ;

    private TextView currentX, currentY, currentZ, velocity, thresholdValue;
    private SeekBar seekBarThreshold;

    final CountDownTimer timer = new CountDownTimer(TIME_DURATION, TIME_INTERVAL) {
        @SuppressLint({"SetTextI18n", "DefaultLocale"})
        @Override
        public void onTick(long l) {
            int counter = 0;
            float currentVelocity = lastVelocity;
            float sum = 0;
            float[] temp = new float[64];
            try {
                while (!capturedX.isEmpty()) {
                    temp[counter] = capturedX.take();
                    counter++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            for (float elements : temp)
                sum += elements;

            float average = sum / counter;
            float diff = (average * (TIME_INTERVAL / 1000f));
            currentVelocity += diff;

            if (currentVelocity > 0f) {
                velocity.setText(String.format("%.2f", currentVelocity));
            }
            else {
                velocity.setText(String.format("%.2f", 0f));
                currentVelocity = 0;
            }
            lastVelocity = currentVelocity;
        }

        @Override
        public void onFinish() {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentX = findViewById(R.id.accel_x);
        currentY = findViewById(R.id.accel_y);
        currentZ = findViewById(R.id.accel_z);

        velocity = findViewById(R.id.velocity);

        thresholdValue = findViewById(R.id.threshold_value);
        seekBarThreshold = findViewById(R.id.seekbar_threshold);
        seekBarThreshold.setOnSeekBarChangeListener(seekBarListener);

        Button initButton = findViewById(R.id.init);
        initButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                init();
            }
        });

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        else {
            Toast.makeText(this, "No accelerometer detected on device", Toast.LENGTH_LONG).show();
        }

        init();
    }

    SeekBar.OnSeekBarChangeListener seekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            deltaThreshold = progress / 10f;
            thresholdValue.setText(Float.toString(deltaThreshold));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    @SuppressLint("SetTextI18n")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        try {
            capturedX.put(sensorEvent.values[0]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        showCurrentValues();

        deltaX = Math.abs(lastX - sensorEvent.values[0]);
        deltaY = Math.abs(lastY - sensorEvent.values[1]);
        deltaZ = Math.abs(lastZ - sensorEvent.values[2]);

        if (deltaX < deltaThreshold)
            deltaX = 0;
        if (deltaY < deltaThreshold)
            deltaY = 0;
        if (deltaZ < deltaThreshold) {
            deltaZ = 0;
        }

        lastX = sensorEvent.values[0];
        lastY = sensorEvent.values[1];
        lastZ = sensorEvent.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    private void init() {
        timer.cancel();

        lastX = 0;
        lastY = 0;
        lastZ = 0;
        lastVelocity = 0;

        deltaX = 0;
        deltaY = 0;
        deltaZ = 0;
        deltaThreshold = seekBarThreshold.getProgress() / 10f;

        capturedX.clear();

        timer.start();
    }

    @SuppressLint("SetTextI18n")
    private void showCurrentValues() {
        currentX.setText(Float.toString(deltaX));
        currentY.setText(Float.toString(deltaY));
        currentZ.setText(Float.toString(deltaZ));
    }
}
