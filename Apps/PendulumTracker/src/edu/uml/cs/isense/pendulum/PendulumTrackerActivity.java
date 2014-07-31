package edu.uml.cs.isense.pendulum;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.comm.Connection;
import edu.uml.cs.isense.comm.uploadInfo;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.credentials.EnterName;
import edu.uml.cs.isense.objects.RProjectField;
import edu.uml.cs.isense.queue.QDataSet;
import edu.uml.cs.isense.queue.QueueLayout;
import edu.uml.cs.isense.queue.UploadQueue;
import edu.uml.cs.isense.waffle.Waffle;

public class PendulumTrackerActivity extends Activity implements
// OnTouchListener, CvCameraViewListener2 {
		OnLongClickListener, CvCameraViewListener2 {

	private static final String TAG = "PendulumTracker::Activity";
	
	private Waffle w;

	public static UploadQueue uq;

	Paint paint;

	public static Context mContext;

	// iSENSE member variables
	// use development site
	Boolean useDevSite = false;
	
	// iSENSE uploader
	API api;
	
	private TextView initInstr;

	// create session name based upon first name and last initial user enters
	static String firstName = "";
	static String lastInitial = "";
	private static final int ENTERNAME_REQUEST = 1098;
	private static final int LOGIN_REQUESTED  = 2000;
	private final int QUEUE_UPLOAD_REQUESTED = 2001;
	
	//Project fields pulled from project when user starts recording
	
	Boolean sessionNameEntered = false;

	private static String experimentNumber = "29"; // production = 29, dev = 39
		
	private String dateString;

	// upload progress dialogue
	ProgressDialog dia;
	// JSON array for uploading pendulum position data,
	// accessed from ColorBlobDetectionView
	public static JSONArray mDataSet = new JSONArray();

	// OpenCV

	// displayed image width and height
	private int mImgWidth = 0;
	private int mImgHeight = 0;

	private boolean mIsColorSelected = false;
	private Mat mRgba;
	private Scalar mBlobColorRgba;
	private Scalar mBlobColorHsv;

	private MarkerDetector mDetector;
	private Mat mSpectrum;
	private Size SPECTRUM_SIZE;
	private Scalar CONTOUR_COLOR;

	private CameraBridgeViewBase mOpenCvCameraView;
	static volatile boolean mDataCollectionEnabled = false; // volatile because
															// written on 1
															// thread, read-only
															// on another
	private static boolean mSessionCreated = false;
	private boolean mDisplayStatus = false;
	private boolean mEnableTouchMode = false;

	// start / stop icons
	Menu menu;
	MenuItem startStopButton;
	Drawable startIcon;
	Drawable stopIcon;

	Handler mHandler;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				mOpenCvCameraView
				// .setOnTouchListener(PendulumTrackerActivity.this);
						.setOnLongClickListener(PendulumTrackerActivity.this); // is
																				// like
																				// setting
																				// button.setOnTouchListener()
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public PendulumTrackerActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);

		// set context (for starting new Intents,etc)
		mContext = this;
		
		// iSENSE network connectivity stuff
		api = API.getInstance();
		api.useDev(false);
//		pf = api.getProjectFields(Integer.parseInt(experimentNumber));

		w = new Waffle(mContext); //Waffle is our version of android toast (message that pops up on screen)
		
		// Create a new upload queue
		uq = new UploadQueue("PendulumTrackerActivity", mContext, api);
		uq.buildQueueFromFile();

		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// set CBD activity content from xml layout file.
		// all Views, e.g. JavaCameraView will be inflated (e.g. created)
		setContentView(R.layout.color_blob_detection_surface_view);

		// think base class pointer: make mOpenCvCameraView become a
		// JavaCameraView
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);

		// set higher level camera parameters
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.enableFpsMeter();
		// mOpenCvCameraView.setMaxFrameSize(1280,720);
		// mOpenCvCameraView.setMaxFrameSize(640, 480); // debug!
		mOpenCvCameraView.setMaxFrameSize(320, 240);

		

		// TextView for instruction overlay
		initInstr = (TextView) findViewById(R.id.instructions);
		initInstr.setVisibility(View.VISIBLE);

		// set start and stop icons for data collections
		startIcon = getResources().getDrawable(R.drawable.start_icon);
		stopIcon = getResources().getDrawable(R.drawable.stop_icon);
		
		//Loggin to api if info is saved in prefs
		CredentialManager.login(mContext, api);
		
		//Pull fields from project that is selected
