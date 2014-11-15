/***************************************************************************************************/
/***************************************************************************************************/
/**                                                                                               **/
/**      IIIIIIIIIIIII            General Purpose Amusement Park Application      SSSSSSSSS       **/
/**           III                                                               SSS               **/
/**           III                    Original Creator: John Fertita            SSS                **/
/**           III                    Optimized By:     Jeremy Poulin,           SSS               **/
/**           III                                      Michael Stowell           SSSSSSSSS        **/
/**           III                    Faculty Advisor:  Fred Martin                      SSS       **/
/**           III                    Special Thanks:   Don Rhine                         SSS      **/
/**           III                    Group:            ECG, iSENSE                      SSS       **/
/**      IIIIIIIIIIIII               Property:         UMass Lowell              SSSSSSSSS        **/
/**                                                                                               **/
/***************************************************************************************************/
/***************************************************************************************************/

package edu.uml.cs.isense.canobiev2;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.dfm.DataFieldManager;
import edu.uml.cs.isense.proj.Setup;
import edu.uml.cs.isense.queue.QDataSet.Type;
import edu.uml.cs.isense.queue.QueueLayout;
import edu.uml.cs.isense.queue.UploadQueue;
import edu.uml.cs.isense.waffle.Waffle;
import android.app.ActionBar;


