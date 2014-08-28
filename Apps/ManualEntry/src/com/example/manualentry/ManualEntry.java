package com.example.manualentry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.objects.RProjectField;
import edu.uml.cs.isense.proj.Setup;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ManualEntry extends Activity {
	public static Context mContext;
	private API api; 
	private Boolean useDev = false;
	private LinearLayout fieldsLayout;
	ArrayList<RProjectField> fields;
	
	public static final int LOGIN_STATUS_REQUESTED = 6005;
	public static final int PROJECT_REQUESTED = 6005;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manual_entry);
		mContext = this;
		api = API.getInstance();
		api.useDev(useDev);

		CredentialManager.login(mContext, api);
		
		fieldsLayout = (LinearLayout) findViewById(R.id.fields_sv);
		
		new addNewFieldsTask().execute();


		
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
				Log.e("MANUAL", "here2");

				clearFields();
				new addNewFieldsTask().execute();
			}
		}
	}
	
	private void clearFields() {
		fieldsLayout.removeAllViews();
	}
	
	/**
	 * Gets Fields from project and sets ui
	 * 
	 * @author Bobby
	 */
	public class addNewFieldsTask extends AsyncTask<Void, Integer, Void> {


		/**
		 * Tries to get the project from iSENSE.
		 */
		@Override
		protected Void doInBackground(Void... arg0) {
			SharedPreferences setupPrefs = getSharedPreferences(
					Setup.PROJ_PREFS_ID, Context.MODE_PRIVATE);
			int projectID = Integer.parseInt(setupPrefs.getString(Setup.PROJECT_ID,
						"-1"));
			fields = api.getProjectFields(projectID);
			
			Log.e("MANUAL", "" + api.getProjectFields(projectID));
			return null;
		}

		/**
		 * Called once you've finished getting the fields.
		 * Sets UI according to new projects fields 
		 */
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			
			
			for(int i=0; i<fields.size(); i++) {
				Log.e("MANUAL", "here");
				
				//TODO add edit texts for fields
				RProjectField field = fields.get(i);
				if (field.type == RProjectField.TYPE_TIMESTAMP) {
					LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					ViewGroup parent = new RelativeLayout(mContext);
					inflater.inflate(R.layout.field, parent);
					
					EditText et = (EditText) parent.findViewById(R.id.field_et);
					TextView tv = (TextView) parent.findViewById(R.id.field_tv);
					

					SimpleDateFormat sdf = new SimpleDateFormat("HH:mm MM/dd/yyyy");
					String currentDateandTime = sdf.format(new Date());

					et.setText(currentDateandTime);
					et.setEnabled(false);
					tv.setText(field.name + ":");
					fieldsLayout.addView(parent, i);	

				} else if (field.type == RProjectField.TYPE_TEXT) {
					LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					ViewGroup parent = new RelativeLayout(mContext);
					inflater.inflate(R.layout.field, parent);
					
					EditText et = (EditText) parent.findViewById(R.id.field_et);
					TextView tv = (TextView) parent.findViewById(R.id.field_tv);
					
					tv.setText(field.name + ":");
					fieldsLayout.addView(parent, i);	
					
				} else if (field.type == RProjectField.TYPE_NUMBER) {
					LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					ViewGroup parent = new RelativeLayout(mContext);
					inflater.inflate(R.layout.field, parent);
					
					EditText et = (EditText) parent.findViewById(R.id.field_et);
					TextView tv = (TextView) parent.findViewById(R.id.field_tv);
					
					tv.setText(field.name + ":");
					fieldsLayout.addView(parent, i);	
					
				} else if (field.type == RProjectField.TYPE_LON) {
					LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					ViewGroup parent = new RelativeLayout(mContext);
					inflater.inflate(R.layout.field, parent);
					
					EditText et = (EditText) parent.findViewById(R.id.field_et);
					TextView tv = (TextView) parent.findViewById(R.id.field_tv);
					
					et.setEnabled(false);
					tv.setText(field.name + ":");
					fieldsLayout.addView(parent, i);	
					
				} else if (field.type == RProjectField.TYPE_LAT) {
					LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					ViewGroup parent = new RelativeLayout(mContext);
					inflater.inflate(R.layout.field, parent);
					
					EditText et = (EditText) parent.findViewById(R.id.field_et);
					TextView tv = (TextView) parent.findViewById(R.id.field_tv);
					
					et.setEnabled(false);
					tv.setText(field.name + ":");
					fieldsLayout.addView(parent, i);	

				}
			}			
		}

	}
	
	
}
