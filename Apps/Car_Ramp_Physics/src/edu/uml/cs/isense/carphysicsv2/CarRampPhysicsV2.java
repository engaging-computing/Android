/***************************************************************************************************/
/***************************************************************************************************/
/**                                                                                               **/
/**      IIIIIIIIIIIII               iSENSE Car Ramp Physics App                 SSSSSSSSS        **/
/**           III                                                               SSS               **/
/**           III                    By: Michael Stowell                       SSS                **/
/**           III                    and Virinchi Balabhadrapatruni           SSS                 **/
/**           III                    Some Code From: iSENSE Amusement Park      SSS               **/
/**           III                                    App (John Fertita)          SSSSSSSSS        **/
/**           III                    Faculty Advisor:  Fred Martin                      SSS       **/
/**           III                    Group:            ECG,                              SSS      **/
/**           III                                      iSENSE                           SSS       **/
/**      IIIIIIIIIIIII               Property:         UMass Lowell              SSSSSSSSS        **/
/**                                                                                               **/
/***************************************************************************************************/
/***************************************************************************************************/

package edu.uml.cs.isense.carphysicsv2;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;


import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import edu.uml.cs.isense.carphysicsv2.dialogs.About;
import edu.uml.cs.isense.carphysicsv2.dialogs.Help;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.ClassroomMode;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.credentials.EnterName;
import edu.uml.cs.isense.dfm.DataFieldManager;
import edu.uml.cs.isense.objects.RPerson;
import edu.uml.cs.isense.proj.Setup;
import edu.uml.cs.isense.queue.QueueLayout;
import edu.uml.cs.isense.queue.UploadQueue;
import edu.uml.cs.isense.queue.QDataSet.Type;
import edu.uml.cs.isense.supplements.OrientationManager;
import edu.uml.cs.isense.waffle.Waffle;

