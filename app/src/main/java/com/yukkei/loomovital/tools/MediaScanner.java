package com.yukkei.loomovital.tools;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * @date: 24.04.2022
 * @author Zhouyao
 * This is the class to handle MTP issue
 * The MTP folder will not refresh, sometimes invisiable, unless the android machine reboots
 * Use scanFile() to refresh the file or dir, so that make it reload
 */
public class MediaScanner {
    private String TAG = "Media Scanner";

    private MediaScannerConnection mConn = null;
    private SannerClient mClient = null;
    private File mFile = null;
    private String mMimeType = null;

    public MediaScanner(Context context) {
        if (mClient == null) {
            mClient = new SannerClient();
        }
        if (mConn == null) {
            mConn = new MediaScannerConnection(context, mClient);
        }
    }

    class SannerClient implements MediaScannerConnection.MediaScannerConnectionClient {

        public void onMediaScannerConnected() {

            if (mFile == null) {
                return;
            }
            scan(mFile, mMimeType);
        }

        private void scan(File file, String type) {
            Log.e(TAG, "scan " + file.getAbsolutePath());
            if (file.isFile()) {
                mConn.scanFile(file.getAbsolutePath(), null);
                return;
            }
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File f : file.listFiles()) {
                scan(f, type);
            }
        }

        @Override
        public void onScanCompleted(String s, Uri uri) {

        }
    }

    /**
     * Method to refresh folder in MTP dir
     *
     * @param file     the dir or file you want to refresh
     * @param mimeType use null as default
     */
    public void scanFile(File file, String mimeType) {
        mFile = file;
        mMimeType = mimeType;
        mConn.connect();
    }

    /**
     * Method to refresh the dir where data for slam is installed
     */
    public void scanCaptureDir() {
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/Capture");
        scanFile(f, null);
    }
}
