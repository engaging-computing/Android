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

import java.text.DecimalFormat;

import edu.uml.cs.isense.motion.Motion;
import edu.uml.cs.isense.motion.R;

public class TempFragment extends Fragment implements SensorEventListener {

	private SensorManager mSensorManager;
    private TextView tempF;
    private TextView tempC;

    @SuppressLint("InlinedApi")
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.temp_fragment, container, false);

        tempF = (TextView) rootView.findViewById(R.id.tempf);
        tempC = (TextView) rootView.findViewById(R.id.tempc);

		mSensorManager = (SensorManager) Motion.mContext.getSystemService(Context.SENSOR_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
        	 mSensorManager.registerListener(this,
     				mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),
     				SensorManager.SENSOR_DELAY_UI);
			if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) == null) {
				tempC.setVisibility(View.GONE);
				tempF.setText("No Sensor");
				tempF.setTextColor(Color.RED);
			}
		} else {
			tempC.setVisibility(View.GONE);
			tempF.setText("Not supported before Android 3.0");
			tempF.setTextColor(Color.RED);
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
				mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE ) {
            DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
			try {
				tempF.setText(oneDigit.format(((event.values[0] * 1.8) + 32)) +  " \u2109");
				tempC.setText(oneDigit.format(event.values[0]) +  " \u2103");

			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}


	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}
}