package com.owncloud.android.oc_framework_test_project;

import java.io.IOException;

import com.owncloud.android.oc_framework.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.oc_framework.network.webdav.OwnCloudClientFactory;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.remote.CreateRemoteFolderOperation;
import com.owncloud.android.oc_framework.operations.remote.RenameRemoteFileOperation;

import android.os.AsyncTask;
import android.os.Bundle;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;

/**
 * Activity to test OC framework
 * @author masensio
 *
 */
public class TestActivity extends Activity {
	
	private static final String TAG = "TestActivity";
	
	// This account must exists on the simulator / device
	private static final String mAccountHost = "beta.owncloud.com";
	private static final String mAccountUser = "testandroid";
	private static final String mAccountName = mAccountUser + "@"+ mAccountHost;
	private static final String mAccountPass = "testandroid";
	private static final String mAccountType = "owncloud";	
	
	private Account mAccount = null;
	private WebdavClient mClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);

		AccountManager am = AccountManager.get(this);
		
		Account[] ocAccounts = am.getAccountsByType(mAccountType);
        for (Account ac : ocAccounts) {
           if (ac.name.equals(mAccountName)) {
        	   mAccount = ac;
        	   break;
            }
        }

        // Get the WebDavClient
        AuthTask task = new AuthTask();
        task.execute(this.getApplicationContext());
        
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.test, menu);
		return true;
	}

	/**
	 * Access to the library method to Create a Folder
	 * @param remotePath
	 * @param createFullPath
	 * 
	 * @return
	 */
	public RemoteOperationResult createFolder(String remotePath, boolean createFullPath) {
		
		CreateRemoteFolderOperation createOperation = new CreateRemoteFolderOperation(remotePath, createFullPath);
		RemoteOperationResult result =  createOperation.execute(mClient);
		
		return result;
	}
	
	/**
	 * Access to the library method to Rename a File or Folder
	 * @param oldName			Old name of the file.
     * @param oldRemotePath		Old remote path of the file. For folders it starts and ends by "/"
     * @param newName			New name to set as the name of file.
     * @param isFolder			'true' for folder and 'false' for files
     * 
     * @return
     */

	public RemoteOperationResult renameFile(String oldName, String oldRemotePath, String newName, boolean isFolder) {
		
		RenameRemoteFileOperation renameOperation = new RenameRemoteFileOperation(oldName, oldRemotePath, newName, isFolder);
		RemoteOperationResult result = renameOperation.execute(mClient);
		
		return result;
	}
	
	private class AuthTask extends AsyncTask<Context, Void, WebdavClient> {

		@Override
		protected WebdavClient doInBackground(Context... params) {
			WebdavClient client = null;
			try {
				client = OwnCloudClientFactory.createOwnCloudClient(mAccount, (Context) params[0] );
			} catch (OperationCanceledException e) {
				Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
				e.printStackTrace();
			} catch (AuthenticatorException e) {
				Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
				e.printStackTrace();
			} catch (AccountNotFoundException e) {
				Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
				e.printStackTrace();
			} catch (IllegalStateException e) {
				Log.e(TAG, "Error while trying to access to " + mAccount.name, e);
				e.printStackTrace();
			}
			return client;
		}

		@Override
		protected void onPostExecute(WebdavClient result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mClient = result;
		}
		
	}
}
