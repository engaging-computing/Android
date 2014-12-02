package edu.uml.cs.isense.motion.dialogs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import edu.uml.cs.isense.motion.Motion;
import edu.uml.cs.isense.motion.R;

public class ViewData extends ActionBarActivity {

	Button view, cancel;

	@Override
	public void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.view_data);

		view = (Button) findViewById(R.id.view);
		cancel = (Button) findViewById(R.id.noview);

		setTitle("View Your Data On iSENSE?");

		view.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse(Motion.dataSetUrl));
				startActivity(i);
				finish();

			}
		});

		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();

			}
		});

	}

}
