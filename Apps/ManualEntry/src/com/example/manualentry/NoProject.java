package com.example.manualentry;

import com.example.manualentry.ManualEntry.getNewFieldsTask;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.proj.Setup;
import edu.uml.cs.isense.waffle.Waffle;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class NoProject extends Activity {
	
	private Button selectProject;
	private static int actionBarTapCount = 0;
	private Waffle w;
	
	private API api; 
	private Boolean useDev = false;
	public static Context mContext;
	
	@SuppressLint("NewApi")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.no_project);
		
		api = API.getInstance();
		api.useDev(useDev);
		
		mContext = this;
		
		w = new Waffle(mContext);

		
		selectProject = (Button) findViewById(R.id.project);
		
		// Initialize action bar customization for API >= 11
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			ActionBar bar = getActionBar();

			// make the actionbar clickable
			bar.setDisplayHomeAsUpEnabled(true);
		}
		
		selectProject.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
			Intent setup = new Intent(ManualEntry.mContext, Setup.class);
			setup.putExtra("showSelectLater", false);
			startActivityForResult(setup, ManualEntry.PROJECT_REQUESTED);
				
			}
			
		});
		
	}
	
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);
		if (reqCode == ManualEntry.PROJECT_REQUESTED) {
			if (resultCode == RESULT_OK) {
				this.finish();
			}
			Log.e("MANUAL", "Proj Requested Result not ok");
		} 
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		
		if (id ==  android.R.id.home) {
			CountDownTimer cdt = null;
	
			// Give user 10 seconds to switch dev/prod mode
			if (actionBarTapCount == 0) {
				cdt = new CountDownTimer(5000, 5000) {
					public void onTick(long millisUntilFinished) {
					}
					public void onFinish() {
						actionBarTapCount = 0;
					}
				}.start();
			}
	
			String other = (useDev) ? "production" : "dev";
	
			switch (++actionBarTapCount) {
			case 5:
				w.make(getResources().getString(R.string.two_more_taps) + other
						+ getResources().getString(R.string.mode_type));
				break;
			case 6:
				w.make(getResources().getString(R.string.one_more_tap) + other
						+ getResources().getString(R.string.mode_type));
				break;
			case 7:
				w.make(getResources().getString(R.string.now_in_mode) + other
						+ getResources().getString(R.string.mode_type));
				useDev = !useDev;
	
				if (cdt != null)
					cdt.cancel();
	
				api.useDev(useDev);
					
				actionBarTapCount = 0;
				break;
			
			}

	}
	
	
	return true;
}
	
}