public class CarRampPhysicsV2 extends Activity implements SensorEventListener,
		LocationListener {

	public static String projectNumber = "-1";
	public static final String DEFAULT_PROJ = "-1";
	public static boolean useDev = false;
	public static boolean promptForName = true;

	public static final String VIS_URL_PROD = "http://isenseproject.org/projects/";
	public static final String VIS_URL_DEV = "http://rsense-dev.cs.uml.edu/projects/";
	public static String baseDataSetUrl = "";
	public static String dataSetUrl = "";

	public static String RECORD_SETTINGS = "RECORD_SETTINGS";

	private Button startStop;
	private TextView values;
	public static Boolean running = false;

	private SensorManager mSensorManager;

	
	private Timer timeTimer;
	private int INTERVAL = 50;

	private DataFieldManager dfm;
	public API api;

	private int countdown;

	static String firstName = "";
	static String lastInitial = "";

	public static final int RESULT_GOT_NAME = 1098;
	public static final int UPLOAD_OK_REQUESTED = 90000;
	public static final int LOGIN_STATUS_REQUESTED = 6005;
	public static final int RECORDING_LENGTH_REQUESTED = 4009;
	public static final int PROJECT_REQUESTED = 9000;
	public static final int QUEUE_UPLOAD_REQUESTED = 5000;
	public static final int RESET_REQUESTED = 6003;
	public static final int SAVE_MODE_REQUESTED = 10005;
	public static final String ACCEL_SETTINGS = "ACCEL_SETTINGS";

//	private boolean timeHasElapsed = false;

	private MediaPlayer mMediaPlayer;

//	private int elapsedMillis = 0;

	DecimalFormat toThou = new DecimalFormat("######0.000");

	int i = 0;
	int len = 0;
	int len2 = 0;
	int length;

	ProgressDialog dia;
	double partialProg = 1.0;

	public static String nameOfDataSet = "";

	static boolean inPausedState = false;
	static boolean useMenu = true;
	static boolean setupDone = false;
	static boolean choiceViaMenu = false;
	static boolean dontToastMeTwice = false;
	static boolean exitAppViaBack = false;
	static boolean dontPromptMeTwice = false;
	
	private Handler mHandler;
	public static JSONArray dataSet;

	long currentTime;

	public static Context mContext;

	public static TextView loggedInAs;
	private Waffle w;
	public static boolean inApp = false;

	public static UploadQueue uq;

	public static Bundle saved;

	public static Menu menu;
	

	/* Action Bar */
	private static int actionBarTapCount = 0;

	/* Make sure url is updated when useDev is set. */
	void setUseDev(boolean useDev) {
		api.useDev(useDev);
	}

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		saved = savedInstanceState;
		mContext = this;
		

		api = API.getInstance();
		setUseDev(useDev);

		if (api.getCurrentUser() != null) {
			Runnable r = new Runnable() {
				public void run() {
					api.deleteSession();
					api.useDev(useDev);
				}
			};
			new Thread(r).start();
		}

		// Initialize action bar customization for API >= 11
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			ActionBar bar = getActionBar();

			// make the actionbar clickable
			bar.setDisplayHomeAsUpEnabled(true);
		}
		
		uq = new UploadQueue("carrampphysics", mContext, api);
		uq.buildQueueFromFile();

		w = new Waffle(mContext);

		CredentialManager.login(mContext, api);

		mHandler = new Handler();

		startStop = (Button) findViewById(R.id.startStop);

		values = (TextView) findViewById(R.id.values);

		SharedPreferences prefs = getSharedPreferences("RECORD_LENGTH", 0);
		length = countdown = prefs.getInt("length", 10);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		dfm.registerSensors(mSensorManager, CarRampPhysicsV2.this);

		if (savedInstanceState == null) {
			if (firstName.equals("") || lastInitial.equals("")) {
			
				Intent iEnterName = new Intent(this, EnterName.class);
				iEnterName.putExtra(EnterName.PREFERENCES_CLASSROOM_MODE,
						true);
				startActivityForResult(iEnterName, RESULT_GOT_NAME);
				
			}
		}

		initDfm();

		new DecimalFormat("#,##0.0");

		startStop.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {

				mMediaPlayer.setLooping(false);
					mMediaPlayer.start();
	
					if (running) {
							running = false;
							try{
							getWindow().clearFlags(
									WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
							} catch (Exception e){
								Log.e("onButtonPress", "Failed to remove layoutParams.FLAG_KEEP_SCREEN_ON");
							}
							
							dfm.stopRecording();
							int dataPointCount = dataSet.length();
							
							SharedPreferences mPrefs = getSharedPreferences(Setup.PROJ_PREFS_ID, 0);
							String projId = mPrefs.getString(Setup.PROJECT_ID, "");
							
							String dataName = firstName + " " + lastInitial;;
							String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
							String description = "Time: " + currentDateTimeString + "\n" + "Number of Data Points: " + dataPointCount;
							Type type = Type.DATA;

							uq.addToQueue(dataName, description, type, dataSet, null, projId, null);
	
							setupDone = false;
							useMenu = true;
							countdown = length;
	
							startStop.setText("Hold to Start");
	
							timeTimer.cancel();
							choiceViaMenu = false;
	
							startStop.setBackgroundResource(R.drawable.button_rsense);
	
					} else {
	
						OrientationManager.disableRotation(CarRampPhysicsV2.this);
						getWindow().addFlags(
								WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	
						startStop.setBackgroundResource(R.drawable.button_rsense_green);
	
						dataSet = new JSONArray();
						len = 0;
						len2 = 0;
						i = 0;
						
						initDfm();
						dfm.recordData(INTERVAL);

						useMenu = false;
	
						running = true;
						startStop.setText("" + countdown);
						
						timeTimer = new Timer();
						timeTimer.scheduleAtFixedRate(new TimerTask() {
	
							public void run() {
	
								if (i >= (length * (1000 / INTERVAL))) {
	
									timeTimer.cancel();
									
									CarRampPhysicsV2.this.runOnUiThread(new Runnable() {
									    public void run() {
											startStop.performLongClick();
									    }
									});
	
								} else {
	
									i++;
									len++;
									len2++;
	
									if (i % (1000 / INTERVAL) == 0) {
										mHandler.post(new Runnable() {
											@Override
											public void run() {
												startStop.setText("" + countdown);
											}
										});
										countdown--;
									}
									//TODO look into timestamp
									//f.timeMillis = currentTime + elapsedMillis;
		
								}
	
							}
						}, 0, INTERVAL);
						
				}
			
					return running;
		}
	});

		
	}

	@Override
	public void onPause() {
		super.onPause();
		if (timeTimer != null)
			timeTimer.cancel();
		inPausedState = true;
		if (running) {
			startStop.performLongClick();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (timeTimer != null)
			timeTimer.cancel();
		inPausedState = true;
	}


	@Override
	public void onStart() {
		super.onStart();
		inPausedState = false;

		mSensorManager.registerListener(CarRampPhysicsV2.this,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.setGroupEnabled(0, useMenu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.login:
			startActivityForResult(new Intent(this, CredentialManager.class),
					LOGIN_STATUS_REQUESTED);
			return true;
		case R.id.project_select:
			Intent setup = new Intent(this, Setup.class);
			setup.putExtra("constrictFields", true);
			setup.putExtra("app_name", "CRP");
			startActivityForResult(setup, PROJECT_REQUESTED);
			return true;
		case R.id.upload:
			manageUploadQueue();
			return true;
		case R.id.record_length:
			createSingleInputDialog("Change Recording Length", "",
					RECORDING_LENGTH_REQUESTED);
			return true;
		case R.id.changename:
			Intent iEnterName = new Intent(mContext, EnterName.class);
			SharedPreferences classPrefs = getSharedPreferences(
					ClassroomMode.PREFS_KEY_CLASSROOM_MODE, 0);
			iEnterName.putExtra(EnterName.PREFERENCES_CLASSROOM_MODE,
					classPrefs.getBoolean(
							ClassroomMode.PREFS_BOOLEAN_CLASSROOM_MODE, true));
			startActivityForResult(iEnterName, RESULT_GOT_NAME);
			return true;
		case R.id.reset:
			startActivityForResult(new Intent(this, ResetToDefaults.class),
					RESET_REQUESTED);
			return true;
		case R.id.about_app:
			startActivity(new Intent(this, About.class));
			return true;
		case R.id.helpMenuItem:
			startActivity(new Intent(this, Help.class));
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

				if (cdt != null)
					cdt.cancel();

				setUseDev(useDev);

				actionBarTapCount = 0;
				break;
			}

			return true;
		}

		return false;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (dfm != null) {
			dfm.updateValues(event);
		} else {
			Log.e("onSensorChanged ", "dfm is null");
		}
		
		if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION ||
			event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
			
		DecimalFormat oneDigit = new DecimalFormat("#,##0.0");
		String xPrepend = event.values[0] > 0 ? "+" : "";
		String yPrepend = event.values[1] > 0 ? "+" : "";
		String zPrepend = event.values[2] > 0 ? "+" : "";
		
		values.setText("Ax: " + xPrepend
				+ oneDigit.format(event.values[0]) + " " + "Ay: " + yPrepend
				+ oneDigit.format(event.values[1]) + " " + "Az: " + zPrepend
				+ oneDigit.format(event.values[2]) + " ");
		}
		

	}

	@Override
	public void onLocationChanged(Location location) {
		dfm.updateLoc(location);
	}

	public static int getApiLevel() {
		return android.os.Build.VERSION.SDK_INT;
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);
		dontPromptMeTwice = false;

		if (reqCode == PROJECT_REQUESTED) {
			if (resultCode == RESULT_OK) {
				SharedPreferences prefs = getSharedPreferences("PROJID", 0);
				projectNumber = prefs.getString("project_id", null);
				
				if (projectNumber == null) {
					projectNumber = DEFAULT_PROJ;
				}
				
				dfm.setProjID(Integer.parseInt(projectNumber));
				dfm.registerSensors(mSensorManager, CarRampPhysicsV2.this);

			}

		} else if (reqCode == QUEUE_UPLOAD_REQUESTED) {
			uq.buildQueueFromFile();

		} else if (reqCode == LOGIN_STATUS_REQUESTED) {
			if (resultCode == RESULT_OK) {

			}


		} else if (reqCode == RECORDING_LENGTH_REQUESTED) {
			if (resultCode == RESULT_OK) {
				length = Integer.parseInt(data.getStringExtra("input"));
				countdown = length;
				SharedPreferences prefs = getSharedPreferences("RECORD_LENGTH",
						0);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt("length", length);
				editor.putInt("Interval", INTERVAL);
				editor.commit();
			}
		} else if (reqCode == RESULT_GOT_NAME) {
			if (resultCode == RESULT_OK) {
				SharedPreferences namePrefs = getSharedPreferences(
						EnterName.PREFERENCES_KEY_USER_INFO, MODE_PRIVATE);
				
				if (namePrefs
						.getBoolean(
								EnterName.PREFERENCES_USER_INFO_SUBKEY_USE_ACCOUNT_NAME,
								true)) {
					RPerson user = api.getCurrentUser();

					firstName = user.name;
					lastInitial = "";

				} else {
					firstName = namePrefs.getString(
							EnterName.PREFERENCES_USER_INFO_SUBKEY_FIRST_NAME,
							"");
					lastInitial = namePrefs
							.getString(
									EnterName.PREFERENCES_USER_INFO_SUBKEY_LAST_INITIAL,
									"");
				}

			}
		} else if (reqCode == RESET_REQUESTED) {
			if (resultCode == RESULT_OK) {
				SharedPreferences prefs = getSharedPreferences("RECORD_LENGTH",
						0);
				countdown = length = prefs.getInt("length", 10);

				CredentialManager.login(this, api);

				SharedPreferences eprefs = getSharedPreferences("PROJID", 0);
				SharedPreferences.Editor editor = eprefs.edit();
				projectNumber = DEFAULT_PROJ;
				editor.putString("project_id", projectNumber);
				editor.commit();
				INTERVAL = 50;

				dfm.setUpDFMWithAllSensorFields(mContext);
				
				Intent iEnterName = new Intent(mContext, EnterName.class);
				SharedPreferences classPrefs = getSharedPreferences(
						ClassroomMode.PREFS_KEY_CLASSROOM_MODE, 0);
				iEnterName.putExtra(EnterName.PREFERENCES_CLASSROOM_MODE,
						classPrefs.getBoolean(
								ClassroomMode.PREFS_BOOLEAN_CLASSROOM_MODE, true));
				startActivityForResult(iEnterName, RESULT_GOT_NAME);

			}
			
		} 
	}

	
	private void manageUploadQueue() {

		if (!uq.emptyQueue()) {
			Intent i = new Intent().setClass(mContext, QueueLayout.class);
			i.putExtra(QueueLayout.PARENT_NAME, uq.getParentName());
			startActivityForResult(i, QUEUE_UPLOAD_REQUESTED);
		} else {
			w.make("There are no data to upload!", Waffle.LENGTH_LONG,
					Waffle.IMAGE_X);
		}
	}

	public void createMessageDialog(String title, String message, int reqCode) {
		Intent i = new Intent(mContext, MessageDialogTemplate.class);
		i.putExtra("title", title);
		i.putExtra("message", message);

		startActivityForResult(i, reqCode);
	}

	public void createSingleInputDialog(String title, String message,
			int reqCode) {

		Intent i = new Intent(mContext, SingleInputDialogTemplate.class);
		i.putExtra("title", title);
		i.putExtra("message", message);
		startActivityForResult(i, reqCode);

	}


	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
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
	
	
}