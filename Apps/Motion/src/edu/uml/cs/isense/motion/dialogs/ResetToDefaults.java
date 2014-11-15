package edu.uml.cs.isense.motion.dialogs;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import edu.uml.cs.isense.motion.R;

public class ResetToDefaults extends Activity {

	Button reset, noreset;

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		this.setContentView(R.layout.reset);

		setTitle("Are You Sure?");

		reset = (Button) findViewById(R.id.reset);
		noreset = (Button) findViewById(R.id.noreset);

		reset.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_OK);
				finish();
			}
		});

		noreset.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();

			}
		});

	}

}
