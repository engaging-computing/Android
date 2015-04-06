package edu.uml.cs.isense.riverwalk.dialogs;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;

import android.view.View;
import android.widget.Button;
import edu.uml.cs.isense.riverwalk.R;
import android.view.MenuItem;


public class Help extends ActionBarActivity {
	private Button okButton;

	@Override
	public void onCreate(Bundle savedInstanceBundle) {
		super.onCreate(savedInstanceBundle);
		setContentView(R.layout.help);

        // Initialize action bar customization for API >= 11
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            ActionBar bar = getSupportActionBar();

            // make the actionbar clickable
            bar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle("Help");
		// Creates the OK button so users can leave
		okButton = (Button) findViewById(R.id.okButton);
		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
