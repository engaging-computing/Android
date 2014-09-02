package edu.uml.cs.isense.carphysicsv2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONArray;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.dfm.DataFieldManager;
import edu.uml.cs.isense.proj.Setup;
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

    Intent intent;
    Context serviceContext;
    
    Timer recordLength;

    private String dataSetName = "";
    CountDownTimer mTimer;

    LocalBroadcastManager broadcaster;
    static final public String RESULT = "edu.uml.cs.isense.carphysicsv2.Recording_Service.REQUEST_PROCESSED";

    /**
     * This is called when service is first created. The location manager is initiated but no data is
     * being recorded at this point
     */
    @SuppressLint("NewApi")
    @Override
	public void onCreate() {
		super.onCreate();    
        //initialize dfm which handles project fields and data recording
        initDfm();
        
        serviceContext = this;
        
        //enables all sensors and gps based on the fields
		dfm.registerSensors();

        broadcaster = LocalBroadcastManager.getInstance(this);
        
        updateButtonStart("Start");

        /*Persistent notification while recording*/
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            Intent intent = new Intent(CarRampPhysicsV2.mContext, CarRampPhysicsV2.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(CarRampPhysicsV2.mContext, NOTIFICATION_ID, intent, 0);

            Notification.Builder builder = new Notification.Builder(getApplicationContext());
            builder.setContentIntent(pendingIntent);
            builder.setContentTitle("iSense DataWalk");
            builder.setContentText("Recording Data");
            builder.setTicker("Started Recording");
            builder.setSmallIcon(R.drawable.ic_launcher);
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
			
			SharedPreferences mPrefs = getSharedPreferences(Setup.PROJ_PREFS_ID, 0);
			String projectInput = mPrefs.getString(Setup.PROJECT_ID, "-1");
			dfm = new DataFieldManager(Integer.parseInt(projectInput), api, CarRampPhysicsV2.mContext);
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
    	
        running = true;
        
		SharedPreferences prefs = getSharedPreferences("RECORD_LENGTH",
				0);
		length = prefs.getInt("length", 10);
		
		SharedPreferences prefs2 = getSharedPreferences("RECORD_RATE", 0);
		rate = prefs2.getInt("rate", 50);

		dfm.setProjID(Integer.parseInt(CarRampPhysicsV2.projectNumber));
        
        //record data        
        dfm.recordData(rate);
        
        if(length != -1) {
        	mTimer =  new CountDownTimer(length * 1000, 1000) {

        	     public void onTick(long millisUntilFinished) {
        	    	 updateButtonTimer("" + millisUntilFinished / 1000);
        	     }

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

        	//Stops timer from still counting down after recording has stopped
        	if (mTimer != null) {
        		mTimer.cancel();
        	}
        	
            // Cancel the recording timer and get back the data
            dataSet = dfm.stopRecording();
            
            updateButtonStop("Stop");

            // Create the name of the session using the entered name
            dataSetName = CarRampPhysicsV2.firstName + " " + CarRampPhysicsV2.lastInitial;
			String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
			String description = "Time: " + currentDateTimeString + "\n" + "Number of Data Points: " + dataSet.length();
			Type type = Type.DATA;

			//add new dataset to queue
			CarRampPhysicsV2.uq.buildQueueFromFile();
			CarRampPhysicsV2.uq.addToQueue(dataSetName, description, type, dataSet, null, CarRampPhysicsV2.projectNumber, null);
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