public class AmusementPark extends Activity implements SensorEventListener,
		LocationListener {

	/* Default Constants */
	private final String ACTIVITY_NAME = "canobielake";
	private final String TIME_OFFSET_PREFS_ID = "time_offset";
	private final String TIME_OFFSET_KEY = "timeOffset";

	/* UI Handles */
	public static EditText experimentInput;
	public static TextView rideName;
	private TextView time;
	private TextView values;
	private Button startStop;
	private ToggleButton gravity;
	public static EditText dataset; 
	public static EditText project;
	public static CheckBox projectLater;
	public static EditText sampleRate;
	public static EditText studentNumber;
	public static CheckBox isCanobie;
	
	/* Recording Constants */
	private final int SAMPLE_INTERVAL = 200;
	
	/*Values obtained from Configuration*/
	public static int projectNum = -1;
	public static String dataName = "";
	public static String rate = "200";
	public static String rideNameString = "NOT SET";
	public static String stNumber = "1";
	public static Boolean projectLaterChecked = false;
	public static Boolean canobieChecked = true;
	public static int spinnerid = 0;

	
	/* Managers and Their Variables */
	public static DataFieldManager dfm;
	private SensorManager mSensorManager;
	private LocationManager mLocationManager;
	private LocationManager mRoughLocManager;
	public static UploadQueue uq;
	private Vibrator vibrator;

	/* Other Important Objects */
	private MediaPlayer mMediaPlayer;
	private API api;
	public static Context mContext;
	private Waffle w;

	/* Work Flow Variables */
	private boolean isRunning = false;
	private static boolean useMenu = true;
	public static boolean setupDone = false;

	

	/* Recording Variables */
	private long srate = SAMPLE_INTERVAL;
	private boolean includeGravity = true;
	
	/* Action Bar */
	private static int actionBarTapCount = 0;
	private static boolean useDev = false;

	/* Start Activity Codes*/
	private final int QUEUE_UPLOAD_REQUESTED = 1;
	private final int SYNC_TIME_REQUESTED = 4;
	private final int SETUP_REQUESTED = 5;
	private final int LOGIN_REQUESTED = 6;
	
	private int elapsedSecs;

	/* Used with Sync Time */
	private long timeOffset = 0;	
	
	/* Used to set time elapsed */
	private String sec = "";
	private int min = 0;

	public static JSONArray dataSet;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Think pointer to this activity
		mContext = this;

		// Initialize everything you're going to need
		initVars();
		
		//setupDataFieldManager with all fields
		dfm.setUpDFMWithAllSensorFields(mContext);
		
		// Main Layout Button for Recording Data
		startStop.setOnLongClickListener(new OnLongClickListener() {

			@SuppressLint("NewApi")
			@Override
			public boolean onLongClick(View arg0) {
				if ((!setupDone)) {
					w.make("You must setup before recording data.",
							Waffle.LENGTH_LONG, Waffle.IMAGE_WARN);

					isRunning = false;
					return isRunning;

				} else {

					// Vibrate and Beep
					vibrator.vibrate(300);
					mMediaPlayer.setLooping(false);
					mMediaPlayer.start();

					

					// Stop the recording and reset UI if running
					if (isRunning) {
						isRunning = false;
						useMenu = true;	
						if (android.os.Build.VERSION.SDK_INT >= 11)
							invalidateOptionsMenu();

						// Unregister sensors to save battery
						mSensorManager.unregisterListener(AmusementPark.this);

						// Update the main button
						enableMainButton(false);

						// Reset main UI
						time.setText(getResources().getString(
								R.string.timeElapsed));
						values.setText(R.string.xyz);
								
						//enable gravity togglebutton
						gravity.setEnabled(true);
						
						/* Cancel the Recording and add to queue*/
						dataSet = dfm.stopRecording();
						int dataPointCount = dataSet.length();
						
						SharedPreferences mPrefs = getSharedPreferences(Setup.PROJ_PREFS_ID, 0);
						String projId = mPrefs.getString(Setup.PROJECT_ID, "");
						Date date = new Date();
						
						String name = dataName + rideNameString + " Gravity: " + ((includeGravity) ? "Included" : "Not Included");
						String description = "Time: " + getNiceDateString(date) + "\n" + "Number of Data Points: " + dataPointCount;
						Type type = Type.DATA;

						uq.addToQueue(name, description, type, dataSet, null, projId, null);
					
						showSummary(date);
						return isRunning;

						// Start the recording
					} else {
						srate = Integer.parseInt(rate);
						
						
						//new SensorCheckTask().execute();
						
						isRunning = true;
						useMenu = false;			
						
						//disable gravity togglebutton
						gravity.setEnabled(false);
						
						//disable menu
						if (android.os.Build.VERSION.SDK_INT >= 11)
							invalidateOptionsMenu();

						// Check to see if a valid project was chosen. If not,
						// (projectNum is -1) enable all fields for recording.
						
						if (projectNum == -1) {
							dfm.setUpDFMWithAllSensorFields(mContext);
						}

						// Create a file so that we can write results to the
						// sdCard
						prepWriteToSDCard(new Date());

						//enable sensors needed
						dfm.registerSensors();

						elapsedSecs = 0;

						startStop.setText(getResources().getString(R.string.stopString));
						startStop.setBackgroundResource(R.drawable.button_rsense_green);						
						
						new TimeElapsedTask().execute();
						
						initDfm();
						dfm.recordData(srate);
						
					}
					
					return isRunning;
				}
			}

		});
		
		gravity.setOnCheckedChangeListener(new OnCheckedChangeListener () {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				includeGravity = gravity.isChecked();				
			}
		});
		
	}
	
	private void enableMainButton(boolean enable) {
		if (enable) {

		} else {
			startStop.setText(getResources().getString(R.string.startString));
			startStop.setBackgroundResource(R.drawable.button_rsense);
			startStop.setTextColor(Color.parseColor("#0066FF"));
		}
	}

	/**
	 * Returns a nicely formatted date.
	 * 
	 * @param date
	 *            Date you wish to convert
	 * @return The date in string form: MM/dd/yyyy, HH:mm:ss
	 */
	String getNiceDateString(Date date) {

		SimpleDateFormat niceFormat = new SimpleDateFormat(
				"MM/dd/yyyy, HH:mm:ss", Locale.US);

		return niceFormat.format(date);
	}

	/**
	 * Prepares a file for writing date to the sdCard.
	 * 
	 * @param date
	 *            Time stamp for the file name
	 * @return Newly created file on sdCard
	 */
	private File prepWriteToSDCard(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy--HH-mm-ss",
				Locale.US);

		String dateString = sdf.format(date);

		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/iSENSE");

		if (!folder.exists()) {
			folder.mkdir();
		}
		
		String sdFileName = rideNameString + "-" + stNumber + "-"
				+ dateString + ".csv";
		File sdFile = new File(folder, sdFileName);

		return sdFile;

	}

	@Override
	public void onPause() {
		super.onPause();

		// Stop the current sensors to save battery.
		mLocationManager.removeUpdates(AmusementPark.this);
		mSensorManager.unregisterListener(AmusementPark.this);
		
		//Stop recording
		if (isRunning) {
			startStop.performLongClick();
		}


	}

	@Override
	public void onResume() {
		super.onResume();

		// Silently logs in the user to iSENSE
		CredentialManager.login(mContext, api);

		// Rebuilds the upload queue
		if (uq != null)
			uq.buildQueueFromFile();
		
		
		initLocManager();
	}
	
	@Override
	protected void onStart() {
		super.onStart();

		initLocManager();

	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	

	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupEnabled(0, useMenu);
		return true;
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.MENU_ITEM_SETUP:
			Intent iSetup = new Intent(AmusementPark.this, Configuration.class);
			startActivityForResult(iSetup, SETUP_REQUESTED);
			return true;
			
		case R.id.MENU_ITEM_UPLOAD:
			showUploadQueue();
			return true;
			
		case R.id.MENU_ITEM_ABOUT:
			// Shows the about dialog
			startActivity(new Intent(this, About.class));
			return true;
			
		case R.id.MENU_ITEM_HELP:
			// Shows the about dialog
			startActivity(new Intent(this, Help.class));
			return true;
			
		case R.id.MENU_ITEM_LOGIN:
			startActivityForResult(new Intent(getApplicationContext(),
					CredentialManager.class), LOGIN_REQUESTED);
			return true;
			
		case android.R.id.home:
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
				api.useDev(useDev);
				if (cdt != null)
					cdt.cancel();
				CredentialManager.login(this, api);
				actionBarTapCount = 0;
				break;
			}
			return true;
		
		default:
			return false;
		}
	}
	
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	/**
	 * Stores some data into our global objects as quickly as we get new points.
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (dfm != null) {
			dfm.updateValues(event);
		} else {
			Log.e("onSensorChanged ", "dfm is null");
		}
		if (isRunning) {
			if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION ||
				event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
				
			DecimalFormat oneDigit = new DecimalFormat("#,#00.0");
			String xPrepend = event.values[0] > 0 ? "+" : "";
			String yPrepend = event.values[1] > 0 ? "+" : "";
			String zPrepend = event.values[2] > 0 ? "+" : "";
			
			values.setText("Ax: " + xPrepend
					+ oneDigit.format(event.values[0]) + " " + "m/s^2" + "\nAy: " + yPrepend
					+ oneDigit.format(event.values[1]) + " " + "m/s^2" + "\nAz: " + zPrepend
					+ oneDigit.format(event.values[2]) + " " + "m/s^2");
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if (dfm != null) {
			dfm.updateLoc(location);
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	// Performs tasks after returning to main UI from previous activities
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == SYNC_TIME_REQUESTED) {
			if (resultCode == RESULT_OK) {
				timeOffset = data.getExtras().getLong("offset");
				SharedPreferences mPrefs = getSharedPreferences(
						TIME_OFFSET_PREFS_ID, 0);
				SharedPreferences.Editor mEditor = mPrefs.edit();
				mEditor.putLong(TIME_OFFSET_KEY, timeOffset);
				mEditor.commit();
			}
		} else if (requestCode == QUEUE_UPLOAD_REQUESTED) {
			boolean success = uq.buildQueueFromFile();
			if (!success) {
				w.make("Could not re-build queue from file!", Waffle.IMAGE_X);
			}
		} else if (requestCode == SETUP_REQUESTED) {
				rideName.setText("Ride: " + rideNameString);
				
				SharedPreferences mPrefs = getSharedPreferences(
						Setup.PROJ_PREFS_ID, 0);
				final SharedPreferences.Editor mEdit = mPrefs.edit();
				mEdit.putString(Setup.PROJECT_ID, Integer.toString(projectNum));
				mEdit.commit();
				
				//change fields for new project
				dfm.setProjID(projectNum);
				dfm.registerSensors();
				
		} else if (requestCode == LOGIN_REQUESTED) {
			
			
		}

	}

	/**
	 * Writes the passed in time to the main screen.
	 * 
	 * @param seconds
	 */ 
	public void setTime(int seconds) {
		elapsedSecs = seconds;
		
		min = seconds / 60;
		int secInt = seconds % 60;

		sec = "";
		if (secInt <= 9)
			sec = "0" + secInt;
		else
			sec = "" + secInt;
		

		runOnUiThread(new Runnable() {
		    public void run() {
		    	time.setText("Time Elapsed: " + min + ":" + sec);
		    }
		});

	}
	
	private class TimeElapsedTask extends AsyncTask<Void, Integer, Void> {
		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(Void... voids) {
			for(int time = 0; isRunning; time++ ) {
				setTime(time);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void voids) {
			
		}
	}
	
	
	

	/**
	 * Everything needed to be initialized for onCreate in one helpful function.
	 */
	@SuppressLint("NewApi")
	private void initVars() {

		api = API.getInstance();
		api.useDev(useDev);
		
		// Initialize DataFieldManager
		initDfm();
		
		// Initialize action bar customization for API >= 11
				if (android.os.Build.VERSION.SDK_INT >= 11) {
					ActionBar bar = getActionBar();

					// make the actionbar clickable
					bar.setDisplayHomeAsUpEnabled(true);
				}

		// Login to iSENSE
		CredentialManager.login(this, api);

		// Create a new upload queue
		uq = new UploadQueue(ACTIVITY_NAME, mContext, api);
		
		// OMG a button!
		startStop = (Button) findViewById(R.id.startStop);
		
		//Toggle button for gravity
		gravity = (ToggleButton) findViewById(R.id.tbGravity);
		gravity.setChecked(includeGravity);
		
		// Have some TVs. TextViews I mean.
		values = (TextView) findViewById(R.id.values);
		time = (TextView) findViewById(R.id.time);
		rideName = (TextView) findViewById(R.id.ridename);
		rideName.setText("Ride: " + rideNameString);

		// Start some managers
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Waffles ares UI messages, and fields are used in recording
		w = new Waffle(this);

		// Fire up the GPS chip (not literally)
		initLocManager();

		

		// Most important feature. Makes the button beep.
		mMediaPlayer = MediaPlayer.create(this, R.raw.beep);
	}

	

	
	
	//set up GPS
	/**
	 * Initialize the location manager
	 */
	private void initLocManager() {
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mRoughLocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
				&& mRoughLocManager
						.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {

			mLocationManager.requestLocationUpdates(
					mLocationManager.getBestProvider(c, true), 0, 0, AmusementPark.this);
			mRoughLocManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0, AmusementPark.this);
		} else {
			
		}
		
		dfm.updateLoc(new Location(mLocationManager.getBestProvider(c, true)));
		
	}
	
	
