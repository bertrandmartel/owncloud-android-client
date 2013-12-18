/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.operations;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.oc_framework.network.webdav.OnDatatransferProgressListener;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.RemoteFile;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.remote.DownloadRemoteFileOperation;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.Log_OC;

import android.accounts.Account;
import android.webkit.MimeTypeMap;

/**
 * Remote mDownloadOperation performing the download of a file to an ownCloud server
 * 
 * @author David A. Velasco
 * @author masensio
 */
public class DownloadFileOperation extends RemoteOperation {
    
    private static final String TAG = DownloadFileOperation.class.getSimpleName();

    private Account mAccount;
    private OCFile mFile;
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet<OnDatatransferProgressListener>();
    private long mModificationTimestamp = 0;
    
    private DownloadRemoteFileOperation mDownloadOperation;

    
    public DownloadFileOperation(Account account, OCFile file) {
        if (account == null)
            throw new IllegalArgumentException("Illegal null account in DownloadFileOperation creation");
        if (file == null)
            throw new IllegalArgumentException("Illegal null file in DownloadFileOperation creation");
        
        mAccount = account;
        mFile = file;
        
    }


    public Account getAccount() {
        return mAccount;
    }
    
    public OCFile getFile() {
        return mFile;
    }

    public String getSavePath() {
        String path = mFile.getStoragePath();   // re-downloads should be done over the original file 
        if (path != null && path.length() > 0) {
            return path;
        }
        return FileStorageUtils.getDefaultSavePathFor(mAccount.name, mFile);
    }
    
    public String getTmpPath() {
        return FileStorageUtils.getTemporalPath(mAccount.name) + mFile.getRemotePath();
    }
    
    public String getTmpFolder() {
        return FileStorageUtils.getTemporalPath(mAccount.name);
    }
    
    public String getRemotePath() {
        return mFile.getRemotePath();
    }

    public String getMimeType() {
        String mimeType = mFile.getMimetype();
        if (mimeType == null || mimeType.length() <= 0) {
            try {
                mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(
                            mFile.getRemotePath().substring(mFile.getRemotePath().lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log_OC.e(TAG, "Trying to find out MIME type of a file without extension: " + mFile.getRemotePath());
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }
    
    public long getSize() {
        return mFile.getFileLength();
    }
    
    public long getModificationTimestamp() {
        return (mModificationTimestamp > 0) ? mModificationTimestamp : mFile.getModificationTimestamp();
    }

    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        File newFile = null;
        boolean moved = true;
        
        /// download will be performed to a temporal file, then moved to the final location
        File tmpFile = new File(getTmpPath());
        
        String tmpFolder =  getTmpFolder();
        RemoteFile remoteFile = FileStorageUtils.fillRemoteFile(mFile);
        
        /// perform the download
        mDownloadOperation = new DownloadRemoteFileOperation(remoteFile, tmpFolder);
        Iterator<OnDatatransferProgressListener> listener = mDataTransferListeners.iterator();
        while (listener.hasNext()) {
            mDownloadOperation.addDatatransferProgressListener(listener.next());
        }
        result = mDownloadOperation.execute(client);
        
        if (result.isSuccess()) {
            newFile = new File(getSavePath());
            newFile.getParentFile().mkdirs();
            moved = tmpFile.renameTo(newFile);
        
            if (!moved)
                result = new RemoteOperationResult(RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_MOVED);
        }
        Log_OC.i(TAG, "Download of " + mFile.getRemotePath() + " to " + getSavePath() + ": " + result.getLogMessage());
        
        
        return result;
    }

    public void cancel() {
        mDownloadOperation.cancel();
    }


    public void addDatatransferProgressListener (OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.add(listener);
        }
    }
    
    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        synchronized (mDataTransferListeners) {
            mDataTransferListeners.remove(listener);
        }
    }
    
}
