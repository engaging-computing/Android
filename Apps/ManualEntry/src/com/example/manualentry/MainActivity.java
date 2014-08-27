package com.example.manualentry;

import java.util.ArrayList;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.objects.RProjectField;
import edu.uml.cs.isense.proj.Setup;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {
	public static Context mContext;
	private API api; 
	private Boolean useDev = false;
	
	public static final int LOGIN_STATUS_REQUESTED = 6005;
	public static final int PROJECT_REQUESTED = 6005;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = this;
		api = API.getInstance();
		api.useDev(useDev);

		CredentialManager.login(mContext, api);

		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.MENU_ITEM_LOGIN) {
			startActivityForResult(new Intent(this, CredentialManager.class),
					LOGIN_STATUS_REQUESTED);
			return true;
		}
		if (id == R.id.MENU_ITEM_PROJECT) {
			Intent setup = new Intent(mContext, Setup.class);
			startActivityForResult(setup, PROJECT_REQUESTED);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);
		if (reqCode == LOGIN_STATUS_REQUESTED) {
			if (resultCode == RESULT_OK) {
				
			}
		}
		if (reqCode == PROJECT_REQUESTED) {
			if (resultCode == RESULT_OK) {
				clearFields();
				addNewFields();
			}
		}
	}
	
	private void clearFields() {
		
	}
	
	private void addNewFields() {
		SharedPreferences setupPrefs = getSharedPreferences(
				Setup.PROJ_PREFS_ID, Context.MODE_PRIVATE);
		int projectID = Integer.parseInt(setupPrefs.getString(Setup.PROJECT_ID,
					"-1"));
		ArrayList<RProjectField> fields = api.getProjectFields(projectID);
		
		for(int i=0; i<fields.size(); i++) {
			//TODO add edit texts for fields
			RProjectField field = fields.get(i);
			if (field.type == RProjectField.TYPE_TIMESTAMP) {
				
			} else if (field.type == RProjectField.TYPE_TEXT) {
				
			} else if (field.type == RProjectField.TYPE_NUMBER) {
				
			} else if (field.type == RProjectField.TYPE_LON) {
				
			} else if (field.type == RProjectField.TYPE_LAT) {
				
			}
		}
		
		
	}
}
