package edu.uml.cs.isense.motion.fields;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import edu.uml.cs.isense.motion.Motion;
import edu.uml.cs.isense.motion.R;

public class AltFragment extends Fragment implements LocationListener{

	private LocationManager mLocManager;
	TextView alt;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.alt_fragment, container, false);

        alt = (TextView) rootView.findViewById(R.id.alt);

        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        mLocManager = (LocationManager) Motion.mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocManager.requestLocationUpdates(mLocManager.getBestProvider(c, true), 0, 0, this);

        return rootView;
    }

    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	mLocManager.removeUpdates(this);
    }

    @Override
	public void onPause() {
		super.onPause();
    	mLocManager.removeUpdates(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        mLocManager.requestLocationUpdates(mLocManager.getBestProvider(c, true), 0, 0, this);
	}

	@Override
	public void onLocationChanged(Location location) {
		try {
			alt.setText(""+ location.getAltitude() + " meters");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}


}