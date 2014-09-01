package com.example.manualentry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.objects.RProjectField;
import edu.uml.cs.isense.proj.Setup;
import edu.uml.cs.isense.queue.QueueLayout;
import edu.uml.cs.isense.queue.UploadQueue;
import edu.uml.cs.isense.queue.QDataSet.Type;
import edu.uml.cs.isense.waffle.Waffle;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ManualEntry extends Activity {
	public static Context mContext;
	private API api; 
	private Boolean useDev = false;
	private LinearLayout datapointsLayout;
	private ArrayList<RProjectField> fields;
	private Button addField;
	private Button save;
	private int datapoints = 0;
	private Waffle w;
	public static UploadQueue uq;


	
	/* Action Bar */
	private static int actionBarTapCount = 0;
	
	public static final int LOGIN_STATUS_REQUESTED = 6005;
	public static final int PROJECT_REQUESTED = 6009;
	public static final int QUEUE_UPLOAD_REQUESTED = 7021;

	@SuppressLint("NewApi") @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manual_entry);
		mContext = this;
		api = API.getInstance();
		api.useDev(useDev);
		
		uq = new UploadQueue("ManualEntry", mContext, api);
		uq.buildQueueFromFile();
		
		w = new Waffle(mContext);

		CredentialManager.login(mContext, api);
		addField = (Button) findViewById(R.id.adddatapoint);
		save = (Button) findViewById(R.id.upload);
		
		// Initialize action bar customization for API >= 11
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			ActionBar bar = getActionBar();

			// make the actionbar clickable
			bar.setDisplayHomeAsUpEnabled(true);
		}
		
		datapointsLayout = (LinearLayout) findViewById(R.id.datapoints_sv);
		if (fields == null) {
			new getNewFieldsTask().execute();
		} else {
			addFields();
		}

		addField.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.e("Here", "push button");
				addFields();				
			}
			
		});
		save = (Button) findViewById(R.id.save);
		
		save.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				JSONArray uploadData = getDataFromScreen();
				
				String dataSetName = "test";
				String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
				String description = "Time: " + currentDateTimeString + "\n" + "Number of Data Points: " + uploadData.length();
				Type type = Type.DATA;
				
				SharedPreferences setupPrefs = getSharedPreferences(
						Setup.PROJ_PREFS_ID, Context.MODE_PRIVATE);
				int projectID = Integer.parseInt(setupPrefs.getString(Setup.PROJECT_ID,
							"-1"));

				//add new dataset to queue
				uq.buildQueueFromFile();
				uq.addToQueue(dataSetName, description, type, uploadData, null, Integer.toString(projectID), null);
	        
				
				clearFields();
			}
			
		});
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
			this.startActivityForResult(setup, PROJECT_REQUESTED);
			return true;
		}
		
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
			
			clearFields(); 
			new getNewFieldsTask().execute();

			actionBarTapCount = 0;
			break;
		
		}

	}
	
	if (id ==  R.id.MENU_ITEM_UPLOAD) {
		manageUploadQueue();
	}
	return true;
}

	
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);
		if (reqCode == LOGIN_STATUS_REQUESTED) {
			if (resultCode == RESULT_OK) {
				
			}
		} else if (reqCode == PROJECT_REQUESTED) {
			if (resultCode == RESULT_OK) {
				Log.e("MANUAL", "Proj Requested");
				clearFields();
				new getNewFieldsTask().execute();
			}
			Log.e("MANUAL", "Proj Requested Result not ok");
		}  else if (reqCode == QUEUE_UPLOAD_REQUESTED) {
			uq.buildQueueFromFile();

		}
	}
	
	/**
	 * Removes current fields from the layout
	 */
	private void clearFields() {
		datapoints = 0;
		fields = null; 
		datapointsLayout.removeAllViews();
	}
	
	/**
	 * Adds current fields to the layout
	 */
	private void addFields() {
		datapoints++;	

		LayoutInflater inflater = (LayoutInflater)getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout datapoint = new LinearLayout(mContext);
		FrameLayout dataPointFrame = new FrameLayout(mContext);
		
		dataPointFrame = (FrameLayout) getLayoutInflater().inflate(R.layout.data_point, null);
		
		datapoint = (LinearLayout) dataPointFrame.findViewById(R.id.ll_data_point);
		
		TextView datapointnumber = new TextView(mContext);
		datapointnumber.setText("Data Point: " + datapoints);
		datapointnumber.setWidth(LayoutParams.MATCH_PARENT);
		
		datapoint.addView(datapointnumber, 0);	
		
		
		for(int i=0; i<fields.size(); i++) {
			RProjectField field = fields.get(i);
			
			if (field.type == RProjectField.TYPE_TIMESTAMP) {
				ViewGroup singlefield = new RelativeLayout(mContext);
				inflater.inflate(R.layout.field, singlefield);
				
				EditText et = (EditText) singlefield.findViewById(R.id.field_et);
				TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);
				

				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm MM/dd/yyyy");
				String currentDateandTime = sdf.format(new Date());

				et.setText(currentDateandTime);
				et.setEnabled(false);
				et.setInputType(InputType.TYPE_CLASS_DATETIME);
				tv.setText(field.name + ":");
				datapoint.addView(singlefield, i+1);	

			} else if (field.type == RProjectField.TYPE_TEXT) {
				ViewGroup singlefield = new RelativeLayout(mContext);
				inflater.inflate(R.layout.field, singlefield);
				
				EditText et = (EditText) singlefield.findViewById(R.id.field_et);
				TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);
				
				tv.setText(field.name + ":");
				datapoint.addView(singlefield, i+1);	
				
			} else if (field.type == RProjectField.TYPE_NUMBER) {
				ViewGroup singlefield = new RelativeLayout(mContext);
				inflater.inflate(R.layout.field, singlefield);
				
				EditText et = (EditText) singlefield.findViewById(R.id.field_et);
				TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);
				
				et.setInputType(InputType.TYPE_CLASS_NUMBER);
				tv.setText(field.name + ":");
				datapoint.addView(singlefield, i+1);	
				
			} else if (field.type == RProjectField.TYPE_LON) {
				ViewGroup singlefield = new RelativeLayout(mContext);
				inflater.inflate(R.layout.field, singlefield);
				
				EditText et = (EditText) singlefield.findViewById(R.id.field_et);
				TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);
				
				et.setEnabled(false);
				tv.setText(field.name + ":");
				datapoint.addView(singlefield, i+1);	
				
			} else if (field.type == RProjectField.TYPE_LAT) {
				ViewGroup singlefield = new RelativeLayout(mContext);
				inflater.inflate(R.layout.field, singlefield);
				
				EditText et = (EditText) singlefield.findViewById(R.id.field_et);
				TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);
				
				et.setEnabled(false);
				tv.setText(field.name + ":");
				datapoint.addView(singlefield, i+1);	
			}
		}
		
		
		datapointsLayout.addView(dataPointFrame, datapoints - 1);
	}
	
	/**
	 * Gets Fields from project and sets ui
	 * 
	 * @author Bobby
	 */
	public class getNewFieldsTask extends AsyncTask<Void, Integer, Void> {


		/**
		 * Tries to get the project from iSENSE.
		 */
		@Override
		protected Void doInBackground(Void... arg0) {
			SharedPreferences setupPrefs = getSharedPreferences(
					Setup.PROJ_PREFS_ID, Context.MODE_PRIVATE);
			int projectID = Integer.parseInt(setupPrefs.getString(Setup.PROJECT_ID,
						"-1"));
			try {
				fields = api.getProjectFields(projectID);
			} catch(Exception e) {
				e.printStackTrace();
				fields = new ArrayList<RProjectField>(); 
			}
			
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
			addFields();				
		}

	}
	
	JSONArray getDataFromScreen() {
		JSONArray data = new JSONArray();
		
		for (int i = 0; i < datapointsLayout.getChildCount(); i++) {
			JSONArray dataPoint = new JSONArray();

			FrameLayout datapointFrameLayout = (FrameLayout) datapointsLayout.getChildAt(i);
			LinearLayout datapointlayout = (LinearLayout) datapointFrameLayout.getChildAt(0);

			for (int j = 0; j < fields.size(); j++) {
				Log.e("TEST", ""+j);
				
				RelativeLayout fieldlayout = (RelativeLayout) datapointlayout.getChildAt(j + 1);
				EditText etData = (EditText) fieldlayout.findViewById(R.id.field_et);
				
				//try to get data from edittext for a field and add to json
				try {
					if (etData == null) {
						dataPoint.put("");
					} else {
						dataPoint.put(j, etData.getText());
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//try to add datapoint json to data json
			try {
				data.put(i, dataPoint);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		
		Log.e("data", "Data: " + data.toString());
		return data;
	}
	
	private void manageUploadQueue() {

		if (!uq.emptyQueue()) {
			Intent i = new Intent().setClass(mContext, QueueLayout.class);
			i.putExtra(QueueLayout.PARENT_NAME, uq.getParentName());
			startActivityForResult(i, QUEUE_UPLOAD_REQUESTED);
		} else {
			w.make("There is no data to upload!", Waffle.LENGTH_LONG,
					Waffle.IMAGE_X);
		}
	}
	
}
