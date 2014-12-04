package edu.uml.cs.isense.motion;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONArray;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.dfm.DataFieldManager;
import edu.uml.cs.isense.proj.ProjectManager;
import edu.uml.cs.isense.queue.QDataSet.Type;


public class RecordingService extends Service {
	private DataFieldManager dfm;
    private JSONArray dataSet;

    final int NOTIFICATION_ID = 5001;

    public static boolean running = false;

    /* Distance and Velocity */
    float distance = 0;
    float velocity = 0;
    float deltaTime = 0;
    boolean bFirstPoint = true;
    float totalDistance = 0;
    long startTime;

    private MediaPlayer mMediaPlayer;
    private Vibrator vibrator;


    Intent intent;
    Context serviceContext;

    Timer recordLength;

    private String dataSetName = "";
    CountDownTimer mTimer;

    LocalBroadcastManager broadcaster;
    static final public String RESULT = "edu.uml.cs.isense.motion.RecordingService.REQUEST_PROCESSED";

    @Override
	public void onCreate() {
		super.onCreate();
        //initialize dfm which handles project fields and data recording
        initDfm();

        serviceContext = this;

        // Beep sound
        mMediaPlayer = MediaPlayer.create(this, R.raw.beep);

        // Vibrator for Long Click
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        //enables all sensors and gps based on the fields
		dfm.registerSensors();

        broadcaster = LocalBroadcastManager.getInstance(this);

        updateButtonStart("Start");

        /*Persistent notification while recording*/
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            Intent intent = new Intent(Motion.mContext, Motion.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(Motion.mContext, NOTIFICATION_ID, intent, 0);

            Notification.Builder builder = new Notification.Builder(getApplicationContext());
            builder.setContentIntent(pendingIntent);
            builder.setContentTitle("iSENSE Motion");
            builder.setContentText("Recording Data");
            builder.setTicker("Started Recording");
            builder.setSmallIcon(R.drawable.ic_stat_name);
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            builder.setLargeIcon(bm);
            builder.setContentIntent(pendingIntent);
            builder.setOngoing(true);
            builder.setPriority(0);
            Notification notification = builder.build();
            NotificationManager notificationManger =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManger.notify(NOTIFICATION_ID, notification);
        }



	}

    /**
	 * Initialize DataFieldManager Object
	 */
		private void initDfm() {
			API api = API.getInstance();
			String projectInput = ProjectManager.getProject(this);
			dfm = new DataFieldManager(Integer.parseInt(projectInput), api, Motion.mContext);
			dfm.enableAllSensorFields();
		}

    public void updateButtonTimer(String message) {
        Intent intent = new Intent(RESULT);
        if(message != null)
            intent.putExtra("BUTTON", message);
        broadcaster.sendBroadcast(intent);
    }

    public void updateButtonStart(String message) {
        Intent intent = new Intent(RESULT);
        if(message != null)
            intent.putExtra("BUTTONSTART", message);
        broadcaster.sendBroadcast(intent);
    }

    public void updateButtonStop(String message) {
        Intent intent = new Intent(RESULT);
        if(message != null)
            intent.putExtra("BUTTONSTOP", message);
        broadcaster.sendBroadcast(intent);
    }

    /**
     * This is called to start recording data
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	int length; //length of dataset
    	int rate; //interval between datapoints

        // Vibrate and beep
        vibrator.vibrate(300);
        mMediaPlayer.setLooping(false);
        mMediaPlayer.start();

        running = true;

		SharedPreferences prefs = getSharedPreferences(Motion.MY_SAVED_PREFERENCES,
				0);
		length = prefs.getInt(Motion.LENGTH_PREFS_KEY, 10);

		SharedPreferences prefs2 = getSharedPreferences(Motion.MY_SAVED_PREFERENCES, 0);
		rate = prefs2.getInt(Motion.RATE_PREFS_KEY, 50);

		dfm.setProjID(Integer.parseInt(ProjectManager.getProject(this)));

        //record data
        dfm.recordData(rate);

        if(length != -1) {
        	mTimer =  new CountDownTimer(length * 1000, 1000) {

        	     @Override
				public void onTick(long millisUntilFinished) {
        	    	 updateButtonTimer("" + millisUntilFinished / 1000);
        	     }

        	     @Override
				public void onFinish() {
        	         serviceContext.stopService(new Intent(serviceContext, RecordingService.class));
        	     }

        	  }.start();
        }

        return super.onStartCommand(intent, flags, startId);
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        if (running) {
        	running = false;

            // Vibrate and beep
            vibrator.vibrate(300);
            mMediaPlayer.setLooping(false);
            mMediaPlayer.start();

        	//Stops timer from still counting down after recording has stopped
        	if (mTimer != null) {
        		mTimer.cancel();
        	}

            // Cancel the recording timer and get back the data
            dataSet = dfm.stopRecording();

            updateButtonStop("Stop");

            // Create the name of the session using the entered name
            dataSetName = Motion.firstName + " " + Motion.lastInitial;
			String description = "Number of Data Points: " + dataSet.length();
			Type type = Type.DATA;

			//add new dataset to queue
			Motion.uq.buildQueueFromFile();
			Motion.uq.addToQueue(dataSetName, description, type, dataSet, null, ProjectManager.getProject(this), null, false);
        }

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(NOTIFICATION_ID);

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


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }





    // formats numbers to 2 decimal points
    double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }


}


