package edu.uml.cs.isense.motion.dialogs;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import edu.uml.cs.isense.R;
import edu.uml.cs.isense.motion.Motion;

/**
 * Allows the user to identify his/her data sets with their chosen name. This
 * can allow for students to use a teacher's iSENSE Account, because each
 * student will have a unique identity. The Login activity should be called
 * before this activity.
 * 
 * @author iSENSE Android Dev Team
 */
public class EnterName extends AppCompatActivity {
	public static final String Name_Key = "NAME";
	private static final String blankName = "Do not leave name blank.";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.enter_name);

		final EditText nameInput = (EditText) findViewById(R.id.edittext_name);
		final Button okButton = (Button) findViewById(R.id.button_ok);

        SharedPreferences settings = getSharedPreferences(Motion.MY_SAVED_PREFERENCES, 0);
        String dataSetName = settings.getString(Name_Key, "");
        nameInput.setText(dataSetName);

		/*
		 * Write the user's information into memory.
		 */
		okButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (nameInput.length() == 0) {
					nameInput.setError(blankName);
				} else {
					SharedPreferences settings = getSharedPreferences(Motion.MY_SAVED_PREFERENCES, 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString(Name_Key, nameInput.getText().toString());
                    editor.commit();

                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
					finish();
				}
			}

		});
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

	@Override
	public void onBackPressed() {
		setResult(RESULT_CANCELED, null);
		super.onBackPressed();
	}

}
