package edu.uml.cs.isense.motion.dialogs;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;

import edu.uml.cs.isense.motion.R;

public class Presets extends Activity{
	public static final String PROJECT = "project";
	public static final String LENGTH = "length";
	public static final String RATE = "rate";
    RadioButton accel;
    RadioButton gps;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.presets);

		accel = (RadioButton) findViewById(R.id.accel);
        gps = (RadioButton) findViewById(R.id.gps);

        Button ok = (Button) findViewById(R.id.okButton);
        Button cancel = (Button) findViewById(R.id.cancelButton);

		ok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
                if (accel.isChecked()) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(RATE, 50);
                    resultIntent.putExtra(LENGTH, 10);
                    resultIntent.putExtra(PROJECT, "570");
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                } else if(gps.isChecked()) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(RATE, 1000);
                    resultIntent.putExtra(LENGTH, -1);
                    resultIntent.putExtra(PROJECT, "13");
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }
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
