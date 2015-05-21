package edu.uml.cs.isense.dfm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import edu.uml.cs.isense.R;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.objects.RProjectField;

/**
 * The DataFieldManager class is designed to, as its name implies, manage how
 * data is associated with project fields on the iSENSE website. It provides
 * field matching, organizing and formatting data sets, applying sensor
 * compatibility checks, and writing data sets to a .csv file.
 *
 * @author iSENSE Android Development Team
 */
@SuppressLint("UseValueOf")
public class DataFieldManager extends Application {
    private MyListener mListener;

	private float rawAccel[] = new float[4];
	private float rawMag[] = new float[3];
	private final float accel[] = new float[4];
	private float orientation[] = new float[3];
	private final float mag[] = new float[3];
	private String temperature = "";
	private String pressure = "";
	private String light = "";
	private Location loc;
	private Location prevLoc;
	private float distance = 0;
	private float totalDistance = 0;
	private float velocity = 0;
	private float humidity = 0;

    private SensorManager mSensorManager;
    private LocationManager mLocationManager;

	long dataSetRate = 0;

	private int projID;
	private final API api;
	private Context mContext;

	private ArrayList<RProjectField> projFields;
	private LinkedList<String> order;
	private LinkedList<String> realOrder; // the actual fields in the project,
											// used for .csv file header writing
	private LinkedList<Long> fieldIDs; // IDs for the fields in order, in order
	private Fields f;

	private final String CSV_DELIMITER = "-:;_--:-;-;_::-;";

	public static JSONArray dataSet;
	private Timer recordingTimer;


	/**
	 * Boolean array of size 19 containing a list of fields enabled for
	 * recording data. See the {@link edu.uml.cs.isense.dfm.Fields Fields} class
	 * for a list of the constants associated with this boolean array's
	 * respective indices. By default, each field is disabled.
	 *
	 * To enable a particular field for recording from your class, perform an
	 * operation such as:
	 *
	 * <pre>
	 * {@code
	 *  myDFMInstance.enabledFields[Fields.ACCEL_X] = true;
	 * }
	 * </pre>
	 */
	public boolean[] enabledFields = { false, false, false, false, false,
			false, false, false, false, false, false, false, false, false,
			false, false, false, false, false, false, false, false };

	/**
	 * Constructor for the DataFieldManager class.
	 *
	 * @param projID
	 *            - The ID of the project to be associated with this
	 *            DataFieldManager, or -1 for no associated project.
	 * @param api
	 *            - An instance of the {@link edu.uml.cs.isense.comm.API} class.
	 * @param mContext
	 *            - The context of the class containing the DataFieldManager
	 *            object instance.
	 * @return An instance of DataFieldManager.
	 */
	public DataFieldManager(int projID, API api, Context mContext) {
		this.projID = projID;
		this.api = api;
		this.order = new LinkedList<String>();
		this.realOrder = new LinkedList<String>();
		this.fieldIDs = new LinkedList<Long>();
		this.mContext = mContext;
		this.f = new Fields();

		if (projID == -1) {
			setUpDFMWithAllSensorFields(mContext);
		} else {
			//TODO setup dfm with needed fields
		}

	}

	/**
	 * Creates a list, stored in this DataFieldManager instance's "order"
	 * object, of matched fields from the iSENSE project with the instance's
	 * "projID".
	 *
	 * If no associated project is passed in (-1), the order array contains all
	 * possible fields to be recorded. Otherwise, the order array contains all
	 * fields that could be matched string-wise with the associated project's
	 * fields.
	 *
	 * NOTE: Ensure you call this method before recording data, or otherwise you
	 * will be given blank data back from the
	 * {@link edu.uml.cs.isense.dfm.DataFieldManager#putData() putData()}
	 * method. Error checking may be added such as:
	 *
	 * <pre>
	 * {@code
	 *  if (myDFMInstance.getOrderList().size() == 0)
	 *     myDFMInstance.getOrder();
	 * }
	 * </pre>
	 *
	 * to prevent such a bug from occurring.
	 *
	 * Additionally, if you intend on calling this function from within an
	 * AsyncTask, call
	 * {@link edu.uml.cs.isense.dfm.DataFieldManager#getOrderWithExternalAsyncTask()
	 * getOrderWithExternalAsyncTask()} instead.
	 */
	public void getOrder() {
		if (!order.isEmpty())
			return;

		if (projID == -1) {
			order.clear();
			order.add(mContext.getString(R.string.time));
			order.add(mContext.getString(R.string.accel_x));
			order.add(mContext.getString(R.string.accel_y));
			order.add(mContext.getString(R.string.accel_z));
			order.add(mContext.getString(R.string.accel_total));
			order.add(mContext.getString(R.string.latitude));
			order.add(mContext.getString(R.string.longitude));
			order.add(mContext.getString(R.string.magnetic_x));
			order.add(mContext.getString(R.string.magnetic_y));
			order.add(mContext.getString(R.string.magnetic_z));
			order.add(mContext.getString(R.string.magnetic_total));
			order.add(mContext.getString(R.string.heading_deg));
			order.add(mContext.getString(R.string.heading_rad));
			order.add(mContext.getString(R.string.temperature_c));
			order.add(mContext.getString(R.string.pressure));
			order.add(mContext.getString(R.string.altitude));
			order.add(mContext.getString(R.string.luminous_flux));
			order.add(mContext.getString(R.string.temperature_f));
			order.add(mContext.getString(R.string.velocity));
			order.add(mContext.getString(R.string.distance));
			order.add(mContext.getString(R.string.humidity));

		} else {
			// Execute a new task
			new GetOrderTask().execute();
		}
	}

