package ryu.j.assignment3;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;

import static android.content.Context.SENSOR_SERVICE;

public class CompassSensor implements SensorEventListener {

    private Context context;
    private SensorManager mSensorManager;
    private float degree;

    private TextView title;

    public CompassSensor(Context context, TextView title) {
        this.context = context;
        this.title = title;
        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
    }

    public void startListening() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopListening() {
        mSensorManager.unregisterListener(this);
    }

    public float getDegree() {
        return degree;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        degree = Math.round(sensorEvent.values[0]);
        title.setText(String.valueOf(degree));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}
