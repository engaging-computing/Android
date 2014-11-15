/***************************************************************************************************/
/***************************************************************************************************/
/**                                                                                               **/
/**      IIIIIIIIIIIII               iSENSE Writer App                           SSSSSSSSS        **/
/**           III                                                               SSS               **/
/**           III                    By: Bobby Donald                          SSS                **/
/**           III                                                             SSS                 **/
/**           III                   									        SSS               **/
/**           III                                    				             SSSSSSSSS        **/
/**           III                    Faculty Advisor:  Fred Martin                      SSS       **/
/**           III                    Group:            ECG,                              SSS      **/
/**           III                                      iSENSE                           SSS       **/
/**      IIIIIIIIIIIII               Property:         UMass Lowell              SSSSSSSSS        **/
/**                                                                                               **/
/***************************************************************************************************/
/***************************************************************************************************/

package edu.uml.cs.isense.writer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.objects.RProjectField;
import edu.uml.cs.isense.proj.ProjectManager;
import edu.uml.cs.isense.queue.QDataSet.Type;
import edu.uml.cs.isense.queue.QueueLayout;
import edu.uml.cs.isense.queue.UploadQueue;
import edu.uml.cs.isense.waffle.Waffle;

public class ManualEntry extends Activity implements LocationListener {
    public static Context mContext;
    private API api;
    private Boolean useDev = false;
    private LinearLayout datapointsLayout;
    private ArrayList<RProjectField> fields;
    private Button addField;
    private Button save;
    private EditText datasetName;
    private int datapoints = 0;
    private Waffle w;
    public static UploadQueue uq;
    private LocationManager mLocationManager;

    /* Action Bar */
    private static int actionBarTapCount = 0;
    public static final int LOGIN_STATUS_REQUESTED = 6005;
    public static final int PROJECT_REQUESTED = 6009;
    public static final int QUEUE_UPLOAD_REQUESTED = 7021;

    private static final String DEFAULT_PROJ = "514";

    @SuppressLint("NewApi")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_entry);

        mContext = this;
        api = API.getInstance();
        w = new Waffle(mContext);

        uq = new UploadQueue("ManualEntry", mContext, api);
        uq.buildQueueFromFile();

        initializeLocationManager();


        CredentialManager.login(mContext, api);
        addField = (Button) findViewById(R.id.adddatapoint);
        save = (Button) findViewById(R.id.upload);
        datasetName = (EditText) findViewById(R.id.dataset_name);
        datasetName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                datasetName.setError(null);
            }

        });

