package edu.uml.cs.isense.proj;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import edu.uml.cs.isense.R;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.comm.Connection;
import edu.uml.cs.isense.credentials.CredentialManager;
import edu.uml.cs.isense.objects.RProjectField;
import edu.uml.cs.isense.supplements.ObscuredSharedPreferences;
import edu.uml.cs.isense.waffle.Waffle;

/**
 * This Activity is designed to select a project from the iSENSE website. It
 * features an EditText that the user may manually enter a project ID into, a
 * Browse feature to pick from a list of projects, and a QR code scanning
 * feature to find the project ID from a project on iSENSE.
 *
 * To use this Activity, launch an Intent to this class and catch it in your
 * onActivityResult() method. To obtain the project ID returned by this
 * Activity, create a SharedPreferences object using the PREFS_ID variable as
 * the "name" parameter. Then, request a String with the PROJECT_ID "key"
 * parameter. For example:
 *
 * <pre>
 * {
 * 	&#064;code
 * 	SharedPreferences mPrefs = getSharedPreferences(Setup.PREFS_ID, 0);
 * 	String projID = mPrefs.getString(Setup.PROJECT_ID, &quot;-1&quot;);
 * }
 * </pre>
 *
 * @author iSENSE Android Development Team
 */
public class ProjectManager extends Activity implements OnClickListener {

	private EditText projInput;

	private Button okay;
	private Button cancel;
//	private Button qrCode;
	private Button browse;
	private Button createProject;
	private Button projLater;

	private LinearLayout oklayout;
	private LinearLayout selectLaterLayout;

	private Context mContext;
	private Waffle w;
	private API api;

	private String projectID = "-1";

	private static final String PROJECT_ID_KEY = "projectKey";
	private static final String PROJ_PREFS_ID = "projectPrefsId";


	private static final int QR_CODE_REQUESTED = 100;
	private static final int PROJECT_CODE = 101;
	private static final int NO_QR_REQUESTED = 102;
	private static final int NAME_FOR_NEW_PROJECT_REQUESTED = 103;
	private static final int NEW_PROJ_REQUESTED = 104;
	private static final int LOGIN_STATUS_REQUESTED = 105;

	private static final String DEFAULT_NO_PROJ = "-1";

	public static String APPNAME;

	private boolean showOKCancel = true;
	private boolean constrictFields = false;
	private boolean themeNavBar = false;
	private boolean showSelectLater = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.project_id);

		mContext = this;

		api = API.getInstance();

		w = new Waffle(mContext);

		okay = (Button) findViewById(R.id.project_ok);
		okay.setOnClickListener(this);

		cancel = (Button) findViewById(R.id.project_cancel);
		cancel.setOnClickListener(this);