	/**
	 * Use this function instead of getOrder() if and only if you are calling
	 * this function in an AsyncTask.
	 *
	 * See the {@link edu.uml.cs.isense.dfm.DataFieldManager#getOrder()
	 * getOrder()} function for more details.
	 */
	public void getOrderWithExternalAsyncTask() {
		if (!order.isEmpty())
			return;

		if (projID == -1) {
			order.clear();
			order.add(mContext.getString(R.string.time));
			order.add(mContext.getString(R.string.accel_x));
			order.add(mContext.getString(R.string.accel_y));
			order.add(mContext.getString(R.string.accel_z));
			order.add(mContext.getString(R.string.accel_total));
			order.add(mContext.getString(R.string.latitude));
			order.add(mContext.getString(R.string.longitude));
			order.add(mContext.getString(R.string.magnetic_x));
			order.add(mContext.getString(R.string.magnetic_y));
			order.add(mContext.getString(R.string.magnetic_z));
			order.add(mContext.getString(R.string.magnetic_total));
			order.add(mContext.getString(R.string.heading_deg));
			order.add(mContext.getString(R.string.heading_rad));
			order.add(mContext.getString(R.string.temperature_c));
			order.add(mContext.getString(R.string.pressure));
			order.add(mContext.getString(R.string.altitude));
			order.add(mContext.getString(R.string.luminous_flux));
			order.add(mContext.getString(R.string.temperature_f));
			order.add(mContext.getString(R.string.velocity));
			order.add(mContext.getString(R.string.distance));
			order.add(mContext.getString(R.string.humidity));

		} else {
			// Function is being called within an AsyncTask already, so
			// no need to create a new task for the API call
			projFields = api.getProjectFields(projID);
			getProjectFieldOrder();

		}
	}

	/**
	 * Sets the DataFieldManager's order array based not on project field
	 * matching but rather the field's returned from from user field matching.
	 *
	 * @param input
	 *            A field list built from the FieldMatching dialog.
	 */
	public void setOrder(String input) {
		this.order = new LinkedList<String>();

		String[] fields = input.split(",");

		for (String s : fields) {
			order.add(s);
		}
	}

