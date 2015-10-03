package edu.uml.cs.isense.proj;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import edu.uml.cs.isense.R;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.objects.RProjectField;
import edu.uml.cs.isense.supplements.OrientationManager;

/**
 * Dialog that displays allowing the implementor to name a custom project
 * created with the fields designated by the "Constrict Fields" extra
 * passed to the {@link edu.uml.cs.isense.proj.ProjectManager Setup} class.
 *
 * @author iSENSE Android Development Team
 *
 */
public class ProjectFieldDialog extends ActionBarActivity {

	private EditText nameInput;
	private Button ok,cancel, selectAll;
	private boolean allchecked = false;


	private CheckBox timestamp;
	private CheckBox acceleration;
	private CheckBox magnetic;
	private CheckBox location;
	private CheckBox speed;
	private CheckBox distance;
	private CheckBox altitude;
	private CheckBox pressure;
	private CheckBox humidity;
	private CheckBox heading;
	private CheckBox temp;
	private CheckBox light;
	private ProgressDialog dia;

	private Context mContext;
	private API api;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.project_fields_checklist);

		mContext = this;

		api = API.getInstance();

		nameInput = (EditText) findViewById(R.id.new_proj_name);
		ok = (Button) findViewById(R.id.okB);
		cancel = (Button) findViewById(R.id.clB);
		selectAll = (Button) findViewById(R.id.select_deselect_all);

		timestamp = (CheckBox) findViewById(R.id.cbTimestamp);
		acceleration = (CheckBox) findViewById(R.id.cbAcceleration);
		magnetic = (CheckBox) findViewById(R.id.cbMagnetic);
		location = (CheckBox) findViewById(R.id.cbLocation);
		speed = (CheckBox) findViewById(R.id.cbSpeed);
		distance = (CheckBox) findViewById(R.id.cbDistance);
		altitude = (CheckBox) findViewById(R.id.cbAltitude);
		pressure = (CheckBox) findViewById(R.id.cbPressure);
		humidity = (CheckBox) findViewById(R.id.cbHumidity);
		heading = (CheckBox) findViewById(R.id.cbHeading);
		temp = (CheckBox) findViewById(R.id.cbTemp);
		light = (CheckBox) findViewById(R.id.cbLuminuous);


		ok.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String name = nameInput.getText().toString();
				if (name.equals("")){
					nameInput.setError("Project Name Cannot Be Empty");
				} else {
					ArrayList<RProjectField> fields = new ArrayList<RProjectField>();

					if (timestamp.isChecked()) {
						RProjectField time = new RProjectField();
						time.name = "Timestamp";
						time.type = RProjectField.TYPE_TIMESTAMP;
						fields.add(time);
					}

					if (acceleration.isChecked()) {
						RProjectField accelx = new RProjectField();
						accelx.name = "Accel-x";
						accelx.type = RProjectField.TYPE_NUMBER;
						accelx.unit = "m/s^2";
						fields.add(accelx);

						RProjectField accely = new RProjectField();
						accely.name = "Accel-y";
						accely.type = RProjectField.TYPE_NUMBER;
						accely.unit = "m/s^2";
						fields.add(accely);

						RProjectField accelz = new RProjectField();
						accelz.name = "Accel-z";
						accelz.type = RProjectField.TYPE_NUMBER;
						accelz.unit = "m/s^2";
						fields.add(accelz);

						RProjectField accelmag = new RProjectField();
						accelmag.name = "Accel-magnitude";
						accelmag.type = RProjectField.TYPE_NUMBER;
						accelmag.unit = "m/s^2";
						fields.add(accelmag);
					}

					if (magnetic.isChecked()) {
						RProjectField magx = new RProjectField();
						magx.name = "Magnetic-x";
						magx.type = RProjectField.TYPE_NUMBER;
						magx.unit = "μT";
						fields.add(magx);

						RProjectField magy = new RProjectField();
						magy.name = "Magnetic-y";
						magy.type = RProjectField.TYPE_NUMBER;
						magy.unit = "μT";
						fields.add(magy);

						RProjectField magz = new RProjectField();
						magz.name = "Magnetic-z";
						magz.type = RProjectField.TYPE_NUMBER;
						magz.unit = "μT";
						fields.add(magz);

						RProjectField magmag = new RProjectField();
						magmag.name = "Magnetic-magnitude";
						magmag.type = RProjectField.TYPE_NUMBER;
						magmag.unit = "μT";
						fields.add(magmag);
					}

					if (location.isChecked()) {
						RProjectField lat = new RProjectField();
						lat.name = "Latitude";
						lat.type = RProjectField.TYPE_LAT;
						lat.unit = "Degrees";
						fields.add(lat);

						RProjectField lon = new RProjectField();
						lon.name = "Longitude";
						lon.type = RProjectField.TYPE_LON;
						lon.unit = "Degrees";
						fields.add(lon);
					}

					if (speed.isChecked()) {
						RProjectField speed = new RProjectField();
						speed.name = "Speed";
						speed.type = RProjectField.TYPE_NUMBER;
						speed.unit = "m/s";
						fields.add(speed);
					}

					if (distance.isChecked()) {
						RProjectField distance = new RProjectField();
						distance.name = "Distance";
						distance.type = RProjectField.TYPE_NUMBER;
						distance.unit = "meters";
						fields.add(distance);
					}

					if (altitude.isChecked()) {
						RProjectField alt = new RProjectField();
						alt.name = "Altitude";
						alt.type = RProjectField.TYPE_NUMBER;
						alt.unit = "meters above sea level";
						fields.add(alt);
					}

					if (pressure.isChecked()) {
						RProjectField pres = new RProjectField();
						pres.name = "Pressure";
						pres.type = RProjectField.TYPE_NUMBER;
						pres.unit = "hPa or mbar";
						fields.add(pres);
					}

					if (humidity.isChecked()) {
						RProjectField hum = new RProjectField();
						hum.name = "Humidity";
						hum.type = RProjectField.TYPE_NUMBER;
						hum.unit = "%";
						fields.add(hum);
					}

					if (heading.isChecked()) {
						RProjectField headingdeg = new RProjectField();
						headingdeg.name = "Heading Degrees";
						headingdeg.type = RProjectField.TYPE_NUMBER;
						headingdeg.unit = "Deg";
						fields.add(headingdeg);

						RProjectField headingrad = new RProjectField();
						headingrad.name = "Heading Radians";
						headingrad.type = RProjectField.TYPE_NUMBER;
						headingrad.unit = "Rad";
						fields.add(headingrad);
					}

					if (temp.isChecked()) {
						RProjectField tempf = new RProjectField();
						tempf.name = "Temperature F";
						tempf.type = RProjectField.TYPE_NUMBER;
						tempf.unit = "Fahrenheit";
						fields.add(tempf);

						RProjectField tempc = new RProjectField();
						tempc.name = "Temperature C";
						tempc.type = RProjectField.TYPE_NUMBER;
						tempc.unit = "Celsius";
						fields.add(tempc);
					}

					if (light.isChecked()) {
						RProjectField l = new RProjectField();
						l.name = "Light";
						l.type = RProjectField.TYPE_NUMBER;
						l.unit = "lx";
						fields.add(l);
					}

					nameInput.setError(null);

					new CreateProjectTask().execute(name, fields);
				}

			}
		});

		cancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		selectAll.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (allchecked) {
					selectAll.setText("Select All");
					allchecked = false;
					timestamp.setChecked(false);
					acceleration.setChecked(false);
					magnetic.setChecked(false);
					location.setChecked(false);
					speed.setChecked(false);
					distance.setChecked(false);
					altitude.setChecked(false);
					pressure.setChecked(false);
					humidity.setChecked(false);
					heading .setChecked(false);
					temp.setChecked(false);
					light.setChecked(false);
				} else {
					selectAll.setText("Deselect All");
					allchecked = true;
					timestamp.setChecked(true);
					acceleration.setChecked(true);
					magnetic.setChecked(true);
					location.setChecked(true);
					speed.setChecked(true);
					distance.setChecked(true);
					altitude.setChecked(true);
					pressure.setChecked(true);
					humidity.setChecked(true);
					heading .setChecked(true);
					temp.setChecked(true);
					light.setChecked(true);
				}
			}

		});
	}

	public class CreateProjectTask extends AsyncTask<Object, Void, Integer> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			OrientationManager.disableRotation(ProjectFieldDialog.this);

			dia = new ProgressDialog(ProjectFieldDialog.this);
			dia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dia.setMessage("Creating your new project...");
			dia.setCancelable(false);
			dia.show();
		}
		@Override
		protected void onPostExecute(Integer projNum) {
			super.onPostExecute(projNum);
			if (dia != null)
				dia.cancel();
			OrientationManager.enableRotation(ProjectFieldDialog.this);

			setResult(Activity.RESULT_OK);
			finish();
		}

		@Override
		protected Integer doInBackground(Object... params) {
			String projName = (String) params[0];

			// Make sure there are RProjectFields
			if (params[1] instanceof ArrayList<?>) {
				@SuppressWarnings("unchecked")
				ArrayList<RProjectField> fields = (ArrayList<RProjectField>) params[1];
				int projID = api.createProject(projName, fields);
				ProjectManager.setProject(mContext, String.valueOf(projID));
				return projID;
			} else {
				return -1;
			}
		}
	}
}
