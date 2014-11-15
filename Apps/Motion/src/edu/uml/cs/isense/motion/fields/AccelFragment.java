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

public class AccelFragment extends Fragment implements SensorEventListener {

	private SensorManager mSensorManager;
    private TextView x;
    private TextView y;
    private TextView z;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.accel_fragment, container, false);

        x = (TextView) rootView.findViewById(R.id.accelx);
        y = (TextView) rootView.findViewById(R.id.accely);
        z = (TextView) rootView.findViewById(R.id.accelz);

		mSensorManager = (SensorManager) Motion.mContext.getSystemService(Context.SENSOR_SERVICE);

        mSensorManager.registerListener(this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) == null) {
        	y.setVisibility(View.GONE);
        	z.setVisibility(View.GONE);
        	x.setText("No Sensor");
        	x.setTextColor(Color.RED);
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
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
			try {
				DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
				String xPrepend = event.values[0] > 0 ? "+" : "";
				String yPrepend = event.values[1] > 0 ? "+" : "";
				String zPrepend = event.values[2] > 0 ? "+" : "";

				x.setText("X: " + xPrepend
						+ oneDigit.format(event.values[0]));
				y.setText("Y: " + yPrepend
						+ oneDigit.format(event.values[1]));
				z.setText("Z: " + zPrepend
						+ oneDigit.format(event.values[2]));

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