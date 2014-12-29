package edu.uml.cs.isense.motion;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import edu.uml.cs.isense.motion.dialogs.Presets;
import edu.uml.cs.isense.proj.ProjectManager;

public class PresetScreen extends ActionBarActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preset_screen);

        Context mContext = this;

        Button accelButton = (Button) findViewById(R.id.accel_button);
        Button gpsButton = (Button) findViewById(R.id.gps_button);
        Button continueButton = (Button) findViewById(R.id.continue_button);

        String project = ProjectManager.getProject(mContext);

        //if not gps or accel preset project give option to continue using last project
        if (!(project.equals(Presets.ACCEL_PROJECT) || project.equals(Presets.GPS_PROJECT))) {
            if (project.equals("-1")) {
                continueButton.setText("Select a Project Later");
            } else {
                continueButton.setText("Continue using project: " + project);
            }
            continueButton.setVisibility(View.VISIBLE);

        }

        accelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences(Motion.MY_SAVED_PREFERENCES, 0);
                SharedPreferences.Editor editor = settings.edit();

                editor.putString(Presets.LAST_PRESET, Presets.ACCEL);

                Intent resultIntent = new Intent();
                resultIntent.putExtra(Presets.RATE, Presets.ACCEL_PROJECT_RATE);
                resultIntent.putExtra(Presets.LENGTH, Presets.ACCEL_PROJECT_LENGTH);
                resultIntent.putExtra(Presets.PROJECT, Presets.ACCEL_PROJECT);
                resultIntent.putExtra(Presets.FIELD, Presets.ACCEL_CAROUSEL_POSITION);
                setResult(Activity.RESULT_OK, resultIntent);

                editor.commit();
                finish();
            }
        });

        gpsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences settings = getSharedPreferences(Motion.MY_SAVED_PREFERENCES, 0);
                SharedPreferences.Editor editor = settings.edit();

                editor.putString(Presets.LAST_PRESET, Presets.GPS);

                Intent resultIntent = new Intent();
                resultIntent.putExtra(Presets.RATE, Presets.GPS_PROJECT_RATE);
                resultIntent.putExtra(Presets.LENGTH, Presets.GPS_PROJECT_LENGTH);
                resultIntent.putExtra(Presets.PROJECT, Presets.GPS_PROJECT);
                resultIntent.putExtra(Presets.FIELD, Presets.GPS_CAROUSEL_POSITION);
                setResult(Activity.RESULT_OK, resultIntent);

                editor.commit();
                finish();

            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
            }
        });

    }
}