//		qrCode = (Button) findViewById(R.id.project_qr);
//		qrCode.setOnClickListener(this);

		browse = (Button) findViewById(R.id.project_browse);
		browse.setOnClickListener(this);

		createProject = (Button) findViewById(R.id.createProjectBtn);
		createProject.setOnClickListener(this);

		projLater = (Button) findViewById(R.id.project_later);
		projLater.setOnClickListener(this);

		oklayout = (LinearLayout) findViewById(R.id.OKCancelLayout);
		oklayout.setVisibility(View.VISIBLE);

		selectLaterLayout = (LinearLayout) findViewById(R.id.select_later_layout);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			showOKCancel = extras.getBoolean("showOKCancel", true);
			constrictFields = extras.getBoolean("constrictFields", false);
			themeNavBar = extras.getBoolean(ProjectCreate.THEME_NAV_BAR, false);
			showSelectLater = extras.getBoolean("showSelectLater", true);

			if (!showOKCancel)
				oklayout.setVisibility(View.GONE);

			if(!showSelectLater)
				selectLaterLayout.setVisibility(View.GONE);

			//TODO remove shared prefs
			projectID = getProject(this);

			projInput = (EditText) findViewById(R.id.projectInput);
			if (projectID.equals("-1")) {
				projInput.setText("");
			} else {
				projInput.setText(projectID);
			}

		}
	}

	public static void setProject(Context appContext, String projectID) {
		SharedPreferences mPrefs = new ObscuredSharedPreferences(
				appContext, appContext.getSharedPreferences(ProjectManager.PROJ_PREFS_ID,
						MODE_PRIVATE));
		mPrefs.edit().putString(ProjectManager.PROJECT_ID_KEY, projectID).commit();
	}

	public static String getProject(Context appContext) {
		SharedPreferences mPrefs = new ObscuredSharedPreferences(
				appContext, appContext.getSharedPreferences(ProjectManager.PROJ_PREFS_ID,
						MODE_PRIVATE));
		String projID = mPrefs.getString(ProjectManager.PROJECT_ID_KEY, DEFAULT_NO_PROJ);

		return projID;
	}

	@Override
	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.project_ok) {
			boolean pass = true;
			if (projInput.getText().length() == 0) {
				projInput.setError("Enter a project ID");
				pass = false;
			}
			if (pass) {
				setProject(mContext, projInput.getText().toString());
				setResult(RESULT_OK);
				finish();
			}
		} else if (id == R.id.project_cancel) {
			setResult(RESULT_CANCELED);
			finish();
		//Find project id by QR code (currently website does not have QR codes anymore)
//		} else if (id == R.id.project_qr) {
//			try {
//				Intent intent = new Intent(
//						"com.google.zxing.client.android.SCAN");
//
//				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//
//				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
//				startActivityForResult(intent, QR_CODE_REQUESTED);
//			} catch (ActivityNotFoundException e) {
//				Intent iNoQR = new Intent(Setup.this, NoQR.class);
//				startActivityForResult(iNoQR, NO_QR_REQUESTED);
//			}
		} else if (id == R.id.project_browse) {
			Intent iProject = new Intent(getApplicationContext(),
					BrowseProjects.class);

			startActivityForResult(iProject, PROJECT_CODE);

		} else if (id == R.id.project_later) {
			setProject(mContext, "-1");
			setResult(Activity.RESULT_OK);
			finish();

		} else if (id == R.id.createProjectBtn) {
			if (!Connection.hasConnectivity(mContext))
				w.make("Internet connection required to create project",
						Waffle.LENGTH_LONG, Waffle.IMAGE_WARN);
			else if (api.getCurrentUser() == null) {
				w.make("Login required to create project",
						Waffle.LENGTH_LONG, Waffle.IMAGE_WARN);
				startActivityForResult(new Intent(mContext, CredentialManager.class),
						LOGIN_STATUS_REQUESTED);
			} else {
				if (!constrictFields) {
					Intent iProjCreate = new Intent(getApplicationContext(),
							ProjectCreate.class);
					iProjCreate.putExtra(ProjectCreate.THEME_NAV_BAR,
							themeNavBar);
					startActivityForResult(iProjCreate, NEW_PROJ_REQUESTED);
				} else {
					Intent iNewProjName = new Intent(getApplicationContext(),
							ProjectFieldDialog.class);
					startActivityForResult(iNewProjName,
							NAME_FOR_NEW_PROJECT_REQUESTED);
				}
			}
		}

	}

	// Performs tasks after returning to main UI from previous activities
	@SuppressWarnings("unchecked")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == QR_CODE_REQUESTED) {
			if (resultCode == RESULT_OK) {
				String contents = data.getStringExtra("SCAN_RESULT");

				String delimiter = "projects/";
				String[] split = contents.split(delimiter);

				try {
					projInput.setText(split[1]);
					Integer.parseInt(split[1]);
				} catch (ArrayIndexOutOfBoundsException e) {
					w.make("Invalid QR code scanned", Waffle.LENGTH_LONG,
							Waffle.IMAGE_X);
				} catch (NumberFormatException nfe) {
					w.make("Invalid QR code scanned", Waffle.LENGTH_LONG,
							Waffle.IMAGE_X);
				}

			}
		} else if (requestCode == PROJECT_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				String projID = getProject(this);
				projInput.setText(projID);

			}
		} else if (requestCode == NO_QR_REQUESTED) {
			if (resultCode == RESULT_OK) {
				String url = "https://play.google.com/store/apps/details?id=com.google.zxing.client.android";
				Intent urlIntent = new Intent(Intent.ACTION_VIEW);
				urlIntent.setData(Uri.parse(url));
				startActivity(urlIntent);
			}
		} else if (requestCode == NAME_FOR_NEW_PROJECT_REQUESTED) {
			if (resultCode == RESULT_OK) {
				String projID = getProject(this);
				projInput.setText(projID);
			}
		} else if (requestCode == NEW_PROJ_REQUESTED) {
			if (resultCode == RESULT_OK) {
				if (data.hasExtra(ProjectCreate.NEW_PROJECT_ID)) {
					//TODO remove s
					String projID = getProject(this);
					projInput.setText(projID);
				}
			} else {
				setResult(RESULT_CANCELED);
				finish();
			}
		} else if (requestCode == LOGIN_STATUS_REQUESTED) {
			if (resultCode == RESULT_OK) {

				w.make("Login successful", Waffle.LENGTH_SHORT,
						Waffle.IMAGE_CHECK);

			} else if (resultCode == CredentialManager.RESULT_ERROR) {

				startActivityForResult(new Intent(mContext, CredentialManager.class),
						LOGIN_STATUS_REQUESTED);

			}
		}
	}

	public class CreateProjectTask extends AsyncTask<Object, Void, Integer> {

		@Override
		protected void onPostExecute(Integer projNum) {
			super.onPostExecute(projNum);

			setProject(mContext, String.valueOf(projNum));
			projInput.setText(String.valueOf(projNum));

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
				return api.createProject(projName, fields);
			} else {
				return -1;
			}
		}
	}


	@Override
	public void onBackPressed() {
		setResult(RESULT_CANCELED);
		finish();
	}
}
