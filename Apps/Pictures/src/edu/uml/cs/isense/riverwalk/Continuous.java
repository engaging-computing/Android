package edu.uml.cs.isense.riverwalk;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.view.ViewGroup;

public class Continuous extends ActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.continuous_shooting);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);


        if (android.os.Build.VERSION.SDK_INT >= 11)
        setFinishOnTouchOutside(false);

        final CheckBox continuous_cb = (CheckBox) findViewById(R.id.checkContinuous);
        continuous_cb.setChecked(Pictures.continuous); // if true then continuous_cb will be set to checked if not then it will not be checked

        final EditText continuous_time = (EditText) findViewById(R.id.etInterval);
        continuous_time.setText(String.valueOf(Pictures.continuousInterval));

        continuous_cb.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (continuous_cb.isChecked()){							//Continuous checkbox is checked
                    Pictures.continuous = true;
                    Pictures.addPicture.setVisibility(View.GONE);
                    Pictures.takePicture.setText(R.string.takePicContinuous);
                }else{													//Continuous checkbox is not checked
                    Pictures.continuous = false;
                    Pictures.addPicture.setVisibility(View.VISIBLE);
                    Pictures.takePicture.setText(R.string.takePicSingle);
                }
            }
        });

	final Button ok = (Button) findViewById(R.id.description_okay);
	ok.setOnClickListener(new OnClickListener() {
		@Override
		public void onClick(View v) {
			try{
				Pictures.continuousInterval = Integer.parseInt(continuous_time.getText().toString());
				if (Pictures.continuousInterval == 0)
					Pictures.continuousInterval = 1;
				finish();
			} catch(NumberFormatException e) {
				continuous_time.setError("Please Enter a Value.");
			}
		}
	});
	}
}
