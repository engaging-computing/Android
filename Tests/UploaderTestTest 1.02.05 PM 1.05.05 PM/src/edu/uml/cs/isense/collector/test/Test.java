package edu.uml.cs.isense.collector.test;

import android.test.ActivityInstrumentationTestCase2;
import edu.uml.cs.isense.collector.splash.Welcome;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.CredentialManager;
import junit.framework.Assert;
import android.view.View;
import com.robotium.solo.Solo;

public class Test extends ActivityInstrumentationTestCase2<Welcome> {
	
	Solo solo;
	
	public Test() {
		super(Welcome.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(), getActivity());
	}

	protected void tearDown() throws Exception {
		solo.finishOpenedActivities();
	}

	public void testApp() throws Exception {
		
		devMode();
		selectProject("594"); 
		/*setUpDataSet("Data","50","5");
		recordData();
		uploadDataWithKey("testing");
		logIn("mobile.fake@example.com","mobile");
		setUpLoggedIn("Data2","60","10");
		recordLoggedIn();
		uploadLoggedIn();*/
		enterDataManually("Data","13","15");
		uploadManualEntry();
	}
	
	public void devMode() throws Exception {
		
		// click isense logo 7 times
		for (int i = 0; i < 7; i++) {
			View actionbarItem11 = solo.getView(android.R.id.home);
			solo.clickOnView(actionbarItem11);
			Thread.sleep(500);
		}
		// check to see if in dev mode
		Assert.assertTrue(API.getInstance().isUsingDevMode());
	}
	
	public void selectProject( final String Number) throws Exception {
		
		//click on select project button
		solo.clickOnButton("Continue With New or Existing iSENSE Project");
		
		//enter project number
		solo.clearEditText(0);
		solo.enterText(0,Number);
		solo.clickOnButton("OK");
		
		//check to see if number saved
		solo.goBack();
		solo.clickOnButton("Continue With New or Existing iSENSE Project");
		if(solo.searchText(Number) == false)
			Assert.assertTrue(false);
		
		solo.clickOnButton("OK");
	}
	
	public void setUpDataSet( final String DataSet, final String Interval, final String Length) throws Exception {
		
		//use device's sensors
		solo.clickOnButton("Collect Data Using My Device's Hardware Sensors");
		
		//setup data sets
		solo.clickOnText("Step 1: Setup Your Data Set");
		
		//enter name for data set
		solo.clearEditText(0);
		solo.enterText(0, DataSet);
		
		//enter time interval
		solo.clearEditText(1);
		solo.enterText(1,Interval);
		
		//enter total length
		solo.clearEditText(2);
		solo.enterText(2, Length);	
		solo.clickOnButton("OK");
		
		//check to see if setup info saved
		solo.clickOnButton("Step 1: Setup Your Data Set");
		
		if(solo.searchText(DataSet) == false)
			Assert.assertTrue(false);
		
		else if(solo.searchText(Interval) == false)
			Assert.assertTrue(false);
	
		else if(solo.searchText(Length) == false)
			Assert.assertTrue(false);
		
		else {
			Assert.assertTrue(true);
		}
		
		solo.clickOnButton("OK");
		
	}
	
	public void recordData() throws Exception {
		
		//click on record data button
		View b = solo.getButton("Step 2");
		solo.clickLongOnView(b);
		Thread.sleep(6000);
		
		//enter description
		solo.enterText(0,"Description");
		solo.clickOnButton("Save");
		
		//check to see if data recorded
		if(solo.searchText("Data points: 0"))
			Assert.assertTrue(false);
		
		solo.clickOnButton("OK");
		
	}

	public void uploadDataWithKey(final String Key) throws Exception {
		
		solo.clickOnButton("Step 3: Upload Data Sets");
		solo.clickOnButton("Upload");
		solo.enterText(0, Key);
		solo.clickOnButton("OK");
		if(solo.searchText("upload failed"))
			Assert.assertTrue(false);
	
		solo.clickOnButton("OK");
	}
	
	public void logIn(final String Username, final String Password) throws Exception {
		
		// click login
		View actionbarItem1 = solo.getView(edu.uml.cs.isense.collector.R.id.menu_item_login);
		solo.clickOnView(actionbarItem1);

		// am i logged in?
		if (CredentialManager.isLoggedIn()) {
			solo.clickOnButton("Log out");
			}

		// enter Username
		solo.clearEditText(0);
		solo.enterText(0, Username);

		// enter password
		solo.clearEditText(1);
		solo.enterText(1, Password);

		// click login
		solo.clickOnButton("Login");
		Thread.sleep(2000);

		// verify that you are logged in
		Assert.assertNotNull(API.getInstance().getCurrentUser());
		
		solo.clickOnButton("OK");
		
		}
		
	public void setUpLoggedIn(final String DataSet, final String Interval, final String Length) throws Exception {
		
		//setup data sets
		solo.clickOnButton("Step 1: Setup Your Data Set");
				
		//enter name for data set
		solo.clearEditText(0);
		solo.enterText(0, DataSet);
				
		//enter time interval
		solo.clearEditText(1);
		solo.enterText(1,Interval);
				
		//enter total length
		solo.clearEditText(2);
		solo.enterText(2, Length);	
		solo.clickOnButton("OK");
				
		//check to see if setup info saved
		solo.clickOnButton("Step 1: Setup Your Data Set");
		
		if(solo.searchText(DataSet) == false)
			Assert.assertTrue(false);
			
		else if(solo.searchText(Interval) == false)
			Assert.assertTrue(false);
			
		else if(solo.searchText(Length) == false)
			Assert.assertTrue(false);
				
		else{
			Assert.assertTrue(true);
		}
	
		solo.clickOnButton("OK");
				
			}
	
	public void recordLoggedIn() throws Exception {
		
		//click on record data button
		solo.clickLongOnText("Step 2");
		Thread.sleep(11000);
				
		//enter description
		solo.enterText(0,"Description");
		solo.clickOnButton("Save");
				
		//check to see if data recorded
		if(solo.searchText("Data points: 0"))
			Assert.assertTrue(false);
				
		solo.clickOnButton("OK");
				
			}

		
	public void uploadLoggedIn() throws Exception {
		
		//click on upload button
		solo.clickOnButton("Step 3: Upload Data Sets");
		solo.clickOnButton("Upload");
		Thread.sleep(3000);
		
		if(solo.searchText("upload successful") == false)
			Assert.assertTrue(false);
		
		solo.clickOnButton("OK");  
		
		//go back a screen
		solo.goBack();
		}
	
	public void enterDataManually(final String Name, final String Number, final String Text) throws Exception {
		
		//click on manual entry
		solo.clickOnButton("Manually Enter Data");
		
		//enter name, numbers, text
		solo.enterText(0, Name);
		solo.enterText(2, Number);
		solo.enterText(3, Text);
		
		//save dataset
		solo.clickOnButton("Save");
		
		//click on upload menu item
		//View actionbarItem1 = solo.getView(edu.uml.cs.isense.collector.R.id.upload);
		//solo.clickOnView(actionbarItem1);
	
		solo.clickOnScreen(875,106);
		
		//check to see if manual entry saved
		if(solo.searchText(Name) == false)
			Assert.assertTrue(false);
		
		//click on upload button
		solo.clickOnButton("Upload");
	}
	
	public void uploadManualEntry() throws Exception {
		
		//click on upload button
		solo.clickOnButton("Upload");	
		
		if(solo.searchText("upload successful") == false)
			Assert.assertTrue(false);
		
		solo.clickOnButton("OK");
		
	}
	
	

}
	

