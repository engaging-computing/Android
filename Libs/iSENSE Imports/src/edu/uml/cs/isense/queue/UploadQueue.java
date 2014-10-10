package edu.uml.cs.isense.queue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import org.json.JSONArray;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import edu.uml.cs.isense.comm.API;
import edu.uml.cs.isense.queue.QDataSet.Type;

/**
 * Class that represents the queue of data sets.  This object
 * can be instantiated, have data sets added to it, and be rebuilt
 * at any time from the serializable file it is stored on.  In order
 * to use the iSENSE-Imports-Queue-Saving technology, an UploadQueue
 * object is necessary.
 *
 * @author Jeremy Poulin and Mike Stowell of the iSENSE team.
 *
 */
public class UploadQueue implements Serializable {

	// serializable key for UploadQueue class - DO NOT CHANGE
	private static final long serialVersionUID = -3036173866992721309L;
	public static final String SERIAL_ID = "upload_queue_object";

	protected Queue<QDataSet> queue;
	protected Queue<QDataSet> mirrorQueue;
	private static String parentName;
	private static Context mContext;
	private static API api;
	private QDataSet ds = null;

	/**
	 * This is the constructor for an UploadQueue object.
	 * All parameters are necessary and essential to ensure that
	 * this object works properly.
	 *
	 *
	 * @param parentName
	 * 			Name of the parent activity the UploadQueue is being made
	 * 			in.  This name is also used in making the serializable file
	 * 			where this UploadQueue will be stored
	 *
	 * @param context
	 * 			Context of the activity instantiating this object.
	 *
	 * @param api
	 * 			An API object that this UploadQueue object may utilize
	 * 			to upload data.
	 *
	 */
	public UploadQueue(String parentName, Context context,
			API api) {
		this.queue = new LinkedList<QDataSet>();
		this.mirrorQueue = new LinkedList<QDataSet>();

		UploadQueue.parentName = parentName;
		UploadQueue.mContext = context;
		UploadQueue.api = api;
	}

	/**
	 * Getter for the parent name of the object.
	 *
	 * @return The parent name of the object
	 */
	public String getParentName() {
		return parentName;
	}

	/**
	 * Getter for the context passed to the object.
	 *
	 * @return The context this object was instantiated with.
	 */
	public Context getContext() {
		return mContext;
	}

	/**
	 * Adds a data set to the queue and updates the serializable file
	 * the queue is stored on.
	 *
	 * @param ds
	 * 			The QDataSet object to be added to the UploadQueue.
	 */
	public void addDataSetToQueue(QDataSet ds) {
		queue.add(ds);
		mirrorQueue.add(ds);
		storeAndReRetrieveQueue(true);
	}

	// Saves Q_COUNT and uploadQueue into memory for later use
	protected void storeAndReRetrieveQueue(boolean rebuildMirrorQueue) {

		Queue<QDataSet> backupQueue = new LinkedList<QDataSet>();
		backupQueue.addAll(queue);

		// save Q_COUNT in SharedPrefs
		final SharedPreferences mPrefs = mContext.getSharedPreferences(
				parentName, Context.MODE_PRIVATE);
		final SharedPreferences.Editor mPrefsEditor = mPrefs.edit();
		int Q_COUNT = backupQueue.size();
		int Q_COUNT_BACKUP = Q_COUNT;
		mPrefsEditor.putInt(parentName + "Q_COUNT", Q_COUNT);
		mPrefsEditor.commit();

		// obtain storage directory and file for the uploadqueue
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/iSENSE");

		if (!folder.exists()) {
			folder.mkdir();
		}

		File uploadQueueFile = new File(folder.getAbsolutePath() + "/"
				+ parentName + ".ser");

		// writes the queue to a serializable file
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(uploadQueueFile));

			// serializes DataSets
			while (Q_COUNT > 0) {
				QDataSet ds = backupQueue.remove();
				out.writeObject(ds);
				Q_COUNT--;
			}

