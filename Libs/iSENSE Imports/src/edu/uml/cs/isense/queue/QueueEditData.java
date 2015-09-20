package edu.uml.cs.isense.queue;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.ArrayList;

import edu.uml.cs.isense.R;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.objects.RProjectField;
import edu.uml.cs.isense.supplements.OrientationManager;

/**
 * Activity that allows the data-alteration of a data set.  
 * NOTE: Media objects have no "data" and thus cannot be altered.
 * Also, multiple-line data sets will only allow the first line of
 * data to be altered.
 * 
 * @author Mike Stowell and Jeremy Poulin of the iSENSE team.
 *
 */
public class QueueEditData extends ActionBarActivity {

	private Button save, cancel;
	private LinearLayout editDataList;
	
	public static QDataSet alter;
	private API api;
	
	private Context mContext;
	private ArrayList<RProjectField> fields;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.queueedit_data);

		alter = QueueLayout.lastDataSetLongClicked;
		
		mContext = this;
		
		fields = new ArrayList<RProjectField>();

		api = API.getInstance();

		save = (Button) findViewById(R.id.queueedit_data_save);
		cancel = (Button) findViewById(R.id.queueedit_data_cancel);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            ActionBar bar = getSupportActionBar();

            // make the actionbar clickable
            bar.setDisplayHomeAsUpEnabled(true);
        }

		save.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getNewFields();
				setResult(RESULT_OK);
				finish();
			}
		});

		cancel.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
		
		editDataList = (LinearLayout) findViewById(R.id.queueedit_data_layout);

		new LoadProjectFieldsTask().execute();

	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

	private void fillScrollView() {

		int i = 0;
		String rawData = alter.getData().replace("[", "").replace("]", "").replace("\"", "").replace("\\", "");
		String[] Data = rawData.split(",");
		int numPoints = 0;
        Log.e("DATATATATATATAT:",  rawData);
		if (fields.size() != 0) {
			numPoints = Data.length / fields.size();
		}
		// if the data is a space, remove the spaces
		for (int j = 0; j < Data.length; j++)
			if (Data[j].equalsIgnoreCase(" ")) Data[j] = "";


        for(int point=0; point < numPoints; point++) {
            TextView pointLabel = new TextView(this);
            pointLabel.setText("Point " + (point+1));
            pointLabel.setTextSize(16);
            pointLabel.setPadding(10, 20, 10, 0);
            pointLabel.setGravity(Gravity.CENTER);

            editDataList.addView(pointLabel);

            for (RProjectField rpf : fields) {
                final View dataRow = View.inflate(mContext, R.layout.edit_row, null);

                TextView label = (TextView) dataRow.findViewById(R.id.edit_row_label);
                label.setText(rpf.name);
                label.setBackgroundColor(Color.TRANSPARENT);
                label.setPadding(10, 10, 10, 0);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) label.getLayoutParams();
                params.setMargins(10, 0, 10, -10);
                label.setLayoutParams(params);

                EditText data = (EditText) dataRow.findViewById(R.id.edit_row_text);
                data.setLayoutParams(params);
                data.setText(Data[i]);
                // See if data is a number.  If not, change input type to text
                try {
                    Double.parseDouble(Data[i]);
                } catch (NumberFormatException nfe) {
                    data.setInputType(InputType.TYPE_CLASS_TEXT);
                }

                editDataList.addView(dataRow);

                ++i;
            }
        }
	}
	
	private void getNewFields() {
		JSONArray data = new JSONArray();
        String rawData = alter.getData();
        String[] Data = rawData.split(",");
        int numPoints = Data.length / fields.size();

        int i = 0;

        for (int point = 0; point < numPoints; point++) {
            JSONArray row = new JSONArray();
            for (int j = 0; j < (fields.size()+1); j++) { //fields + 1 (datapoint number label)
                    View v = editDataList.getChildAt(i);
                try {
                    EditText dataText = (EditText) v.findViewById(R.id.edit_row_text);
                    if (dataText.getText().toString().length() != 0)
                        row.put(dataText.getText().toString());
                    else
                        row.put(" ");
                } catch (Exception e) { //datapoint label
                    ++i;
                    continue;
                }
                ++i;
            }

            data.put(row);
        }

		alter.setData(data.toString());
		
	}

	private class LoadProjectFieldsTask extends AsyncTask<Void, Integer, Void> {
		ProgressDialog dia;
		private boolean error = false;

		@Override
		protected void onPreExecute() {

			OrientationManager.disableRotation(QueueEditData.this);

			dia = new ProgressDialog(QueueEditData.this);
			dia.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dia.setMessage("Loading data fields...");
			dia.setCancelable(false);
			dia.show();

			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {

			int projID = Integer.parseInt(alter.getProjID());
			if (projID != -1) {
				fields = api.getProjectFields(projID);
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			if (!error) {
				
				fillScrollView();

				dia.dismiss();
				OrientationManager.enableRotation(QueueEditData.this);
			}

			super.onPostExecute(result);
		}

	}

}