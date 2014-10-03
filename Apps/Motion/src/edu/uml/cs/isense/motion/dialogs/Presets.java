package edu.uml.cs.isense.motion.dialogs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import edu.uml.cs.isense.motion.R;

public class Presets extends Activity{
	public static final String PROJECT = "project";
	public static final String LENGTH = "length";
	public static final String RATE = "rate";


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.presets);

		Button accel = (Button) findViewById(R.id.buttonAccelerometer);
		Button gps = (Button) findViewById(R.id.buttonGPS);
		Button cancel = (Button) findViewById(R.id.cancelButton);

		accel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra(RATE, 50);
				resultIntent.putExtra(LENGTH, 10);
				resultIntent.putExtra(PROJECT, "570");
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}

		});

		gps.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				resultIntent.putExtra(RATE, 1000);
				resultIntent.putExtra(LENGTH, -1);
				resultIntent.putExtra(PROJECT, "13");
				setResult(Activity.RESULT_OK, resultIntent);
				finish();
			}

		});

		cancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_CANCELED, resultIntent);
				finish();
			}

		});


	}
}
