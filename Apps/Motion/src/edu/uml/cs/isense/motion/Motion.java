/***************************************************************************************************/
/***************************************************************************************************/
/**                                                                                               **/
/**      IIIIIIIIIIIII               iSENSE Car Ramp Physics App                 SSSSSSSSS        **/
/**           III                                                               SSS               **/
/**           III                    By: Bobby Donald, Michael Stowell         SSS                **/
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
package edu.uml.cs.isense.motion;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;

import java.text.DecimalFormat;
import java.util.Timer;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.ClassroomMode;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.credentials.EnterName;
import edu.uml.cs.isense.motion.dialogs.DurationDialog;
import edu.uml.cs.isense.motion.dialogs.Help;
import edu.uml.cs.isense.motion.dialogs.MessageDialogTemplate;
import edu.uml.cs.isense.motion.dialogs.Presets;
import edu.uml.cs.isense.motion.dialogs.RateDialog;
import edu.uml.cs.isense.motion.dialogs.ResetToDefaults;
import edu.uml.cs.isense.motion.fields.AccelFragment;
import edu.uml.cs.isense.motion.fields.AltFragment;
import edu.uml.cs.isense.motion.fields.HumidityFragment;
import edu.uml.cs.isense.motion.fields.LightFragment;
import edu.uml.cs.isense.motion.fields.LocFragment;
import edu.uml.cs.isense.motion.fields.MagFragment;
import edu.uml.cs.isense.motion.fields.PressureFragment;
import edu.uml.cs.isense.motion.fields.TempFragment;
import edu.uml.cs.isense.objects.RPerson;
import edu.uml.cs.isense.proj.ProjectManager;
import edu.uml.cs.isense.queue.QueueLayout;
import edu.uml.cs.isense.queue.UploadQueue;
import edu.uml.cs.isense.waffle.Waffle;


public class Motion extends ActionBarActivity {

	public static final String DEFAULT_PROJ = "-1";
	public static final int DEFAULT_RATE = 50;
	public static final int DEFAULT_LENGTH = 10;
	public static boolean useDev = false;
	public static boolean promptForName = true;

	public static final String VIS_URL_PROD = "http://isenseproject.org/projects/";
	public static final String VIS_URL_DEV = "http://rsense-dev.cs.uml.edu/projects/";
	public static String baseDataSetUrl = "";
	public static String dataSetUrl = "";

	public static String RECORD_SETTINGS = "RECORD_SETTINGS";

	private Button startStop;
	private Button uploadButton;
	private Button projNumB;
	private Button nameB;
	private Button rateB;
	private Button lengthB;
	public static Boolean running = false;

	//saved pref keys
	public static final String MY_SAVED_PREFERENCES = "MyPrefs" ;
	public static final String LENGTH_PREFS_KEY = "length";
	public static final String RATE_PREFS_KEY = "rate";

	private Timer timeTimer;

	public API api;

	static String firstName = "";
	static String lastInitial = "";

	public static final int RESULT_GOT_NAME = 1000;
	public static final int UPLOAD_OK_REQUESTED = 1001;
	public static final int LOGIN_STATUS_REQUESTED = 1002;
	public static final int RECORDING_LENGTH_REQUESTED = 1003;
	public static final int RECORDING_RATE_REQUESTED = 1004;
	public static final int PROJECT_REQUESTED = 1005;
	public static final int QUEUE_UPLOAD_REQUESTED = 1006;
	public static final int RESET_REQUESTED = 1007;
	public static final int SAVE_MODE_REQUESTED = 1008;
	public static final int PRESETS_REQUESTED = 1009;

	 ViewPager fields;
     PagerAdapter fieldAdapter;

	public static final String ACCEL_SETTINGS = "ACCEL_SETTINGS";

	private MediaPlayer mMediaPlayer;
	private Vibrator vibrator;

	DecimalFormat toThou = new DecimalFormat("######0.000");

	int i = 0;
	int len = 0;
	int len2 = 0;

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

	public static JSONArray dataSet;

	long currentTime;

	public static Context mContext;

	public static TextView loggedInAs;
	private Waffle w;
	public static boolean inApp = false;

	public static UploadQueue uq;

	public static Bundle saved;

	public static Menu menu;

    Intent service;

    //Receives info from library to update ui
    BroadcastReceiver receiver;

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

		//bool in resources is false in values-xlarge but true in values
		//this only allows devices with xlarge displays to put this activity in landscape
		if(getResources().getBoolean(R.bool.force_portrait)){
	        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    }