/**
 * Initialize DataFieldManager Object
 */
	private void initDfm() {
		SharedPreferences mPrefs = getSharedPreferences(Setup.PROJ_PREFS_ID, 0);
		String projectInput = mPrefs.getString(Setup.PROJECT_ID, "-1");
		dfm = new DataFieldManager(Integer.parseInt(projectInput), api, mContext);
		dfm.enableAllSensorFields();
	}
	
	
	/**
	 * Prompts the user to upload the rest of their content
	 * upon successful upload of data.
	 */
	private void showUploadQueue() {
		if (!uq.emptyQueue()) {
			Intent i = new Intent().setClass(mContext, QueueLayout.class);
			i.putExtra(QueueLayout.PARENT_NAME, uq.getParentName());
			startActivityForResult(i, QUEUE_UPLOAD_REQUESTED);
		} else {
			w.make("No data to upload!", Waffle.IMAGE_X);
		}
	}
	
	/**
	 * Turns elapsedMillis into readable strings.
	 * 
	 * @author jpoulin
	 */
	private class ElapsedTime {
		private String elapsedSeconds;
		private String elapsedMinutes;
		
		/**
		 * Everybody likes strings.
		 * 
		 * @param seconds
		 */		
		ElapsedTime(int seconds) {
			int minutes;
			
			minutes = seconds / 60;
			seconds %= 60;
			
			if (seconds < 10) {
				elapsedSeconds = "0" + seconds;
			} else {
				elapsedSeconds = "" + seconds;
			}


			if (minutes < 10) {
				elapsedMinutes = "0" + minutes;
			} else {
				elapsedMinutes = "" + minutes;
			}		
		}
		
	}

	/**
	 * Makes a summary dialog.
	 * @param date Time of upload
	 * @param sdFileName Name of the written csv
	 */
	private void showSummary(Date date) {
		ElapsedTime time = new ElapsedTime(elapsedSecs);
		
		Intent iSummary = new Intent(mContext, Summary.class);
		iSummary.putExtra("seconds", time.elapsedSeconds)
				.putExtra("minutes", time.elapsedMinutes)
				.putExtra("date", getNiceDateString(date))
				.putExtra("points", "" + dataSet.length());

		startActivity(iSummary);

	}


}
