package edu.uml.cs.isense.motion.fields;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.uml.cs.isense.motion.Motion;
import edu.uml.cs.isense.motion.R;

public class PressureFragment extends Fragment implements SensorEventListener {

	private SensorManager mSensorManager;
    private TextView pressure;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.pressure_fragment, container, false);

        pressure = (TextView) rootView.findViewById(R.id.pressure);

		mSensorManager = (SensorManager) Motion.mContext.getSystemService(Context.SENSOR_SERVICE);

        mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
				SensorManager.SENSOR_DELAY_UI);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) == null) {
        	pressure.setText("No Sensor");
        	pressure.setTextColor(Color.RED);
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

	@Override
	public void onResume() {
		super.onResume();
		mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_PRESSURE ) {
			DecimalFormat format = new DecimalFormat("#,##0.0000");
			try {
				pressure.setText(format.format(event.values[0]) + " hPa or mbar");
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