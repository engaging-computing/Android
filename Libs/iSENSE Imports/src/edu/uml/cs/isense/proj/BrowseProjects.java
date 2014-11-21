package edu.uml.cs.isense.proj;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import edu.uml.cs.isense.R;
import edu.uml.cs.isense.objects.RProject;

/**
 * This is the class designed to browse projects on the iSENSE website.
 * Projects will be displayed by their title and respective owner on iSENSE.
 *
 * This class has already been implemented in the
 * {@link edu.uml.cs.isense.proj.ProjectManager Setup} class, so no public implementation
 * is necessary.
 *
 * @author iSENSE Android Development Team
 *
 */
public class BrowseProjects extends ListActivity {
	private ProjectAdapter m_adapter;
	private ArrayList<RProject> m_projects;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.projects);

        setResult(Activity.RESULT_CANCELED);

        m_projects = new ArrayList<RProject>();
        m_adapter = new ProjectAdapter(getBaseContext(),
                R.layout.projectrow, R.layout.loadrow,
                m_projects);
        m_adapter.query = "";
        setListAdapter(m_adapter);

        //If api level is 11 or up layout will use SearchView, if not use textview
        if (android.os.Build.VERSION.SDK_INT > 10) {
            SearchView sv = (SearchView) findViewById(R.id.ExperimentSearchInput);
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    search(s);
                    return false;
                }
            });

        } else {

            final EditText et = (EditText) findViewById(R.id.ExperimentSearchInput);

            et.addTextChangedListener(new TextWatcher() {

                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count,
                                              int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before,
                                          int count) {
                    search(s.toString());
                }

            });
        }
    }

        public void search(String s) {
            if (s == null || s.length() == 0) {
                m_projects = new ArrayList<RProject>();
                m_adapter = new ProjectAdapter(getBaseContext(),
                        R.layout.projectrow, R.layout.loadrow,
                        m_projects);
                setListAdapter(m_adapter);
            } else {
                m_projects = new ArrayList<RProject>();
                m_adapter = new ProjectAdapter(getBaseContext(),
                        R.layout.projectrow, R.layout.loadrow,
                        m_projects);
                m_adapter.query = s.toString();
                setListAdapter(m_adapter);
            }


        }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		RProject p = m_projects.get(position);
		ProjectManager.setProject(this, String.valueOf(p.projectId));
		setResult(Activity.RESULT_OK);
		finish();
	}

}
