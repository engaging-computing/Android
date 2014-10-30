package edu.uml.cs.isense.motion.fields;

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

public class LightFragment extends Fragment implements SensorEventListener {

	private SensorManager mSensorManager;
    private TextView light;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.light_fragment, container, false);

        light = (TextView) rootView.findViewById(R.id.light);

		mSensorManager = (SensorManager) Motion.mContext.getSystemService(Context.SENSOR_SERVICE);

        mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
				SensorManager.SENSOR_DELAY_UI);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) == null) {
			light.setText("No Sensor");
        	light.setTextColor(Color.RED);
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
				mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_LIGHT ) {
			try {
				light.setText("Illuminance: " + event.values[0] + " lx");
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