	/**
	 * Creates a row of data from the Fields object this class instance
	 * contains. This function performs no field matching and assumes you are
	 * only calling it with the intention of saving it in the data saver.
	 *
	 * @return The row of data in the form of a JSONArray that is to be
	 *         re-organized at upload time.
	 */
	public JSONArray putData() {

		JSONArray dataJSON = new JSONArray();

		try {
			if (enabledFields[Fields.TIME])
				dataJSON.put("u " + f.timeMillis);
			else
				dataJSON.put("");

			if (enabledFields[Fields.ACCEL_X] && f.accel_x != null)
				dataJSON.put(f.accel_x);
				else
				dataJSON.put("");

			if (enabledFields[Fields.ACCEL_Y] && f.accel_y != null)
				dataJSON.put(f.accel_y);
			else
				dataJSON.put("");

			if (enabledFields[Fields.ACCEL_Z] && f.accel_z != null)
				dataJSON.put(f.accel_z);
			else
				dataJSON.put("");

			if (enabledFields[Fields.ACCEL_TOTAL] && f.accel_total != null)
				dataJSON.put(f.accel_total);
			else
				dataJSON.put("");

			if (enabledFields[Fields.LATITUDE] && f.latitude != 0)
				dataJSON.put(f.latitude);
			else
				dataJSON.put("");

			if (enabledFields[Fields.LONGITUDE] && f.longitude != 0)
				dataJSON.put(f.longitude);
			else
				dataJSON.put("");

			if (enabledFields[Fields.MAG_X] && f.mag_x != null)
				dataJSON.put(f.mag_x);
			else
				dataJSON.put("");

			if (enabledFields[Fields.MAG_Y] && f.mag_y != null)
				dataJSON.put(f.mag_y);
			else
				dataJSON.put("");

			if (enabledFields[Fields.MAG_Z] && f.mag_z != null)
				dataJSON.put(f.mag_z);
			else
				dataJSON.put("");

			if (enabledFields[Fields.MAG_TOTAL] && f.mag_total != null)
				dataJSON.put(f.mag_total);
			else
				dataJSON.put("");

			if (enabledFields[Fields.HEADING_DEG] && f.angle_deg != null) {
				dataJSON.put(f.angle_deg);
			} else {
				dataJSON.put("");
			}

			if (enabledFields[Fields.HEADING_RAD] && f.angle_rad != null) {
				dataJSON.put(f.angle_rad);
			} else {
				dataJSON.put("");
			}
            if (enabledFields[Fields.TEMPERATURE_C] && f.temperature_c != null)
                dataJSON.put(f.temperature_c);
            else
				dataJSON.put("");

			if (enabledFields[Fields.PRESSURE] && f.pressure != null)
				dataJSON.put(f.pressure);
			else
				dataJSON.put("");

			if (enabledFields[Fields.ALTITUDE] && f.altitude != 0)
				dataJSON.put(f.altitude);
			else
				dataJSON.put("");

			if (enabledFields[Fields.LIGHT] && f.lux != null)
				dataJSON.put(f.lux);
			else
				dataJSON.put("");

			if (enabledFields[Fields.TEMPERATURE_F] && f.temperature_f != null)
				dataJSON.put(f.temperature_f);
			else
				dataJSON.put("");

			if (enabledFields[Fields.VELOCITY])
				dataJSON.put(f.velocity);
			else
				dataJSON.put("");

			if (enabledFields[Fields.DISTANCE])
				dataJSON.put(f.distance);
			else
				dataJSON.put("");

			if (enabledFields[Fields.HUMIDITY]) {
				dataJSON.put(f.humidity);
			} else {
				dataJSON.put("");
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		System.out.println("Data line: " + dataJSON.toString());

		return dataJSON;
	}

	/**
	 * Writes a single line of data in .csv format. Data is pulled from this
	 * class instance's Fields object.
	 *
	 * NOTE: Only call this method if you are recording with an associated
	 * project. You will be returned a blank string otherwise.
	 *
	 * Also ensure that your .csv file begins with the String returned by
	 * {@link edu.uml.cs.isense.dfm.DataFieldManager#writeHeaderLine()
	 * writeHeaderLine()}.
	 *
	 * @return A single line of data in .csv format in the form of a String, or
	 *         a blank string if there is no associated project.
	 */
	public String writeSdCardLine() {

		StringBuilder b = new StringBuilder();
		boolean start = true;
		boolean firstLineWritten = false;

		if (projID == -1)
			return "";

		for (String s : this.order) {

			if (s.equals(mContext.getString(R.string.accel_x))) {
				firstLineWritten = true;
				if (start)
					b.append(f.accel_x);
				else
					b.append(", ").append(f.accel_x);

			} else if (s.equals(mContext.getString(R.string.accel_y))) {
				firstLineWritten = true;
				if (start)
					b.append(f.accel_y);
				else
					b.append(", ").append(f.accel_y);

			} else if (s.equals(mContext.getString(R.string.accel_z))) {
				firstLineWritten = true;
				if (start)
					b.append(f.accel_z);
				else
					b.append(", ").append(f.accel_z);

			} else if (s.equals(mContext.getString(R.string.accel_total))) {
				firstLineWritten = true;
				if (start)
					b.append(f.accel_total);
				else
					b.append(", ").append(f.accel_total);

			} else if (s.equals(mContext.getString(R.string.temperature_c))) {
				firstLineWritten = true;
				if (start)
					b.append(f.temperature_c);
				else
					b.append(", ").append(f.temperature_c);

			} else if (s.equals(mContext.getString(R.string.temperature_f))) {
				firstLineWritten = true;
				if (start)
					b.append(f.temperature_f);
				else
					b.append(", ").append(f.temperature_f);

			} else if (s.equals(mContext.getString(R.string.time))) {
				firstLineWritten = true;
				if (start)
					b.append("u " + f.timeMillis);
				else
					b.append(", ").append("u " + f.timeMillis);

			} else if (s.equals(mContext.getString(R.string.luminous_flux))) {
				firstLineWritten = true;
				if (start)
					b.append(f.lux);
				else
					b.append(", ").append(f.lux);

			} else if (s.equals(mContext.getString(R.string.heading_deg))) {
				firstLineWritten = true;
				if (start)
					b.append(f.angle_deg);
				else
					b.append(", ").append(f.angle_deg);

			} else if (s.equals(mContext.getString(R.string.heading_rad))) {
				firstLineWritten = true;
				if (start)
					b.append(f.angle_rad);
				else
					b.append(", ").append(f.angle_rad);

			} else if (s.equals(mContext.getString(R.string.latitude))) {
				firstLineWritten = true;
				if (start)
					b.append(f.latitude);
				else
					b.append(", ").append(f.latitude);

			} else if (s.equals(mContext.getString(R.string.longitude))) {
				firstLineWritten = true;
				if (start)
					b.append(f.longitude);
				else
					b.append(", ").append(f.longitude);

			} else if (s.equals(mContext.getString(R.string.magnetic_x))) {
				firstLineWritten = true;
				if (start)
					b.append(f.mag_x);
				else
					b.append(", ").append(f.mag_x);

			} else if (s.equals(mContext.getString(R.string.magnetic_y))) {
				firstLineWritten = true;
				if (start)
					b.append(f.mag_y);
				else
					b.append(", ").append(f.mag_y);

			} else if (s.equals(mContext.getString(R.string.magnetic_z))) {
				firstLineWritten = true;
				if (start)
					b.append(f.mag_z);
				else
					b.append(", ").append(f.mag_z);

			} else if (s.equals(mContext.getString(R.string.magnetic_total))) {
				firstLineWritten = true;
				if (start)
					b.append(f.mag_total);
				else
					b.append(", ").append(f.mag_total);

			} else if (s.equals(mContext.getString(R.string.altitude))) {
				firstLineWritten = true;
				if (start)
					b.append(f.altitude);
				else
					b.append(", ").append(f.altitude);

			} else if (s.equals(mContext.getString(R.string.pressure))) {
				firstLineWritten = true;
				if (start)
					b.append(f.pressure);
				else
					b.append(", ").append(f.pressure);

			} else if (s.equals(mContext.getString(R.string.velocity))) {
				firstLineWritten = true;
				if (start)
					b.append(f.velocity);
				else
					b.append(", ").append(f.velocity);

			} else if (s.equals(mContext.getString(R.string.distance))) {
				firstLineWritten = true;
				if (start)
					b.append(f.distance);
				else
					b.append(", ").append(f.distance);

			} else if (s.equals(mContext.getString(R.string.humidity))) {
					firstLineWritten = true;
					if (start)
						b.append(f.humidity);
					else
						b.append(", ").append(f.humidity);

			} else {
				firstLineWritten = true;
				if (start)
					b.append("");
				else
					b.append(", ").append("");
			}

			if (firstLineWritten)
				start = false;

		}

		b.append("\n");

		return b.toString();
	}

	/**
	 * Writes the first line in a .csv file for the project you are recording
	 * data for. Data can then by appended to this by calling
	 * {@link edu.uml.cs.isense.dfm.DataFieldManager#writeSdCardLine()
	 * writeSdCardLine()}.
	 *
	 * NOTE: Only call this method if you are recording with an associated
	 * project. You will be returned a blank string otherwise.
	 *
	 * @return A single header line in .csv format in the form of a String, or a
	 *         blank string if there is no associated project.
	 */
	public String writeHeaderLine() {
		StringBuilder b = new StringBuilder();
		boolean start = true;

		if (projID == -1)
			return "";

		for (String unitName : this.realOrder) {

			if (start)
				b.append(unitName);
			else
				b.append(", ").append(unitName);

			start = false;
		}

		b.append("\n");

		return b.toString();
	}

	/**
	 * Use this method only if data was recorded with no associated project AND
	 * you intend to create a {@link edu.uml.cs.isense.queue.QDataSet QDataSet}
	 * object with an associated project before adding the QDataSet to an
	 * {@link edu.uml.cs.isense.queue.UploadQueue UploadQueue} object.
	 *
	 * This method will be called internally if you pass -1 as your project ID
	 * to the QDataSet object, and thus you do not need to call it.
	 *
	 *
	 * @param data
	 *            - A JSONArray of JSONArray objects returned from the
	 *            {@link edu.uml.cs.isense.dfm.DataFieldManager#putData()
	 *            putData()} method.
	 * @param projID
	 *            - The project ID which the data will be re-ordered to match.
	 * @param c
	 *            - The context of the Activity calling this function
	 * @param fieldOrder
	 *            - The list of fields matched using the FieldMatching class, or
	 *            null if FieldMatching wasn't used.
	 * @param fieldIDs
	 *            - The list of field IDs, in order, of the project to reorder
	 *            the data for (or null if you do not have them).
	 * @return A JSONObject.toString() formatted properly for upload to iSENSE.
	 *
	 */
	public static String reOrderData(JSONArray data, String projID, Context c,
			LinkedList<String> fieldOrder, LinkedList<Long> fieldIDs) {
		API api = API.getInstance();

		JSONArray row, outData = new JSONArray();
		JSONObject outRow;
		int len = data.length();

		// if the field order is null, set up the fieldOrder/fieldIDs.
		// otherwise, just get fieldIDs
		//TODO
		if (fieldOrder == null || fieldOrder.size() == 0) {
			DataFieldManager d = new DataFieldManager(Integer.parseInt(projID),
					api, c);
			d.getOrderWithExternalAsyncTask();
			fieldOrder = d.getOrderList();
			fieldIDs = d.getFieldIDs();
		} else if (fieldIDs == null || fieldIDs.size() == 0) {
			DataFieldManager d = new DataFieldManager(Integer.parseInt(projID),
					api, c);
			d.getOrderWithExternalAsyncTask();
			fieldIDs = d.getFieldIDs();
		}

		Activity a = (Activity) c;

		for (int i = 0; i < len; i++) {
			try {
				row = data.getJSONArray(i);
				outRow = new JSONObject();

				for (int j = 0; j < fieldOrder.size(); j++) {
					String s = fieldOrder.get(j);
					Long id = fieldIDs.get(j);

					try {
						// Compare against hard-coded strings
						if (s.equals(a.getResources().getString(
								R.string.accel_x))) {
							outRow.put(id + "", row.getString(Fields.ACCEL_X));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.accel_y))) {
							outRow.put(id + "", row.getString(Fields.ACCEL_Y));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.accel_z))) {
							outRow.put(id + "", row.getString(Fields.ACCEL_Z));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.accel_total))) {
							outRow.put(id + "",
									row.getString(Fields.ACCEL_TOTAL));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.temperature_c))) {
							outRow.put(id + "",
									row.getString(Fields.TEMPERATURE_C));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.temperature_f))) {
							outRow.put(id + "",
									row.getString(Fields.TEMPERATURE_F));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.time))) {
							outRow.put(id + "", row.getString(Fields.TIME));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.luminous_flux))) {
							outRow.put(id + "", row.getString(Fields.LIGHT));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.heading_deg))) {
							outRow.put(id + "",
									row.getString(Fields.HEADING_DEG));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.heading_rad))) {
							outRow.put(id + "",
									row.getString(Fields.HEADING_RAD));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.latitude))) {
							outRow.put(id + "", row.getDouble(Fields.LATITUDE));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.longitude))) {
							outRow.put(id + "", row.getDouble(Fields.LONGITUDE));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.magnetic_x))) {
							outRow.put(id + "", row.getString(Fields.MAG_X));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.magnetic_y))) {
							outRow.put(id + "", row.getString(Fields.MAG_Y));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.magnetic_z))) {
							outRow.put(id + "", row.getString(Fields.MAG_Z));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.magnetic_total))) {
							outRow.put(id + "", row.getString(Fields.MAG_TOTAL));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.altitude))) {
							outRow.put(id + "", row.getString(Fields.ALTITUDE));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.pressure))) {
							outRow.put(id + "", row.getString(Fields.PRESSURE));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.velocity))) {
							outRow.put(id + "", row.getString(Fields.VELOCITY));
							continue;
						} else if (s.equals(a.getResources().getString(
								R.string.distance))) {
							outRow.put(id + "", row.getString(Fields.DISTANCE));
							continue;
						} else if (s.equals(a.getResources().getString(R.string.humidity))) {
							outRow.put(id + "", row.getString(Fields.HUMIDITY));
							continue;
						} else {
							outRow.put(id + "", "");
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				outData.put(outRow);

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return outData.toString();
	}

	/**
	 * Set the context for this instance of DataFieldManager.
	 *
	 * @param c
	 *            - The new context of this DataFieldManager instance.
	 */
	public void setContext(Context c) {
		this.mContext = c;
	}

	// Task for checking sensor availability along with enabling/disabling
	private class GetOrderTask extends AsyncTask<Void, Integer, Void> {

		@Override
		protected Void doInBackground(Void... voids) {
			projFields = api.getProjectFields(projID);
			return null;
		}

		@Override
		protected void onPostExecute(Void voids) {
			getProjectFieldOrder();
		}
	}

	/**
	 * Writes the fields for the project out to SharedPreferences, designed to
	 * aid the writing of .csv files using
	 * {@link edu.uml.cs.isense.dfm.DataFieldManager#getProjectFieldsAndSetCSVOrder()
	 * getProjectFieldsAndSetCSVOrder} when prepared to write a .csv file.
	 *
	 */
	public void writeProjectFields() {
		SharedPreferences mPrefs = this.mContext.getSharedPreferences(
				"CSV_ORDER", 0);
		SharedPreferences.Editor mEdit = mPrefs.edit();

		StringBuilder sb = new StringBuilder();
		boolean start = true;

		for (String s : this.realOrder) {
			if (start)
				sb.append(s);
			else
				sb.append(CSV_DELIMITER).append(s);

			start = false;
		}

		String out = sb.toString();
		mEdit.putString("csv_order", out).commit();

	}

	/**
	 * Retrieve the fields written to SharedPreferences from
	 * {@link edu.uml.cs.isense.dfm.DataFieldManager#writeProjectFields()
	 * writeProjectFields()}, primarily designed for writing the header line in
	 * a .csv file.
	 *
	 */
	public void getProjectFieldsAndSetCSVOrder() {
		//TODO ARE PREFS NEEDED HERE?
		SharedPreferences mPrefs = this.mContext.getSharedPreferences(
				"CSV_ORDER", 0);

		String in = mPrefs.getString("csv_order", "");
		if (in.equals(""))
			return;

		String[] parts = in.split(CSV_DELIMITER);
		this.realOrder.clear();

		for (String s : parts) {
			this.realOrder.add(s);
		}

	}

	/**
	 *
	 */
	private void getProjectFieldOrder() {
		this.order = new LinkedList<String>();
		this.realOrder = new LinkedList<String>();
		this.fieldIDs = new LinkedList<Long>();

		for (RProjectField field : projFields) {

			realOrder.add(field.name);
			fieldIDs.add(field.field_id);

			switch (field.type) {

			// Number
			case RProjectField.TYPE_NUMBER:

				// Temperature
				if (field.name.toLowerCase(Locale.US).contains("temp")) {
					if (field.unit.toLowerCase(Locale.US).contains("c")) {
						order.add(mContext.getString(R.string.temperature_c));
					} else {
						order.add(mContext.getString(R.string.temperature_f));
					}
					break;
				}

				// Potential Altitude
				else if (field.name.toLowerCase(Locale.US).contains("alt")) {
					order.add(mContext.getString(R.string.altitude));
					break;
				}

				// Light
				else if (field.name.toLowerCase(Locale.US).contains("light")) {
					order.add(mContext.getString(R.string.luminous_flux));
					break;
				}

				// Heading
				else if (field.name.toLowerCase(Locale.US).contains("heading")
						|| field.name.toLowerCase(Locale.US).contains("angle")) {
					if (field.unit.toLowerCase(Locale.US).contains("rad")) {
						order.add(mContext.getString(R.string.heading_rad));
					} else {
						order.add(mContext.getString(R.string.heading_deg));
					}
					break;
				}

				// Numeric/Custom
				else if (field.name.toLowerCase(Locale.US).contains("magnetic")) {
					if (field.name.toLowerCase(Locale.US).contains("x")) {
						order.add(mContext.getString(R.string.magnetic_x));
					} else if (field.name.toLowerCase(Locale.US).contains("y")) {
						order.add(mContext.getString(R.string.magnetic_y));
					} else if (field.name.toLowerCase(Locale.US).contains("z")) {
						order.add(mContext.getString(R.string.magnetic_z));
					} else {
						order.add(mContext.getString(R.string.magnetic_total));
					}
					break;
				}

				// Acceleration
				else if (field.name
						.toLowerCase(Locale.US)
						.matches(
								"(^(((x|y|z){1}[^x|y|z]*acc[^x|y|z]*)|[^x|y|z]*(accel))$)"
										+ "|(^((acc[^x|y|z]*(x|y|z){1})|(accel[^x|y|z]*))$)")) {
					if (field.name.toLowerCase(Locale.US).contains("x")) {
						order.add(mContext.getString(R.string.accel_x));
					} else if (field.name.toLowerCase(Locale.US).contains("y")) {
						order.add(mContext.getString(R.string.accel_y));
					} else if (field.name.toLowerCase(Locale.US).contains("z")) {
						order.add(mContext.getString(R.string.accel_z));
					} else {
						order.add(mContext.getString(R.string.accel_total));
					}
					break;
				}

				// Pressure
				else if (field.name.toLowerCase(Locale.US).contains("pres")) {
					order.add(mContext.getString(R.string.pressure));
					break;
				}

				// velocity
				else if (field.name.toLowerCase(Locale.US).contains("vel")) {
					order.add(mContext.getString(R.string.velocity));
					break;
				}

				// distance
				else if (field.name.toLowerCase(Locale.US).contains("dist")) {
					order.add(mContext.getString(R.string.distance));
					break;
				}

				// humidity
				else if (field.name.toLowerCase(Locale.US).contains("humidity")) {
					order.add(mContext.getString(R.string.humidity));
					break;
				}


				else {
					order.add(mContext.getString(R.string.null_string)
							+ field.name);
					break;
				}

				// Time
			case RProjectField.TYPE_TIMESTAMP:
				order.add(mContext.getString(R.string.time));
				break;

			// Latitude
			case RProjectField.TYPE_LAT:
				order.add(mContext.getString(R.string.latitude));
				break;

			// Longitude
			case RProjectField.TYPE_LON:
				order.add(mContext.getString(R.string.longitude));
				break;

			// No match (Just about every other category)
			default:
				order.add(mContext.getString(R.string.null_string) + field.name);
				break;

			}
		}
	}

	/**
	 * Getter for the project ID this DataFieldManager instance operates on.
	 *
	 * @return The current project ID.
	 */
	public int getProjID() {
		return this.projID;
	}

	/**
	 * Setter for the project ID this DataFieldManager instance operates on.
	 *
	 * NOTE: This will also call
	 * {@link edu.uml.cs.isense.dfm.DataFieldManager#getOrder()} to update which
	 * fields this DataFieldManager instance records for.
	 *
	 * @param projectID
	 *            - The new project ID for DataFieldManager to operate with.
	 */
	public void setProjID(int projectID) {
		this.projID = projectID;
		getOrder();
	}

	/**
	 * Getter for the project fields associated with the project ID passed in to
	 * this instance of DataFieldManager.
	 *
	 * @return An ArrayList of {@link edu.uml.cs.isense.objects.RProjectField
	 *         RProjectField} containing the fields from the associated iSENSE
	 *         project.
	 */
	public ArrayList<RProjectField> getProjectFields() {
		return this.projFields;
	}

	/**
	 * Getter for the list of matched fields that this instance of
	 * DataFieldManager will record data for.
	 *
	 * @return The list of matched project fields from the associated project ID
	 */
	public LinkedList<String> getOrderList() {
		return this.order;
	}

	/**
	 * Getter for the list of actual project fields.
	 *
	 * @return The list of project fields from the associated project ID
	 */
	public LinkedList<String> getRealOrderList() {
		return this.realOrder;
	}

	/**
	 * Converts order into a String[]
	 *
	 * @return order in the form of a String[]
	 */
	public String[] convertLinkedListToStringArray(LinkedList<String> ll) {

		String[] sa = new String[ll.size()];
		int i = 0;

		for (String s : ll)
			sa[i++] = s;

		return sa;
	}

	/**
	 * Converts a String[] back into a LinkedList of Strings
	 *
	 * @param sa
	 *            - the String[] to convert
	 * @return sa in the form of a LinkedList of Strings
	 */
	public static LinkedList<String> convertStringArrayToLinkedList(String[] sa) {

		LinkedList<String> lls = new LinkedList<String>();

		for (String s : sa)
			lls.add(s);

		return lls;
	}

	/**
	 * Getter for the Fields object associated with this instance of
	 * DataFieldManager
	 *
	 * @return The Fields object associated with this instance of
	 *         DataFieldManager
	 */
	public Fields getFields() {
		return this.f;
	}

	/**
	 * Setter for the Fields object associated with this instance of
	 * DataFieldManager
	 *
	 * @param fields
	 *            - The Fields object associated with this instance of
	 *            DataFieldManager
	 */
	public void setFields(Fields fields) {
		this.f = fields;
	}

	/**
	 * Getter for the list of field IDs in the order list.
	 *
	 * @return The IDs for the fields, in order, of the project associated with
	 *         this DataFieldManager instance.
	 */
	public LinkedList<Long> getFieldIDs() {
		return this.fieldIDs;
	}

	private ArrayList<Integer> getFieldTypes() {

		ArrayList<Integer> fieldTypes = new ArrayList<Integer>();

		for (RProjectField field : this.projFields) {
			fieldTypes.add(field.type);
		}

		return fieldTypes;

	}

	/**
	 * Checks if project contains timestamp
	 * @return True if project has a timestamp. False if it does not.
	 */

	public boolean projectContainsTimeStamp() {
		ArrayList<Integer> fields = this.getFieldTypes();

		for (Integer i : fields) {
			if (i.intValue() == RProjectField.TYPE_TIMESTAMP) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks to see if project uses location
	 * @return True if project contains lat or long fields. False if it does not.
	 */
	public boolean projectContainsLocation() {
		ArrayList<Integer> fields = this.getFieldTypes();

		for (Integer i : fields) {
			if (i.intValue() == RProjectField.TYPE_LAT
					|| i.intValue() == RProjectField.TYPE_LON) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Enables all fields for recording data
	 */
	public void enableAllSensorFields() {
		enabledFields[Fields.TIME] = true;
		enabledFields[Fields.ACCEL_X] = true;
		enabledFields[Fields.ACCEL_Y] = true;
		enabledFields[Fields.ACCEL_Z] = true;
		enabledFields[Fields.ACCEL_TOTAL] = true;
		enabledFields[Fields.LATITUDE] = true;
		enabledFields[Fields.LONGITUDE] = true;
		enabledFields[Fields.MAG_X] = true;
		enabledFields[Fields.MAG_Y] = true;
		enabledFields[Fields.MAG_Z] = true;
		enabledFields[Fields.MAG_TOTAL] = true;
		enabledFields[Fields.HEADING_DEG] = true;
		enabledFields[Fields.HEADING_RAD] = true;
		enabledFields[Fields.TEMPERATURE_C] = true;
		enabledFields[Fields.TEMPERATURE_F] = true;
		enabledFields[Fields.PRESSURE] = true;
		enabledFields[Fields.ALTITUDE] = true;
		enabledFields[Fields.LIGHT] = true;
		enabledFields[Fields.VELOCITY]= true;
		enabledFields[Fields.DISTANCE] = true;
		enabledFields[Fields.HUMIDITY] = true;
	}

	/**
	 * Set the enabled fields from the acceptedFields parameter.
	 *
	 * @param acceptedFields
	 *            LinkedList of field strings
	 */
	public void setEnabledFields(LinkedList<String> acceptedFields) {
		//Can't use switch on strings below java 1.7, Android uses 1.6 :(
		for (String s : acceptedFields) {
			if (s.equals(mContext.getString(R.string.time)))
				enabledFields[Fields.TIME] = true;
			if (s.equals(mContext.getString(R.string.accel_x)))
				enabledFields[Fields.ACCEL_X] = true;
			if (s.equals(mContext.getString(R.string.accel_y)))
				enabledFields[Fields.ACCEL_Y] = true;
			if (s.equals(mContext.getString(R.string.accel_z)))
				enabledFields[Fields.ACCEL_Z] = true;
			if (s.equals(mContext.getString(R.string.accel_total)))
				enabledFields[Fields.ACCEL_TOTAL] = true;
			if (s.equals(mContext.getString(R.string.latitude)))
				enabledFields[Fields.LATITUDE] = true;
			if (s.equals(mContext.getString(R.string.longitude)))
				enabledFields[Fields.LONGITUDE] = true;
			if (s.equals(mContext.getString(R.string.magnetic_x)))
				enabledFields[Fields.MAG_X] = true;
			if (s.equals(mContext.getString(R.string.magnetic_y)))
				enabledFields[Fields.MAG_Y] = true;
			if (s.equals(mContext.getString(R.string.magnetic_z)))
				enabledFields[Fields.MAG_Z] = true;
			if (s.equals(mContext.getString(R.string.magnetic_total)))
				enabledFields[Fields.MAG_TOTAL] = true;
			if (s.equals(mContext.getString(R.string.heading_deg)))
				enabledFields[Fields.HEADING_DEG] = true;
			if (s.equals(mContext.getString(R.string.heading_rad)))
				enabledFields[Fields.HEADING_RAD] = true;
			if (s.equals(mContext.getString(R.string.temperature_c)))
				enabledFields[Fields.TEMPERATURE_C] = true;
			if (s.equals(mContext.getString(R.string.temperature_f)))
				enabledFields[Fields.TEMPERATURE_F] = true;
			if (s.equals(mContext.getString(R.string.pressure)))
				enabledFields[Fields.PRESSURE] = true;
			if (s.equals(mContext.getString(R.string.altitude)))
				enabledFields[Fields.ALTITUDE] = true;
			if (s.equals(mContext.getString(R.string.luminous_flux)))
				enabledFields[Fields.LIGHT] = true;
			if (s.equals(mContext.getString(R.string.velocity)))
				enabledFields[Fields.VELOCITY] = true;
			if (s.equals(mContext.getString(R.string.distance)))
				enabledFields[Fields.DISTANCE] = true;
			if (s.equals(mContext.getString(R.string.humidity)))
				enabledFields[Fields.HUMIDITY] = true;

		}
	}

	/**
	 * Converts a JSONArray of JSONArray data into a JSONArray of JSONObject
	 * data where each JSONObject is the internal JSONArray element of the old
	 * data, keyed with the project's field IDs.
	 *
	 * @param oldData
	 *            - JSONArray of JSONArray data to be converted
	 * @return A JSONArray of JSONObjects, ready for upload to the associated
	 *         project.
	 */
	public JSONArray convertInternalDataToJSONObject(JSONArray oldData) {

		getOrderWithExternalAsyncTask();
		JSONArray newData = new JSONArray();

		for (int i = 0; i < oldData.length(); i++) {
			try {
				JSONArray oldRow = oldData.getJSONArray(i);
				JSONObject newRow = new JSONObject();
				for (int j = 0; j < this.fieldIDs.size(); j++) {
					String data = oldRow.getString(j);
					newRow.put(fieldIDs.get(j) + "", data);
				}
				newData.put(newRow);
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}

		return newData;
	}

	/**
	 * Called from apps on Location change method to update current location
	 * @param location
	 */
	public void updateLoc(Location location) {
        if (location != null)
			loc = location;
	}



	/**
	 * Called from onSensorChangedEvent to set the current values.
	 * @param event
	 */
	public void updateValues(SensorEvent event) {
		DecimalFormat toThou = new DecimalFormat("######0.000");

		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION ||
				event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {

			if (enabledFields[Fields.ACCEL_X]
					|| enabledFields[Fields.ACCEL_Y]
					|| enabledFields[Fields.ACCEL_Z]
					|| enabledFields[Fields.ACCEL_TOTAL]) {

				rawAccel = event.values.clone();
				accel[0] = event.values[0];
				accel[1] = event.values[1];
				accel[2] = event.values[2];
				accel[3] = (float) Math.sqrt((float) ((Math.pow(accel[0], 2)
						+ Math.pow(accel[1], 2) + Math.pow(accel[2], 2))));

			}

		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if (enabledFields[Fields.MAG_X]
					|| enabledFields[Fields.MAG_Y]
					|| enabledFields[Fields.MAG_Z]
					|| enabledFields[Fields.MAG_TOTAL]
					|| enabledFields[Fields.HEADING_DEG]
					|| enabledFields[Fields.HEADING_RAD]) {

				rawMag = event.values.clone();

				mag[0] = event.values[0];
				mag[1] = event.values[1];
				mag[2] = event.values[2];

				float rotation[] = new float[9];

				if (SensorManager.getRotationMatrix(rotation, null, rawAccel,
						rawMag)) {
					orientation = new float[3];
					SensorManager.getOrientation(rotation, orientation);
				}
			}

		} else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
			if (enabledFields[Fields.TEMPERATURE_C]
					|| enabledFields[Fields.TEMPERATURE_F])
				temperature = toThou.format(event.values[0]);
        } else if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			if (enabledFields[Fields.PRESSURE])
				pressure = toThou.format(event.values[0]);
		} else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
			if (enabledFields[Fields.LIGHT])
				light = toThou.format(event.values[0]);
		} else if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
			if (enabledFields[Fields.HUMIDITY]) {
				humidity = event.values[0];
			}
		}
	}



	/**
	 * Starts recording data into a JSONArray.
	 * To stop recording, the method stopRecording() should be called.
	 * @param srate
	 * @return void
	 */
	public void recordData(long srate) {
		//Clears Data from last recording
		dataSet = new JSONArray();
		dataSetRate = srate;
		recordingTimer = new Timer();
		recordingTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				dataSet.put(recordDataPoint());
			}
		}, srate, srate);
	}


	/**
	 * Stops recording data and returns the data in the form of a JSONArray.
	 * The JSONArray can be added to the uploadQueue or be uploaded directly to iSENSE.
	 * @return JSONArray
	 */
	public JSONArray stopRecording() {
		recordingTimer.cancel();
        unRegisterSensors();

		return dataSet;
	}


	/**
	 * This adds the current data from the sensors to our dataSet.
	 * @return returns a JSONArray of one single data point
	 */
	public JSONArray recordDataPoint() {
		DecimalFormat toThou = new DecimalFormat("######0.000");

        if (enabledFields[Fields.ACCEL_X])
                f.accel_x = toThou.format(accel[0]);
        if (enabledFields[Fields.ACCEL_Y])
                f.accel_y = toThou.format(accel[1]);
        if (enabledFields[Fields.ACCEL_Z])
                f.accel_z = toThou.format(accel[2]);
        if (enabledFields[Fields.ACCEL_TOTAL])
                f.accel_total = toThou.format(accel[3]);
        if (enabledFields[Fields.LATITUDE])
        	if(loc != null)
                f.latitude = loc.getLatitude();
        if (enabledFields[Fields.LONGITUDE])
        	if(loc != null)
                f.longitude = loc.getLongitude();
        if (enabledFields[Fields.HEADING_DEG])
				 f.angle_deg = toThou.format(orientation[0] * (180 / Math.PI ));
        if (enabledFields[Fields.HEADING_RAD])
       	 if (!f.angle_deg.equals("")) {
				f.angle_rad = toThou.format(orientation[0]);
       	 } else {
				f.angle_rad = "";
       	 }
        if (enabledFields[Fields.MAG_X])
                f.mag_x = "" + mag[0];
        if (enabledFields[Fields.MAG_Y])
                f.mag_y = "" + mag[1];
        if (enabledFields[Fields.MAG_Z])
                f.mag_z = "" + mag[2];
        if (enabledFields[Fields.MAG_TOTAL])
                f.mag_total = "" + Math.sqrt(Math.pow(Double.parseDouble(f.mag_x), 2)
		                                 + Math.pow(Double.parseDouble(f.mag_y), 2)
		                                 + Math.pow(Double.parseDouble(f.mag_z), 2));
        if (enabledFields[Fields.TIME])
                f.timeMillis = System.currentTimeMillis();
        if (enabledFields[Fields.TEMPERATURE_C]) {
            if (temperature.equals("")) {
                f.temperature_c = "";
            } else {
                f.temperature_c = "" + Double.parseDouble(temperature);
            }
        }
        if (enabledFields[Fields.TEMPERATURE_F]) {
            if (temperature.equals("")) {
                f.temperature_f = "";
            } else {
                f.temperature_f = "" + toThou.format((Double.parseDouble(temperature) * 1.8) + 32);
            }
        }
        if (enabledFields[Fields.PRESSURE])
                f.pressure = pressure;
        if (enabledFields[Fields.ALTITUDE])
        	if(loc != null)
                f.altitude = loc.getAltitude();
        if (enabledFields[Fields.LIGHT])
                f.lux = light;
        if (enabledFields[Fields.DISTANCE] || enabledFields[Fields.VELOCITY]) {
        	//calculations required for distance and velocity
        	if (prevLoc != null && loc != null) {
        		distance = loc.distanceTo((prevLoc));
        		totalDistance += distance;
        	} else {
        		f.distance = 0;
        	}
        	prevLoc = loc;

	        if (enabledFields[Fields.DISTANCE]) {
	        	f.distance = totalDistance;
	        }

	        if (enabledFields[Fields.VELOCITY]) {
	        	velocity = distance/dataSetRate;
	        	f.velocity = velocity;
	        }
        }

        if (enabledFields[Fields.HUMIDITY]) {
        	f.humidity = humidity;
        }

        return putData();
	}



	/**
	 * This method takes an imageUri and makes a data point from its time stamp and
	 * geo tag
	 * @param imageUri
	 * @return JSONArray (DataPoint)
	 */
	@SuppressLint("SimpleDateFormat")
	public JSONArray getDataFromPic(Uri imageUri) {
		double lat = 0;
		double lon = 0;
		Long timePicTaken = null;
		String picturePath;

	    Cursor cursor = mContext.getContentResolver().query(imageUri, null, null, null, null);
		if (cursor == null) { // Source is a cloud service (Dropbox, GoogleDrive)
			picturePath = imageUri.getPath();
	    } else {
	        cursor.moveToFirst();
	        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
	        picturePath = cursor.getString(idx);
	        cursor.close();
	    }

		/*get data from picture file*/
		ExifInterface exifInterface;
		try {
			exifInterface = new ExifInterface(picturePath);

			/* get timestamp from picture */
			String dateTime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);

			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
				Date parsedDate = dateFormat.parse(dateTime);
				timePicTaken = parsedDate.getTime();
			} catch (Exception e) {
				e.printStackTrace();
			}

			/*get location data from image*/
			String latString = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
			String lonString = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
			String latRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
			String lonRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

			if (latString != null && lonString != null) {

				lat = convertToDegree(latString);
				lon = convertToDegree(lonString);

				if(latRef.equals("N")){
					lat = convertToDegree(latString);
				} else {
					lat = 0 - convertToDegree(latString);
				}

				if(lonRef.equals("E")){
					lon = convertToDegree(lonString);
				} else {
				   lon = 0 - convertToDegree(lonString);
				}

			} else {
				lat = 0;
				lon = 0;
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}

		/* Record the data */
        if (enabledFields[Fields.LATITUDE])
        	if(loc != null)
                f.latitude = lat;
        if (enabledFields[Fields.LONGITUDE])
        	if(loc != null)
                f.longitude = lon;
        if (enabledFields[Fields.TIME])
			if (timePicTaken != null)
                f.timeMillis = timePicTaken;

		return putData();
	}

	@SuppressLint("UseValueOf")
	private Float convertToDegree(String stringDMS){
		Float result = null;
		String[] DMS = stringDMS.split(",", 3);

		String[] stringD = DMS[0].split("/", 2);
	    Double D0 = new Double(stringD[0]);
	    Double D1 = new Double(stringD[1]);
	    Double FloatD = D0/D1;

		 String[] stringM = DMS[1].split("/", 2);
		 Double M0 = new Double(stringM[0]);
		 Double M1 = new Double(stringM[1]);
		 Double FloatM = M0/M1;

		 String[] stringS = DMS[2].split("/", 2);
		 Double S0 = new Double(stringS[0]);
		 Double S1 = new Double(stringS[1]);
		 Double FloatS = S0/S1;

		 result = new Float(FloatD + (FloatM/60) + (FloatS/3600));

		 return result;
	};



	public void setUpDFMWithAllSensorFields(Context appContext) {
		getOrder();

		enableAllSensorFields();

		String acceptedFields = appContext.getResources().getString(R.string.time) + ","
				+ appContext.getResources().getString(R.string.accel_x) + ","
				+ appContext.getResources().getString(R.string.accel_y) + ","
				+ appContext.getResources().getString(R.string.accel_z) + ","
				+ appContext.getResources().getString(R.string.accel_total) + ","
				+ appContext.getResources().getString(R.string.latitude) + ","
				+ appContext.getResources().getString(R.string.longitude) + ","
				+ appContext.getResources().getString(R.string.magnetic_x) + ","
				+ appContext.getResources().getString(R.string.magnetic_y) + ","
				+ appContext.getResources().getString(R.string.magnetic_z) + ","
				+ appContext.getResources().getString(R.string.magnetic_total) + ","
				+ appContext.getResources().getString(R.string.heading_deg) + ","
				+ appContext.getResources().getString(R.string.heading_rad) + ","
				+ appContext.getResources().getString(R.string.temperature_c) + ","
				+ appContext.getResources().getString(R.string.pressure) + ","
				+ appContext.getResources().getString(R.string.altitude) + ","
				+ appContext.getResources().getString(R.string.luminous_flux) + ","
				+ appContext.getResources().getString(R.string.temperature_f) + ","
				+ appContext.getResources().getString(R.string.velocity) + ","
				+ appContext.getResources().getString(R.string.distance) + ","
				+ appContext.getResources().getString(R.string.humidity);

	}

		/**
		 * Called from an app to enable sensors based upon what the fields are of the project.
		 */
		public void registerSensors() {
			//just to be sure
			unRegisterSensors();

            //Initialize sensor managers
			mListener = new MyListener();

		    mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

			if (enabledFields[Fields.ACCEL_X]
					|| enabledFields[Fields.ACCEL_Y]
					|| enabledFields[Fields.ACCEL_Z]
					|| enabledFields[Fields.ACCEL_TOTAL]) {
				mSensorManager.registerListener(mListener,
						mSensorManager
								.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

			if (enabledFields[Fields.MAG_X]
					|| enabledFields[Fields.MAG_Y]
					|| enabledFields[Fields.MAG_Z]
					|| enabledFields[Fields.MAG_TOTAL]
					|| enabledFields[Fields.HEADING_DEG]
					|| enabledFields[Fields.HEADING_RAD]) {
				mSensorManager.registerListener(mListener,
						mSensorManager
								.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

			if (enabledFields[Fields.TEMPERATURE_C]
					|| enabledFields[Fields.TEMPERATURE_F]) {
				if (getApiLevel() >= 14) {
					mSensorManager.registerListener(
							mListener,
							        mSensorManager
											.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE),
									SensorManager.SENSOR_DELAY_FASTEST);
				}
			}

			if (enabledFields[Fields.HUMIDITY]) {
				mSensorManager.registerListener(
						mListener,
						        mSensorManager
										.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY),
								SensorManager.SENSOR_DELAY_FASTEST);
			}

			if (enabledFields[Fields.PRESSURE]
					|| enabledFields[Fields.ALTITUDE]) {
				mSensorManager.registerListener(mListener,
						mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

			if (enabledFields[Fields.LIGHT]) {
				mSensorManager.registerListener(mListener,
						mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

            //if locationmanager is null and field is Lat, Long, or Altitude
			if ((mLocationManager == null) && (enabledFields[Fields.LATITUDE] || enabledFields[Fields.LONGITUDE] || enabledFields[Fields.ALTITUDE])) {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);

                mListener = new MyListener();

                mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

				mLocationManager.requestLocationUpdates(
			                mLocationManager.getBestProvider(criteria, true), 0, 0,
			                mListener);
		    }
			if (enabledFields[Fields.HUMIDITY]) {
				mSensorManager.registerListener(mListener,
						mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY),
						SensorManager.SENSOR_DELAY_FASTEST);
			}
		}

		/**
		 * Disables all active sensors and turns off gps
		 */
		public void unRegisterSensors() {
			if (mLocationManager != null)
	            mLocationManager.removeUpdates(mListener);

	        if (mSensorManager != null)
	            mSensorManager.unregisterListener(mListener);
        }

		// Assists with differentiating between displays for dialogues
		private int getApiLevel() {
			return android.os.Build.VERSION.SDK_INT;
		}



    /**
     * You can not implement a SensorEventListener or a LocationListener in a service so that is why
     * There is a separate class here that implements a SensorEventListener
     */
    public class MyListener implements SensorEventListener, LocationListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            updateValues(event);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
        @Override
        public void onLocationChanged(final Location location) {
            if (location != null)
                updateLoc(location);
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

    }


}




