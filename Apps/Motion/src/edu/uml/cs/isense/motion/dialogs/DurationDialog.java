package edu.uml.cs.isense.motion.dialogs;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;

import edu.uml.cs.isense.motion.Motion;
import edu.uml.cs.isense.motion.R;

public class DurationDialog extends Activity {

	Button ok, cancel;
	EditText input;
	Spinner spinner;
	int length;

	public void onRadioButtonClicked(View view) {

    	Log.e("duration dialog", "" + view.getId());

	    switch (view.getId()) {
	    case R.id.radio0:
	    	length = 1;
	    	break;

	    case R.id.radio1:
	    	length = 2;
	    	break;

	    case R.id.radio2:
    		length = 5;
	    	break;

	    case R.id.radio3:
    		length = 10;
	    	break;

	    case R.id.radio4:
	    	length = 30;
	    	break;

	    case R.id.radio5:
	    	length = 60;
	    	break;

	    default:
	    	length = -1;
	    	break;
	    }

	}


	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.duration_dialog);

		ok = (Button) findViewById(R.id.positive2);
		cancel = (Button) findViewById(R.id.negative2);
		RadioButton defaultR;

		SharedPreferences prefs = getSharedPreferences(Motion.MY_SAVED_PREFERENCES,
				0);
		length = prefs.getInt(Motion.LENGTH_PREFS_KEY, 10);

		switch(length){

			case 1:
				defaultR = (RadioButton) findViewById(R.id.radio0);
				break;
			case 2:
				defaultR = (RadioButton) findViewById(R.id.radio1);
				break;
			case 5:
				defaultR = (RadioButton) findViewById(R.id.radio2);
				break;
			case 10:
				defaultR = (RadioButton) findViewById(R.id.radio3);
				break;
			case 30:
				defaultR = (RadioButton) findViewById(R.id.radio4);
				break;
			case 60:
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
				editor.putInt(Motion.LENGTH_PREFS_KEY, length);
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

}
