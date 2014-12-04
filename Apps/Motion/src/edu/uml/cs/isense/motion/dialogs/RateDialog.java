package edu.uml.cs.isense.motion.dialogs;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import edu.uml.cs.isense.motion.Motion;
import edu.uml.cs.isense.motion.R;

public class RateDialog extends ActionBarActivity {

	Button ok, cancel;
	EditText input;
	Spinner spinner;
	int rate;

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.rate_dialog);

		ok = (Button) findViewById(R.id.positive2);
		cancel = (Button) findViewById(R.id.negative2);
		RadioButton defaultR;

		SharedPreferences prefs = getSharedPreferences(Motion.MY_SAVED_PREFERENCES, 0);
		rate = prefs.getInt(Motion.RATE_PREFS_KEY, 50);

		switch (rate) {

		case 50:
			defaultR = (RadioButton) findViewById(R.id.radio0);
			break;
		case 100:
			defaultR = (RadioButton) findViewById(R.id.radio1);
			break;
		case 500:
			defaultR = (RadioButton) findViewById(R.id.radio2);
			break;
		case 1000:
			defaultR = (RadioButton) findViewById(R.id.radio3);
			break;
		case 5000:
			defaultR = (RadioButton) findViewById(R.id.radio4);
			break;
		case 30000:
			defaultR = (RadioButton) findViewById(R.id.radio5);
			break;
		default:
			defaultR = (RadioButton) findViewById(R.id.radio6);
			break;
		}

		defaultR.setChecked(true);

		ok.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				SharedPreferences prefs = getSharedPreferences(Motion.MY_SAVED_PREFERENCES,
						0);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt(Motion.RATE_PREFS_KEY, rate);
				editor.commit();

				setResult(RESULT_OK);
				finish();

			}
		});

		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();

			}
		});
	}

    public void onRadioButtonClicked(View view) {
        switch (view.getId()) {
            case R.id.radio0:
                rate = 50;
                break;

            case R.id.radio1:
                rate = 100;
                break;

            case R.id.radio2:
                rate = 500;
                break;

            case R.id.radio3:
                rate = 1000;
                break;

            case R.id.radio4:
                rate = 5000;
                break;

            case R.id.radio5:
                rate = 30000;
                break;

            default:
                rate = 60000;
                break;
        }

    }

}