		//initialize intent for service
        service = new Intent(mContext, RecordingService.class);

		if (api.getCurrentUser() != null) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					api.deleteSession();
					api.useDev(useDev);
				}
			};
			new Thread(r).start();
		}

		// Initialize action bar customization for API >= 11
        //having issues with material design and logo, commenting out until I figure out why
//		if (android.os.Build.VERSION.SDK_INT >= 11) {
//			ActionBar bar = getSupportActionBar();
//
//			// make the actionbar clickable
//			bar.setDisplayHomeAsUpEnabled(true);
//		}

		if (RecordingService.running)
			useMenu = false;
		else
			useMenu = true;


		uq = new UploadQueue("motion", mContext, api);
		uq.buildQueueFromFile();

		w = new Waffle(mContext);

		CredentialManager.login(mContext, api);

		// Beep sound
		mMediaPlayer = MediaPlayer.create(this, R.raw.beep);

		// Vibrator for Long Click
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		startStop = (Button) findViewById(R.id.startStop);
		uploadButton = (Button) findViewById(R.id.b_upload);
		projNumB = (Button) findViewById(R.id.b_project);
		nameB = (Button) findViewById(R.id.b_name);
		rateB = (Button) findViewById(R.id.b_rate);
		lengthB = (Button) findViewById(R.id.b_length);
        fields = (ViewPager) findViewById(R.id.viewpager_fields);
        fieldAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        fields.setAdapter(fieldAdapter);

		if (RecordingService.running) {
			startStop.setBackgroundResource(R.drawable.button_rsense_green);
			startStop.setText("Recording");
		} else {
			startStop.setBackgroundResource(R.drawable.button_rsense);
			startStop.setText("Hold to Start");
		}

		SharedPreferences namePrefs = getSharedPreferences(
				EnterName.PREFERENCES_KEY_USER_INFO, MODE_PRIVATE);
		firstName = namePrefs.getString(
				EnterName.PREFERENCES_USER_INFO_SUBKEY_FIRST_NAME,
				"");
		lastInitial = namePrefs
				.getString(
						EnterName.PREFERENCES_USER_INFO_SUBKEY_LAST_INITIAL,
						"");

		if (firstName.length() == 0) {
			Intent iEnterName = new Intent(this, EnterName.class);
			iEnterName.putExtra(EnterName.PREFERENCES_CLASSROOM_MODE,
					true);
			startActivityForResult(iEnterName, RESULT_GOT_NAME);
		} else {
			nameB.setText(firstName + " " + lastInitial);
		}

		String projId = ProjectManager.getProject(mContext);

		if (projId.equals("-1")) {
			projNumB.setText("Generic Project");
		} else {
			projNumB.setText("Project: " + projId);
		}

		setRateText();
		setLengthText();

		 /* update UI with data passed back from service */
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.hasExtra("BUTTON")) {
                    String s = intent.getStringExtra("BUTTON");
                    startStop.setText(s);

                } else if(intent.hasExtra("BUTTONSTART")) {
					startStop.setBackgroundResource(R.drawable.button_rsense_green);
                    startStop.setText("Recording");
                    useMenu = false;
                    if (android.os.Build.VERSION.SDK_INT >= 11)
						invalidateOptionsMenu();

                } else if(intent.hasExtra("BUTTONSTOP")) {
					startStop.setBackgroundResource(R.drawable.button_rsense);
                    startStop.setText("Hold to Start");

        			useMenu = true;
        			if (android.os.Build.VERSION.SDK_INT >= 11)
						invalidateOptionsMenu();
                }
            }
        };

		new DecimalFormat("#,##0.0");

		startStop.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View arg0) {

				mMediaPlayer.setLooping(false);
				mMediaPlayer.start();

				// Vibrate and beep
				vibrator.vibrate(300);
				mMediaPlayer.setLooping(false);
				mMediaPlayer.start();

                if (RecordingService.running) {
					startStop.setBackgroundResource(R.drawable.button_rsense);
					startStop.setText("Hold to Start");
					useMenu = true;
					if (android.os.Build.VERSION.SDK_INT >= 11)
						invalidateOptionsMenu();
                    stopService(service);
                } else {
					startStop.setBackgroundResource(R.drawable.button_rsense_green);
					startStop.setText("Recording");
					useMenu = false;
					if (android.os.Build.VERSION.SDK_INT >= 11)
						invalidateOptionsMenu();
                    startService(service);
                }

				return running;
		}
	});


		uploadButton.setOnClickListener(new OnClickListener (){
			@Override
			public void onClick(View v) {
				//Launched the upload queue dialog
				manageUploadQueue();
			}
		});

		projNumB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Allows the user to pick a project to upload to
				Intent setup = new Intent(mContext, ProjectManager.class);
				setup.putExtra("constrictFields", true);
				startActivityForResult(setup, PROJECT_REQUESTED);
			}

		});

		nameB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Launch the dialog that allows users to enter his/her
				// firstname
				// and last initial
				Intent iEnterName = new Intent(mContext, EnterName.class);
				SharedPreferences classPrefs = getSharedPreferences(
						ClassroomMode.PREFS_KEY_CLASSROOM_MODE, 0);
				iEnterName.putExtra(EnterName.PREFERENCES_CLASSROOM_MODE,
						classPrefs.getBoolean(
								ClassroomMode.PREFS_BOOLEAN_CLASSROOM_MODE,
								true));
				startActivityForResult(iEnterName, RESULT_GOT_NAME);
			}

		});

		rateB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent rate = new Intent(mContext, RateDialog.class);
                startActivityForResult(rate, RECORDING_RATE_REQUESTED);
			}

		});

		lengthB.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(mContext, DurationDialog.class);
				startActivityForResult(i, RECORDING_LENGTH_REQUESTED);

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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}


	@Override
	public void onStart() {
		super.onStart();
		inPausedState = false;
	}

	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		super.onResume();
		//this sets the layout the correct layout for when it is not recording
		//this is relevant if recording finishes while the app is in the background
		if(!RecordingService.running) {
			startStop.setBackgroundResource(R.drawable.button_rsense);
	        startStop.setText("Hold to Start");

			useMenu = true;
			if (android.os.Build.VERSION.SDK_INT >= 11)
				invalidateOptionsMenu();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
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
		case R.id.login:
			startActivityForResult(new Intent(this, CredentialManager.class),
					LOGIN_STATUS_REQUESTED);
			return true;
		case R.id.upload:
			manageUploadQueue();
			return true;
		case R.id.reset:
			startActivityForResult(new Intent(this, ResetToDefaults.class),
					RESET_REQUESTED);
			return true;
		case R.id.presets:
			startActivityForResult(new Intent(this, Presets.class),
					PRESETS_REQUESTED);
		return true;
//		case R.id.about_app:
//			startActivity(new Intent(this, About.class));
//			return true;
		case R.id.helpMenuItem:
			startActivity(new Intent(this, Help.class));
			return true;
		case android.R.id.home:
			CountDownTimer cdt = null;

			// Give user 10 seconds to switch dev/prod mode
			if (actionBarTapCount == 0) {
				cdt = new CountDownTimer(5000, 5000) {
					@Override
					public void onTick(long millisUntilFinished) {
					}
					@Override
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



	public static int getApiLevel() {
		return android.os.Build.VERSION.SDK_INT;
	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);
		dontPromptMeTwice = false;

		if (reqCode == PROJECT_REQUESTED) {
			if (resultCode == RESULT_OK) {
				String projectNumber = ProjectManager.getProject(mContext);

				if (projectNumber.equals("-1")) {
					w.make("All Sensors Enabled", Waffle.IMAGE_CHECK);
					projNumB.setText("Generic Project");
				} else {
					w.make("Sensors Needed for Project " + projectNumber + " are Enabled", Waffle.IMAGE_CHECK);
					projNumB.setText("Project: " + projectNumber);
				}

			}

		} else if (reqCode == QUEUE_UPLOAD_REQUESTED) {
			uq.buildQueueFromFile();

		} else if (reqCode == LOGIN_STATUS_REQUESTED) {
			if (resultCode == RESULT_OK) {

			}


		} else if (reqCode == RECORDING_LENGTH_REQUESTED) {
			if (resultCode == RESULT_OK) {
				setLengthText();
			}
		} else if (reqCode == RECORDING_RATE_REQUESTED) {
			if (resultCode == RESULT_OK) {
				setRateText();
			}

		} else if (reqCode == PRESETS_REQUESTED) {
			if (resultCode == RESULT_OK) {
				//TODO presets set default field (loc for location) (accel sets field to accel)
				/*set project*/
				String projectNumber = data.getExtras().getString(Presets.PROJECT);
				ProjectManager.setProject(mContext, projectNumber);
				projNumB.setText("Project: " + projectNumber);
				w.make("Sensors Needed for Project " + projectNumber + " are Enabled", Waffle.IMAGE_CHECK);

				/*set rate*/
				SharedPreferences ratePrefs = getSharedPreferences(MY_SAVED_PREFERENCES, 0);
				SharedPreferences.Editor editor2 = ratePrefs.edit();
				int rate = data.getExtras().getInt(Presets.RATE);
				editor2.putInt(RATE_PREFS_KEY, rate);
				editor2.commit();
				setRateText();

				/*set recording length*/
				SharedPreferences lengthPrefs = getSharedPreferences(MY_SAVED_PREFERENCES, 0);
				SharedPreferences.Editor editor3 = lengthPrefs.edit();
				int length = data.getExtras().getInt(Presets.LENGTH);
				editor3.putInt(LENGTH_PREFS_KEY, length);
				editor3.commit();
				setLengthText();
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

					nameB.setText(firstName + " " + lastInitial);


				} else {
					firstName = namePrefs.getString(
							EnterName.PREFERENCES_USER_INFO_SUBKEY_FIRST_NAME,
							"");
					lastInitial = namePrefs
							.getString(
									EnterName.PREFERENCES_USER_INFO_SUBKEY_LAST_INITIAL,
									"");

					nameB.setText(firstName + " " + lastInitial);

				}
			}

		} else if (reqCode == RESET_REQUESTED) {
			if (resultCode == RESULT_OK) {
				/*Logout*/
				CredentialManager.logout(this, api);

				/*reset project*/
				ProjectManager.setProject(mContext, DEFAULT_PROJ);
				projNumB.setText("Generic Project");

				/*reset rate*/
				SharedPreferences ratePrefs = getSharedPreferences(MY_SAVED_PREFERENCES, 0);
				SharedPreferences.Editor editor2 = ratePrefs.edit();
				editor2.putInt(RATE_PREFS_KEY, DEFAULT_RATE);
				editor2.commit();
				setRateText();

				/*reset recording length*/
				SharedPreferences lengthPrefs = getSharedPreferences(MY_SAVED_PREFERENCES, 0);
				SharedPreferences.Editor editor3 = lengthPrefs.edit();
				editor3.putInt(LENGTH_PREFS_KEY, DEFAULT_LENGTH);
				editor3.commit();
				setLengthText();

				/*reset name*/
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
			w.make("There is no data to upload!", Waffle.LENGTH_LONG,
					Waffle.IMAGE_X);
		}
	}

	public void createMessageDialog(String title, String message, int reqCode) {
		Intent i = new Intent(mContext, MessageDialogTemplate.class);
		i.putExtra("title", title);
		i.putExtra("message", message);

		startActivityForResult(i, reqCode);
	}

private void setRateText() {
	SharedPreferences ratePrefs = getSharedPreferences(MY_SAVED_PREFERENCES, 0);
	int rate = ratePrefs.getInt(RATE_PREFS_KEY, DEFAULT_RATE);

	switch (rate) {
	case 50:
		rateB.setText("50 mili");
		break;
	case 100:
		rateB.setText("100 mili");
		break;
	case 500:
		rateB.setText("500 mili");
		break;
	case 1000:
		rateB.setText("1 sec");
	break;
	case 5000:
		rateB.setText("5 sec");
		break;
	case 30000:
		rateB.setText("30 sec");
		break;
	default:
		rateB.setText("1 min");
		break;
	}
}

private void setLengthText() {
	SharedPreferences lengthPrefs = getSharedPreferences(MY_SAVED_PREFERENCES, 0);
	int length = lengthPrefs.getInt(LENGTH_PREFS_KEY, DEFAULT_LENGTH);

	switch (length) {
	case 1:
		lengthB.setText("1 sec");
		break;
	case 2:
		lengthB.setText("2 sec");
		break;
	case 5:
		lengthB.setText("5 sec");
		break;
	case 10:
		lengthB.setText("10 sec");
	break;
	case 30:
		lengthB.setText("30 sec");
		break;
	case 60:
		lengthB.setText("1 min");
		break;
	default:
		lengthB.setText("Push to Stop");
		break;
	}
}


    /**
     * A simple pager adapter that is used to look at sensor values in real time
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
        	switch(position) {
        		case 0:
            		return new AccelFragment();
        		case 1:
        			return new LocFragment();
        		case 2:
        			return new AltFragment();
        		case 3:
        			return new MagFragment();
        		case 4:
        			return new PressureFragment();
        		case 5:
        			return new TempFragment();
        		case 6:
        			return new HumidityFragment();
        		case 7:
        			return new LightFragment();
        		default:
        			return null;
        	}
        }

        @Override
        public int getCount() {
        	//num of fragments (0-7)
            return 8;
        }
    }
}