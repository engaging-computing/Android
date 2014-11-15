package edu.uml.cs.isense.motion.fields;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.uml.cs.isense.motion.Motion;
import edu.uml.cs.isense.motion.R;

public class HumidityFragment extends Fragment implements SensorEventListener {

	private SensorManager mSensorManager;
    private TextView humidity;

    @SuppressLint("InlinedApi")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.humidity_fragment, container, false);

        humidity = (TextView) rootView.findViewById(R.id.humidity);

		mSensorManager = (SensorManager) Motion.mContext.getSystemService(Context.SENSOR_SERVICE);
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY),
				SensorManager.SENSOR_DELAY_UI);
			if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) == null) {
				humidity.setText("No Sensor");
	        	humidity.setTextColor(Color.RED);
			}
		} else {
        	humidity.setText("Not supported before Android 3.0");
        	humidity.setTextColor(Color.RED);
		}
        return rootView;
    }

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY),
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY ) {
			try {
				humidity.setText("Percent: " + event.values[0] + "%");
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
}