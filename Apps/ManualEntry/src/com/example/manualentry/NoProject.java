package com.example.manualentry;

import edu.uml.cs.isense.proj.Setup;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class NoProject extends Activity {
	
	private Button selectProject;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.no_project);
		
		selectProject = (Button) findViewById(R.id.project);
		
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
	
}