//		pf = api.getProjectFields(Integer.parseInt(experimentNumber));
	
		// Event handler
		mHandler = new Handler();
		
		// Create session with first name/last initial
		if (PendulumTrackerActivity.mSessionCreated == false) {
			if (firstName.length() == 0 || lastInitial.length() == 0) {
				// Boolean dontPromptMeTwice = true;
				startActivityForResult(new Intent(mContext, EnterName.class), // used to be login activity
						ENTERNAME_REQUEST);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		// if (mOpenCvCameraView != null)
		// mOpenCvCameraView.disableView();
	}

	// this is called any time an activity starts interacting with the user.
	// OpenCV library
	// reloaded anytime activity is resumed (e.g. brought to forefront)
	@Override
	public void onResume() {
		super.onResume();
		
		if (uq != null)
			uq.buildQueueFromFile();

		Log.i(TAG, "Trying to load OpenCV library");
		if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_5, this,
				mLoaderCallback)) {
			Log.e(TAG, "Cannot connect to OpenCV Manager");
		}
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {

		mImgWidth = width;
		mImgHeight = height;

		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mDetector = new MarkerDetector();

		mSpectrum = new Mat();
		mBlobColorRgba = new Scalar(255);
		mBlobColorHsv = new Scalar(255);
		SPECTRUM_SIZE = new Size(200, 64);
		CONTOUR_COLOR = new Scalar(255, 0, 0, 255);

	}

	public void onCameraViewStopped() {
		mRgba.release();
	}

	@Override
	public boolean onLongClick(View v) {
		return false;
	}

	// invoked when camera frame delivered
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) { // processFrame(VideoCapture
																// vc)
		boolean useGrey = true; // debug = false;
		boolean debug = false; // debug = true;
		int contourSize = -1;

		if (useGrey) {
			Point point = new Point(0, 0);

			// get latest camera frame
			mRgba = inputFrame.gray();

			// get location of detected points
			point = mDetector.processGrey(mRgba);

			// ---- DEBUG -----
			if (debug) {
				mRgba = mDetector.getLastDebugImg();
				Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_GRAY2RGB);
				contourSize = 2;
				this.drawDetectedContours(contourSize);
			}
			// ------------------
			else
				// convert grey image to color so we can draw color overlay
				Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_GRAY2RGB); // current
																		// frame

			if (mDataCollectionEnabled) {

				addStatusOverlay(mRgba);
				// add data point to final data set
				
				Point[] params = {point};
				new RecordPointsTask().execute(params);
				
//				// yScale, -xScale
//				if (point.x != 0 && point.y != 0) {
//					// shift x-axis so center vertical axis is set to x=0 in
//					// pendulum coordinates
//					final int shiftX = (int) (mImgWidth / 2);
//					this.addDataPoint(point.x - shiftX, point.y);
//				}

				// Make TextView disappear
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						initInstr.setVisibility(View.GONE);
					}
				});
			} else {
				this.addBoxOverlay(mRgba);

				// Make TextView disappear
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						initInstr.setVisibility(View.VISIBLE);
					}
				});
			}

			// TODO: fix flipping of y-axis
			Core.circle(mRgba, new Point(point.x, -point.y), 7, new Scalar(255,
					0, 0, 255), 2);

		} else {
			mRgba = inputFrame.rgba();

			if (mIsColorSelected) {
				mDetector.process(mRgba);

				this.drawDetectedContours(contourSize);
				// List<MatOfPoint> contours = mDetector.getContours();;
				// Log.e(TAG, "Contours count: " + contours.size());
				// Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

				Mat colorLabel = mRgba.submat(4, 68, 4, 68);
				colorLabel.setTo(mBlobColorRgba);

				Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70,
						70 + mSpectrum.cols());
				mSpectrum.copyTo(spectrumLabel);

			}

		}

		return mRgba;
	}

	private class RecordPointsTask extends AsyncTask<Point, Void, String> {

		ProgressDialog dia;

		@Override
		protected void onPreExecute() {
		}
		
		@Override
		protected String doInBackground(Point...params) {
			Point point = params[0];
			
			// yScale, -xScale
			if (point.x != 0 && point.y != 0) {
				// shift x-axis so center vertical axis is set to x=0 in
				// pendulum coordinates
				final int shiftX = (int) mImgWidth / 2;
				if (mDataCollectionEnabled ) {
					addDataPoint(point.x - shiftX, point.y);
				}
			}
			return null; //strings[0];
		}

		@Override
		protected void onPostExecute(String sdFileName) {

			
		}
	}
	
	
	@SuppressWarnings("unused")
	private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
		Mat pointMatRgba = new Mat();
		Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
		Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL,
				4);

		return new Scalar(pointMatRgba.get(0, 0));

	}

	void drawDetectedContours(int contourSize) {
		// if contourSize = -1, contour will be filled
		List<MatOfPoint> contours = mDetector.getContours();
		Imgproc.drawContours(mRgba, contours, contourSize, CONTOUR_COLOR);
	}

	// ------ screen overlays ------------------------

	void addBoxOverlay(Mat img) {
		// makes this 10% of width?
		// final int boxSize = 10;
		int boxSize = (int) (0.1 * img.width());

		final Point centerUL = new Point(img.width() / 2 - boxSize,
				img.height() / 2 - boxSize / 8);
		final Point centerLR = new Point(img.width() / 2 + boxSize,
				img.height() - 5);

		Core.rectangle(img, centerUL, centerLR, new Scalar(255, 0, 0, 255), 2);

		// Core.putText(img, new String("center pendulum in box"), new
		// Point(img.width()/2 - 3*boxSize, img.height()/2 - boxSize),
		// 0/* don't know what font this is!x */, 0.5, new Scalar(255, 0, 0,
		// 255), 1);

	}

	void addStatusOverlay(Mat img) {

		if (mDisplayStatus == true) {
			// Core.putText(img, new String("[COLLECTING DATA]"), new Point(0,
			// img.height() - 10 ),
			// 0/* CV_FONT_HERSHEY_COMPLEX */, 0.15, new Scalar(0, 255, 0, 255),
			// 2);
			Core.circle(mRgba, new Point(10, mRgba.height() - 10), 2,
					new Scalar(0, 255, 0, 255), -1);
			mDisplayStatus = false;
		} else {
			// Core.putText(img, new String("[COLLECTING DATA]"), new Point(0,
			// img.height() - 10 ),
			// 0/* CV_FONT_HERSHEY_COMPLEX */, 0.15, new Scalar(0, 255, 0, 255),
			// 1);
			Core.circle(mRgba, new Point(10, mRgba.height() - 10), 3,
					new Scalar(0, 255, 0, 255), -1);

			mDisplayStatus = true;
		}
	}

	// ------ iSENSE upload/ActionBar/menu stuff-------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu, menu);
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		// item.set
		switch (item.getItemId()) {
		// STOP experiment and data collection and
		// UPLOAD data
		/*
		 * case R.id.menu_upload:
		 * 
		 * mDataCollectionEnabled = false;
		 * 
		 * new LoginBeforeUploadTask().execute(); return true;
		 */
		// START experiment and data collection
		case R.id.menu_start:

			// Selected item becomes start/stop button (for onActivityResult())
			startStopButton = item;

			// Create session with first name/last initial
			if (PendulumTrackerActivity.mSessionCreated == false) {
				if (firstName.length() == 0 || lastInitial.length() == 0) {
					// Boolean dontPromptMeTwice = true;
					startActivityForResult(new Intent(mContext, EnterName.class), // used to be login activity
							ENTERNAME_REQUEST);
				}

				return true;
			}

			// START data collection
			if (PendulumTrackerActivity.mDataCollectionEnabled == false) {
				// be sure to disable data collection before uploading data to
				// iSENSE
				mDataCollectionEnabled = true;

				// set STOP button and text
				item.setIcon(stopIcon);
				item.setTitle(R.string.stopCollection);

			} else {
				// enable data collection before uploading data to iSENSE
				mDataCollectionEnabled = false;

				// set START button and text
				item.setIcon(startIcon);
				item.setTitle(R.string.startCollection);
				
				
				if (mDataSet.length() > 0) {
					Log.e("Start Stop button pressed", "About to try and add to queue");
					new AddToQueueTask().execute();					
				} else {
					w.make("You must first START data collection to upload data.",
							Waffle.LENGTH_LONG, Waffle.IMAGE_X);
				}
				
			}
			
			return true;
			
		case R.id.menu_upload:
		//Show upload queue
		manageUploadQueue();
		
		return true;
		case R.id.menu_login:
			startActivityForResult(new Intent(getApplicationContext(),
					CredentialManager.class), LOGIN_REQUESTED);
			return true;	
			
		case R.id.menu_exit:

			// Exit app neatly
			// this.finish(); // this only exits Activity not app completely.
			exitNeatly();
			return true;

		case R.id.menu_instructions:

			String strInstruct = "Center at-rest pendulum in center of image. Select 'Start data collection button' to start. Pull pendulum back to left or right edge of image and release when selecting 'OK'. Select 'Stop and upload to iSENSE' to stop. ";
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			// chain together various setter methods to set the dialog
			// characteristics
			builder.setMessage(strInstruct)
					.setTitle("Instructions:")
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								// @Override
								public void onClick(DialogInterface dialog,
										int id) {
									// grab position of target and pass it along
									// If this were Cancel for
									// setNegativeButton() , just do nothin'!

								}

							});

			// get the AlertDialog from create()
			AlertDialog dialog = builder.create();
			dialog.show(); // make me appear!

			return true;
		}
		return true;
	}

	// do these things after an Activity is finished
	public void onActivityResult(int reqCode, int resultCode, Intent data) {

		super.onActivityResult(reqCode, resultCode, data);

		// Enter session name
		if (reqCode == ENTERNAME_REQUEST) {

			if (resultCode == RESULT_OK) {

				PendulumTrackerActivity.mSessionCreated = true;

				Toast.makeText(
						PendulumTrackerActivity.this,
						"Session created. Press 'Start Data Collection' to begin!",
						Toast.LENGTH_SHORT).show();
			}
		} else if (reqCode == QUEUE_UPLOAD_REQUESTED) {
			uq.buildQueueFromFile();
		} else if (reqCode == LOGIN_REQUESTED) {
		
		} else {
			if (resultCode == RESULT_OK) {
				new AddToQueueTask().execute();
			}
		}
	}

	void exitNeatly() {
		// kill process so app completely restarts next time & maintains no
		// state
		int pid = android.os.Process.myPid();
		android.os.Process.killProcess(pid);
	}





	static void addDataPoint(double x, double y) {

		JSONArray dataPoint = new JSONArray();
		// Calendar c = Calendar.getInstance();
		long currentTime = (long) System.currentTimeMillis(); // (c.getTimeInMillis()
																// /*-
																// 14400000*/);
		
			/* Convert floating point to String to send data via HTML */
			try {
				/* Time */dataPoint.put("u " + currentTime);
				/* Posn-x */dataPoint.put(x);
				/* Posn-y */dataPoint.put(y);
				
				//TODO Hardcoding fields is bad. This app needs to use dfm
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		
		
		mDataSet.put(dataPoint);

		Log.i(TAG, "--------------- ADDING DATA POINT ---------------");

	}


	// Calls the api primitives for actual uploading
				private Runnable uploader = new Runnable() {

					@Override
					public void run() {

						SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy, HH:mm:ss");
						Date dt = new Date();
						dateString = sdf.format(dt);
						
						final SharedPreferences mPrefs = getSharedPreferences(
										EnterName.PREFERENCES_KEY_USER_INFO,
										Context.MODE_PRIVATE);
					
						String nameOfSession = mPrefs.getString(EnterName.PREFERENCES_USER_INFO_SUBKEY_FIRST_NAME, "") 
												+ mPrefs.getString(EnterName.PREFERENCES_USER_INFO_SUBKEY_LAST_INITIAL, "")
												+ " - " + dateString;
			
						// Create name from time stamp
						String name = "Test Name";//dataName;
						
						Log.e("DATASET", mDataSet.toString());
						
						Date date = new Date();
						
						// Saves data to queue for later upload
						QDataSet ds = new QDataSet(nameOfSession, "Pendulum Tracker",QDataSet.Type.DATA,
								mDataSet.toString(), null, experimentNumber, null);

			            ds.setRequestDataLabelInOrder(true);

						uq.addDataSetToQueue(ds);
					}

				};

				
	
	/**
	 * Prompts the user to upload the rest of their content
	 * upon successful upload of data.
	 */
	private void manageUploadQueue() {
		if (!uq.emptyQueue()) {
			Intent i = new Intent().setClass(mContext, QueueLayout.class);
			i.putExtra(QueueLayout.PARENT_NAME, uq.getParentName());
			startActivityForResult(i, QUEUE_UPLOAD_REQUESTED);
		} else {
			w.make("No data to upload!", Waffle.IMAGE_X);
		}
	}
	
	/**
	 * Uploads data to iSENSE or something.
	 * 
	 * @author jpoulin
	 */
	private class AddToQueueTask extends AsyncTask<String, Void, String> {

		ProgressDialog dia;

		@Override
		protected void onPreExecute() {

			dia = new ProgressDialog(PendulumTrackerActivity.this);
			dia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dia.setMessage("Please wait while your data and media saved to Queue");
			dia.setCancelable(false);
			dia.show();

		}
		
		@Override
		protected String doInBackground(String... strings) {
			uploader.run();
			return null; //strings[0];
		}

		@Override
		protected void onPostExecute(String sdFileName) {

			dia.setMessage("Done");
			dia.dismiss();
			
			w.make("Data Saved to Queue", Waffle.LENGTH_SHORT,
					Waffle.IMAGE_CHECK);
			manageUploadQueue();
			
		}
	}
}