//        // Initialize action bar customization for API >= 11
//        if (android.os.Build.VERSION.SDK_INT >= 11) {
//            ActionBar bar = getSupportActionBar();
//
//            // make the actionbar clickable
//            bar.setDisplayHomeAsUpEnabled(true);
//        }

        datapointsLayout = (LinearLayout) findViewById(R.id.datapoints_sv);
        if (fields == null) {
            new getNewFieldsTask().execute();
        } else {
            addFields();
        }

        addField.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (fields.size() != 0) {
                    addFields();
                }
            }

        });
        save = (Button) findViewById(R.id.save);

        save.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (fields.size() != 0) {
                    if (!datasetName.getText().toString().equals("")) {
                        JSONArray uploadData = getDataFromScreen();

                        String dataSetName = datasetName.getText().toString();
                        String description = "Number of Data Points: " + uploadData.length();
                        Type type = Type.DATA;

                        int projectID = Integer.parseInt(ProjectManager.getProject(mContext));

                        //add new dataset to queue
                        uq.buildQueueFromFile();
                        uq.addToQueue(dataSetName, description, type, uploadData, null, Integer.toString(projectID), null, true);

                        clearFields();
                        new getNewFieldsTask().execute();
                    } else {
                        datasetName.setError("No Name Entered");
                    }
                }
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
            Intent setup = new Intent(mContext, ProjectManager.class);
            setup.putExtra("showSelectLater", false);
            this.startActivityForResult(setup, PROJECT_REQUESTED);
            return true;
        }

        if (id == android.R.id.home) {
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

                    api.useDev(useDev);

                    clearFields();
                    new getNewFieldsTask().execute();

                    actionBarTapCount = 0;
                    break;
            }

        }

        if (id == R.id.MENU_ITEM_UPLOAD) {
            manageUploadQueue();
        }
        return true;
    }


    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        if (reqCode == LOGIN_STATUS_REQUESTED) {
            if (resultCode == RESULT_OK) {

            }
        } else if (reqCode == PROJECT_REQUESTED) {
            if (resultCode == RESULT_OK) {
                clearFields();
                new getNewFieldsTask().execute();
            }
        } else if (reqCode == QUEUE_UPLOAD_REQUESTED) {
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

        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout datapoint = new LinearLayout(mContext);
        FrameLayout dataPointFrame = new FrameLayout(mContext);

        dataPointFrame = (FrameLayout) getLayoutInflater().inflate(R.layout.data_point, null);
        datapoint = (LinearLayout) dataPointFrame.findViewById(R.id.ll_data_point);

        //Set Datapoint number
        TextView datapointnumber = new TextView(mContext);
        datapointnumber = (TextView) datapoint.findViewById(R.id.pointNumber);
        datapointnumber.setText("Data Point: " + datapoints);

        Button removeDataPoint = (Button) datapoint.findViewById(R.id.deletePoint);
        removeDataPoint.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                if (datapoints == 1) {
                    w.make("Must have one data point", Waffle.LENGTH_LONG, Waffle.IMAGE_X);
                } else {
                    final FrameLayout dataPointFrame = (FrameLayout) v.getParent().getParent().getParent();

                    Animation animation = AnimationUtils.loadAnimation(mContext, android.R.anim.slide_out_right);
                    animation.setDuration(200);

                    dataPointFrame.startAnimation(animation);
                    datapointsLayout.removeView(dataPointFrame);
                    renumberDataPoints();

                }
            }

        });


        int projectID = Integer.parseInt(ProjectManager.getProject(mContext));
        if (fields.size() == 0 && projectID != -1) {
            w.make("This Project Doesn't Have Any Fields", Waffle.IMAGE_X);
            ViewGroup noFieldError = new RelativeLayout(mContext);
            inflater.inflate(R.layout.no_fields, noFieldError);
            datapoint.addView(noFieldError, 1);

            datapointsLayout.addView(dataPointFrame, datapoints - 1);
        } else {

            for (int i = 0; i < fields.size(); i++) {
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
                    datapoint.addView(singlefield, i + 1);

                } else if (field.type == RProjectField.TYPE_TEXT) {
                    //if no restrictions we get back a string "null"
                    if (field.restrictions == "null") {
                        ViewGroup singlefield = new RelativeLayout(mContext);
                        inflater.inflate(R.layout.field, singlefield);

                        TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);

                        tv.setText(field.name + ":");
                        datapoint.addView(singlefield, i + 1);

                    } else {

                        ViewGroup singlefield = new RelativeLayout(mContext);
                        inflater.inflate(R.layout.field_restriction, singlefield);

                        TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);
                        Spinner mSpinner = (Spinner) singlefield.findViewById(R.id.field_spinner);

                        String restricts = field.restrictions;
                        restricts = (String) restricts.subSequence(2, restricts.length() - 2);
                        String delims = "\",\" *";
                        String[] splitFields = restricts.split(delims);

                        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, splitFields); //selected item will look like a spinner set from XML
                        mSpinner.setAdapter(spinnerArrayAdapter);

                        tv.setText(field.name + ":");
                        datapoint.addView(singlefield, i + 1);
                    }

                } else if (field.type == RProjectField.TYPE_NUMBER) {
                    ViewGroup singlefield = new RelativeLayout(mContext);
                    inflater.inflate(R.layout.field, singlefield);

                    EditText et = (EditText) singlefield.findViewById(R.id.field_et);
                    TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);

                    et.setInputType(InputType.TYPE_CLASS_NUMBER);
                    tv.setText(field.name + ":");
                    datapoint.addView(singlefield, i + 1);

                } else if (field.type == RProjectField.TYPE_LON) {
                    ViewGroup singlefield = new RelativeLayout(mContext);
                    inflater.inflate(R.layout.field, singlefield);

                    EditText et = (EditText) singlefield.findViewById(R.id.field_et);
                    TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);

                    et.setEnabled(false);
                    if (this.getLocation() != null) {
                        et.setText("" + this.getLocation().getLongitude());
                    } else {
                        et.setHint("No GPS Signal");
                    }

                    tv.setText(field.name + ":");
                    datapoint.addView(singlefield, i + 1);

                } else if (field.type == RProjectField.TYPE_LAT) {
                    ViewGroup singlefield = new RelativeLayout(mContext);
                    inflater.inflate(R.layout.field, singlefield);

                    EditText et = (EditText) singlefield.findViewById(R.id.field_et);
                    TextView tv = (TextView) singlefield.findViewById(R.id.field_tv);

                    et.setEnabled(false);
                    if (this.getLocation() != null) {
                        et.setText("" + this.getLocation().getLatitude());
                    } else {
                        et.setHint("No GPS Signal");
                    }
                    tv.setText(field.name + ":");
                    datapoint.addView(singlefield, i + 1);
                }
            }

            datapointsLayout.addView(dataPointFrame, datapoints - 1);
        }
    }

    /**
     * Datapoint was removed from screen so now we need to go through and renumber them
     */
    void renumberDataPoints() {
        datapoints--;

        for (int i = 0; i < datapoints; i++) {
            TextView pointNumber = (TextView) datapointsLayout.getChildAt(i).findViewById(R.id.pointNumber);
            pointNumber.setText("Data Point: " + (i + 1));
        }

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

            int projectID = Integer.parseInt(ProjectManager.getProject(mContext));

            if (projectID == -1) {
                projectID = Integer.valueOf(DEFAULT_PROJ);
                ProjectManager.setProject(mContext, DEFAULT_PROJ);
            }

            try {
                fields = api.getProjectFields(projectID);
            } catch (Exception e) {
                e.printStackTrace();
                fields = new ArrayList<RProjectField>();
            }

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


        if (fields.size() == 0) {
            return data;
        } else {

            for (int i = 0; i < datapointsLayout.getChildCount(); i++) {
                JSONArray dataPoint = new JSONArray();

                FrameLayout datapointFrameLayout = (FrameLayout) datapointsLayout.getChildAt(i);
                LinearLayout datapointlayout = (LinearLayout) datapointFrameLayout.getChildAt(0);

                for (int j = 0; j < fields.size(); j++) {
                    RelativeLayout fieldlayout = (RelativeLayout) datapointlayout.getChildAt(j + 1);
                    EditText etData = (EditText) fieldlayout.findViewById(R.id.field_et);

                    //try to get data from edit text for a field and add to json
                    try {
                        if (etData == null) {
                            dataPoint.put("");
                        } else {
                            dataPoint.put(j, etData.getText());
                        }
                    } catch (JSONException e) {
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

            return data;
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

    public Location getLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (locationManager != null) {
            //includes locations from GPS, AGPS
            Location lastKnownGPSLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownGPSLocation != null) {
                return lastKnownGPSLocation;
            } else {
                //includes locations from AGPS, CellID, WiFi MACID
                Location lastKnownNetworkLocation =  locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                return lastKnownNetworkLocation;
            }
        } else {
            return null;
        }
    }

    void initializeLocationManager() {
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //location will come from best provider available. (could be gps, network, or passive)
        mLocationManager.requestLocationUpdates(
                mLocationManager.getBestProvider(c, true), 0, 0, ManualEntry.this);

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            //best provider is not gps (must be network or passive) inform the user
            w.make("GPS is turned off using "+ mLocationManager.getBestProvider(c, true) + " provider" , Waffle.IMAGE_WARN);
        }

    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeLocationManager();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //turn off gps to save battery
        if (mLocationManager != null)
            mLocationManager.removeUpdates(ManualEntry.this);
    }

}
