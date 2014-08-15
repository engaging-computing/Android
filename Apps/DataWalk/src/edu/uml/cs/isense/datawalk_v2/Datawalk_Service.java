package edu.uml.cs.isense.datawalk_v2;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONArray;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.dfm.DataFieldManager;
import edu.uml.cs.isense.proj.Setup;
import edu.uml.cs.isense.queue.QDataSet.Type;


public class Datawalk_Service extends Service {
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private MyLocationListener locationListener;
    private MySensorListener sensorListener;
    
	private DataFieldManager dfm;

    private float accel[];
    private JSONArray dataSet;

    public static boolean running = false;

    /* Distance and Velocity */
    float distance = 0;
    float velocity = 0;
    float deltaTime = 0;
    boolean bFirstPoint = true;
    float totalDistance = 0;
    long startTime;

    Intent intent;

    private String dataSetName = "";

    LocalBroadcastManager broadcaster;
    static final public String DATAWALK_RESULT = "edu.uml.cs.isense.datawalk_v2.Datawalk_Service.REQUEST_PROCESSED";

    /**
     * This is called when service is first created. The location manager is initiated but no data is
     * being recorded at this point
     */
    @SuppressLint("NewApi")
    @Override
	public void onCreate() {
		super.onCreate();
        Log.e("oncreate", "");


        // initialize GPS and Sensor managers
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        
        //initialize dfm which handles project fields and data recording
        initDfm();

        initLocationManager();

        initSensorManager();

        broadcaster = LocalBroadcastManager.getInstance(this);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            Intent intent = new Intent(DataWalk.mContext, DataWalk.class);
            //PendingIntent pendingIntent = PendingIntent.getActivity(this, 01, intent, Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(DataWalk.mContext, 01, intent, 0);

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
            notificationManger.notify(01, notification);
        }



	}
    
    /**
	 * Initialize DataFieldManager Object
	 */
		private void initDfm() {
			API api = API.getInstance();
			
			SharedPreferences mPrefs = getSharedPreferences(Setup.PROJ_PREFS_ID, 0);
			String projectInput = mPrefs.getString(Setup.PROJECT_ID, "-1");
			dfm = new DataFieldManager(Integer.parseInt(projectInput), api, DataWalk.mContext);
			dfm.enableAllSensorFields();
		}

    public void updateDataPoints(String message) {
        Intent intent = new Intent(DATAWALK_RESULT);
        if(message != null)
            intent.putExtra("VELOCITY", message);
        broadcaster.sendBroadcast(intent);
    }

    public void updateDistance(String message) {
        Intent intent = new Intent(DATAWALK_RESULT);
        if(message != null)
            intent.putExtra("TIME", message);
        broadcaster.sendBroadcast(intent);
    }

    public void updateVelocity(String message) {
        Intent intent = new Intent(DATAWALK_RESULT);
        if(message != null)
            intent.putExtra("DISTANCE", message);
        broadcaster.sendBroadcast(intent);
    }

    public void updateTime(String message) {
        Intent intent = new Intent(DATAWALK_RESULT);
        if(message != null)
            intent.putExtra("POINTS", message);
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
        Log.e("onStartCommand", "");
        running = true;

		dfm.setProjID(Integer.parseInt(DataWalk.projectID));
        
        //record data
        runRecordingTimer(intent);
        
        dfm.recordData(1000);
        
        return super.onStartCommand(intent, flags, startId);
    }




    @Override
    public void onDestroy() {
        Log.e("onDestroy", "");


        if (running) {

        	running = false;

            // Cancel the recording timer
            dataSet = dfm.stopRecording();

            // Create the name of the session using the entered name
            dataSetName = DataWalk.firstName + " " + DataWalk.lastInitial;

            // Save the newest DataSet to the Upload Queue if it has at
            // least 1 point
			
			String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
			String description = "Time: " + currentDateTimeString + "\n" + "Number of Data Points: " + dataSet.length();
			Type type = Type.DATA;

			DataWalk.uq.addToQueue(dataSetName, description, type, dataSet, null, DataWalk.projectID, null);
        }

        if (mLocationManager != null)
            mLocationManager.removeUpdates(locationListener);

        if (mSensorManager != null)
            mSensorManager.unregisterListener(sensorListener);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(01);

        super.onDestroy();
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





    /**
     * Runs the main timer that records data and updates the main UI every
     * second.
     */
    void runRecordingTimer(Intent intent) {

        // Prepare new containers where our recorded values will be stored
        dataSet = new JSONArray();
        accel = new float[4];

        // Reset timer variables
        startTime = System.currentTimeMillis();

        // Rajia Set First Point to false hopefully this will fix the big
        // velocity issue
        bFirstPoint = true;

        // Initialize Total Distance
        totalDistance = 0;
        
    }








    /**
     * Sets up the locations manager so that it request GPS permission if
     * necessary and gets only the most accurate points.
     */
    private void initLocationManager() {

        // Set the criteria to points with fine accuracy
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        locationListener = new MyLocationListener();


        mLocationManager.requestLocationUpdates(
                mLocationManager.getBestProvider(criteria, true), 0, 0,
                locationListener);

}

    private void initSensorManager() {
        //MySensorListener is an object of the class I made down below
        sensorListener = new MySensorListener();

        Log.e("Datawalk Service: ", "about to try and register SensorManger");

        // Start the sensor manager so we can get accelerometer data
        mSensorManager.registerListener(sensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);


    }

    // formats numbers to 2 decimal points
    double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }

    /**
     * You can not implement a LocationListener to a service so that is why
     * There is a separate class here that implements a LocationListener
     */
    public class MyLocationListener implements LocationListener
    {

        public void onLocationChanged(final Location location) {
    		dfm.updateLoc(location);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

    }

    /**
     * You can not implement a SensorEventListener to a service so that is why
     * There is a separate class here that implements a SensorEventListener
     */
    public class MySensorListener implements SensorEventListener {

        @Override
        public void onSensorChanged(SensorEvent event) {
        	if (dfm != null) {
    			dfm.updateValues(event);
    		} else {
    			Log.e("onSensorChanged ", "dfm is null");
    		}

            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                try {
                    accel[0] = event.values[0];
                    accel[1] = event.values[1];
                    accel[2] = event.values[2];
                    accel[3] = (float) Math.sqrt((float) (Math.pow(accel[0], 2)
                            + Math.pow(accel[1], 2) + Math.pow(accel[2], 2)));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }
}