			out.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}

		// re-retrieve the queue
		queue = new LinkedList<QDataSet>();
		Q_COUNT = Q_COUNT_BACKUP;

		try {
			// Deserialize the file as a whole
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					uploadQueueFile));

			// Deserialize the objects one by one
			for (int i = 0; i < Q_COUNT; i++) {
				QDataSet dataSet = (QDataSet) in.readObject();
				queue.add(dataSet);
			}
			in.close();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (rebuildMirrorQueue) {
			mirrorQueue.clear();
			mirrorQueue.addAll(queue);
		}


		try {
			if (out != null)
				out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// removes the dataset with the associated key: true if removed, false if not found
	protected boolean removeItemWithKey(long keyVal) {
		LinkedList<QDataSet> backup = new LinkedList<QDataSet>();
		backup.addAll(queue);
		for (QDataSet ds : backup) {
			if (ds.key == keyVal) {
				queue.remove(ds);
				mirrorQueue.remove(ds);
				storeAndReRetrieveQueue(true);
				return true;
			}
		}

		return false;
	}

	/**
	 * Rebuilds the UploadQueue from the serializable file it
	 * is saved on.
	 *
	 * @return
	 * 			@true if the UploadQueue is rebuilt successfully
	 * 			@false if the rebuilding fails
	 */
	public boolean buildQueueFromFile() {

		// reset the queues but save a backup
		Queue<QDataSet> backupQueue = new LinkedList<QDataSet>();
		backupQueue.addAll(queue);
		queue       = new LinkedList<QDataSet>();
		mirrorQueue = new LinkedList<QDataSet>();

		// get Q_COUNT from the SharedPrefs
		final SharedPreferences mPrefs = mContext.getSharedPreferences(
				parentName, Context.MODE_PRIVATE);
		int Q_COUNT = mPrefs.getInt(parentName + "Q_COUNT", -1);
		if (Q_COUNT == -1) {
			queue.addAll(backupQueue);
			mirrorQueue.addAll(backupQueue);
			return false;
		}

		// obtain storage directory and file for the uploadqueue
		File folder = new File(Environment.getExternalStorageDirectory()
				+ "/iSENSE");
		if (!folder.exists()) {
			queue.addAll(backupQueue);
			mirrorQueue.addAll(backupQueue);
			return false;
		}

		File uploadQueueFile = new File(folder.getAbsolutePath() + "/"
				+ parentName + ".ser");

		try {
			// Deserialize the file as a whole
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					uploadQueueFile));

			// Deserialize the objects one by one
			for (int i = 0; i < Q_COUNT; i++) {
				QDataSet dataSet = (QDataSet) in.readObject();
				queue.add(dataSet);
			}
			in.close();

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			queue.addAll(backupQueue);
			mirrorQueue.addAll(backupQueue);
			return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			queue.addAll(backupQueue);
			mirrorQueue.addAll(backupQueue);
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			queue.addAll(backupQueue);
			mirrorQueue.addAll(backupQueue);
			return false;
		}

		mirrorQueue.addAll(queue);
		return true;
	}

	/**
	 * Getter for the API object.
	 *
	 * @return The API object the UploadQueue was instantiated with.
	 */
	public static API getAPI() {
		return api;
	}

	/**
	 * Determines if the queue of data sets is empty.
	 *
	 * @return
	 * 			@true if the queue is empty
	 * 			@false if the queue has data sets
	 */
	public boolean emptyQueue() {
		return (queue.size() == 0);
	}

	/**
	 * Determines the amount of data sets in the queue.
	 *
	 * @return	The amount of data sets stored in the queue.
	 */
	public int queueSize() {
		return (queue.size());
	}

	/**
	 * @param name
	 *            - The name of the of the data set
	 * @param desc
	 *            - A description for the data set
	 * @param type
	 *            - QDataSet.DATA QDataSet.PIC, or QDataSet.BOTH
	 * @param projID
	 *            - The associated project ID for the data set
	 * @param data
	 *            - If type is QDataSet.DATA, we look here.
	 * @param picture
	 *            - If type is QDataSet.PIC, we look here.
	 * @param requestDataLabelInOrder
	 * 			  - If data is already in the correct order and just needs to be given field IDs
	 */
	public void addToQueue(String name, String description, Type type, JSONArray dataSet, File picture, String projID, LinkedList<String>fields, Boolean dataAlreadyInOrder) {
		ds = new QDataSet(name + appendedTimeStamp(), description, type, dataSet.toString(), picture, projID, fields, dataAlreadyInOrder);

		//Add data to Queue to be uploaded
		new AddToQueueTask().execute();
	}

	/**
	 * Creates a unique date and timestamp used to append to data sets uploaded
	 * to the iSENSE website to ensure every data set has a unique identifier.
	 *
	 * @return A pretty formatted date and timestamp
	 */
	private String appendedTimeStamp() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"MM/dd/yy, HH:mm:ss.SSS", Locale.US);
		Calendar cal = Calendar.getInstance();

		return " " + dateFormat.format(cal.getTime());
	}

	/**
	 * Saves Data to upload Queue
	 *
	 * @author jpoulin
	 * moved to UploadQueue - BDonald
	 */
	class AddToQueueTask extends AsyncTask<String, Void, String> {

		ProgressDialog dia;

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected String doInBackground(String... strings) {
			uploader.run();
			return null; //strings[0];
		}

		@Override
		protected void onPostExecute(String sdFileName) {
			Intent i = new Intent().setClass(mContext, QueueLayout.class);
			i.putExtra(QueueLayout.PARENT_NAME, getParentName());
			mContext.startActivity(i);
		}
	}

	// Calls the api primitives for actual uploading
		private final Runnable uploader = new Runnable() {
			@Override
			public void run() {
				addDataSetToQueue(ds);
			}
		};



}
