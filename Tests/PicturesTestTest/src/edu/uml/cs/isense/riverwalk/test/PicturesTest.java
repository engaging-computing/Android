package edu.uml.cs.isense.riverwalk.test;

import android.test.ActivityInstrumentationTestCase2;
import edu.uml.cs.isense.riverwalk.Main;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.credentials.CredentialManager;
import junit.framework.Assert;
import android.view.View;
import com.robotium.solo.Solo;

public class PicturesTest extends ActivityInstrumentationTestCase2<Main> {

	Solo solo;
	
	public PicturesTest() {
		super(Main.class);
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
		enterName("Jeremy D");
		selectProject("248");
		logIn("mobile.fake@example.com","mobile");
		contTakePics();
		upload();
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

	public void enterName( final String Name) throws Exception {
		
		//enter name
		solo.clearEditText(0);
		solo.enterText(0,Name);
		
		//check if name saved
		if(solo.searchText(Name) == false)
			Assert.assertTrue(false);
	}
	
	public void selectProject(final String Number) throws Exception {
		
		//select project number
		solo.clickOnScreen(880, 108);
		solo.clearEditText(0);
		solo.enterText(0, Number);
		solo.clickOnButton("OK");
		
		//check if number saved
		solo.clickOnScreen(880, 108);
		if(solo.searchText(Number) == false)
			Assert.assertTrue(false);
		solo.clickOnButton("OK");
	}
	
	public void logIn(final String UserName, final String Password) throws Exception {
		
		// click login
		View actionbarItem1 = solo.getView(edu.uml.cs.isense.riverwalk.R.id.MENU_ITEM_LOGIN);
		solo.clickOnView(actionbarItem1);

		// am i logged in?
		if (CredentialManager.isLoggedIn()) {
			solo.clickOnButton("Log out");
			}

		// enter Username
		solo.clearEditText(0);
		solo.enterText(0, UserName);

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
		
	public void contTakePics() throws Exception {
		
		//check of continuous button
		solo.clickOnMenuItem("i");
		solo.clickOnText("Continuously Take Photos");
		
		//enter time interval
		solo.clearEditText(0);
		solo.enterText(0, "1");
		solo.clickOnButton("OK");
		
		//take pics
		solo.clickOnText("Press Here");
		solo.sleep(11000);
		solo.clickOnText("Recording");
		solo.sleep(2000);
		
		//check to see if pics saved
		solo.clickOnScreen(735, 110);
		if(solo.searchText("Type:BOTH") == false)
			Assert.assertTrue(false);
		solo.clickOnButton("Cancel");
		}
	
	public void upload() throws Exception {
		
		//click on uplad menu
		solo.clickOnScreen(735, 110);
		
		//upload data
		solo.clickOnButton("Upload");
		
		//check to see if data uploaded
		if(solo.searchText("upload failed"))
			Assert.assertTrue(false);
	
		solo.clickOnButton("OK");
		
		}
	
	}
	

