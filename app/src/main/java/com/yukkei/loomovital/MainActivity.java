package com.yukkei.loomovital;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.segway.robot.sdk.base.bind.ServiceBinder;
import com.segway.robot.sdk.vision.Vision;


import com.segway.robot.sdk.vision.stream.StreamType;
import com.yukkei.loomovital.heartrate.HeartRate;
import com.yukkei.loomovital.pose.GraphicOverlay;
import com.yukkei.loomovital.pose.PoseDetectPresenter;
import com.yukkei.loomovital.tools.MediaScanner;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.FpsMeter;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends CameraActivity implements CompoundButton.OnCheckedChangeListener, CameraBridgeViewBase.CvCameraViewListener2 {

    // Used to load the 'loomovital' library on application startup.
    static {
        System.loadLibrary("loomovital");
    }

    private static final String TAG = "MainActivity";


    private Vision mVision;

    private PoseDetectPresenter mPoseDetectPresenter;

    private SurfaceView mSurfaceView;
    private CameraBridgeViewBase mOpenCvCameraView;
    private TextView mDebugText;
    private Button mScreenShot;
    private GraphicOverlay overlay;
    private Switch mPreviewSwitch;
    private Switch mTransferSwitch;
    private CascadeClassifier faceDetector;
    private HeartRate mHeartRate;
    int mFPS;
    long startTime = 0;
    long currentTime = 1000;
    private MediaScanner scanner;
    private boolean startDetect;
    //here

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        scanner = new MediaScanner(getApplicationContext());
        mPreviewSwitch = (Switch) findViewById(R.id.preview);
        mTransferSwitch = (Switch) findViewById(R.id.transfer);

        mPreviewSwitch.setOnCheckedChangeListener(this);
        mTransferSwitch.setOnCheckedChangeListener(this);

        mScreenShot = findViewById(R.id.debug_button);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_surface_view);
        mOpenCvCameraView.setMaxFrameSize(640,480);
        mOpenCvCameraView.setMinimumWidth(640);
        mOpenCvCameraView.setMinimumHeight(480);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

//        mImageView = (ImageView) findViewById(R.id.displayImage);
        overlay = findViewById(R.id.overlayImage);
        mSurfaceView = findViewById(R.id.previewSurface);

        mVision = Vision.getInstance();
        mVision.bindService(this, mBindStateListener);

        mScreenShot.setOnClickListener(takeScreenShot);

        // Example of a call to a native method
        //TextView tv = binding.sampleText;
        // tv.setText(stringFromJNI());
    }

    View.OnClickListener takeScreenShot  = view -> {

        if(mPoseDetectPresenter != null && mPoseDetectPresenter.getStatus()){
            mPoseDetectPresenter.setScreenShot(true);

        }

        if(mHeartRate != null ){
            mHeartRate.setScreenShot(true);
        }

        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.US);
        String fileName = Environment.getExternalStorageDirectory().getPath() + "/Capture/" +"App overview"+ format.format(new Date())+ ".png";
        this.getWindow().getDecorView().setDrawingCacheEnabled(true);

        Bitmap bitmap = this.getWindow().getDecorView().getDrawingCache();

        if(bitmap!= null){
            Log.e("Screenshot","Screenshot Success!!");
            try{
                FileOutputStream out = new FileOutputStream(fileName);
                bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
                Log.e("Screenshot","output Success!!");

            }catch (Exception e){
                e.printStackTrace();
            }
        } else{
            Log.e("Screenshot", "No bitmap is found!");
        }
        scanner.scanCaptureDir();
    };

    ServiceBinder.BindStateListener mBindStateListener = new ServiceBinder.BindStateListener() {
        @Override
        public void onBind() {
            Log.d(TAG, "onBind() called");
            mPreviewSwitch.setEnabled(true);
            mTransferSwitch.setEnabled(true);
            if (mPoseDetectPresenter == null) {
                mPoseDetectPresenter = new PoseDetectPresenter(mVision, mSurfaceView, overlay);
                mPoseDetectPresenter.startPreview();
            }
            mOpenCvCameraView.enableView();
            Toast.makeText(getApplicationContext(), "Mvision bound", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onUnbind(String reason) {
            Log.d(TAG, "onUnbind() called with: reason = [" + reason + "]");
        }
    };



    @Override
    protected void onResume() {
        super.onResume();
        mPreviewSwitch.setChecked(false);
        mTransferSwitch.setChecked(false);

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        mVision.unbindService();
        finish();
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.preview:
                Log.i("clicked","is clicked");
                if (isChecked) {
                    if (mPoseDetectPresenter != null) {
                        mPoseDetectPresenter.start();
                    }

                } else {
                    mPoseDetectPresenter.stop();
                }

                break;

            case R.id.transfer:
                if (isChecked) {
                    startDetect = true;
                    mHeartRate.init();

                } else {
                    startDetect = false;
                    mHeartRate.reset();
                }
                break;
        }
    }



    private void initClassifier() {
        try {
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            faceDetector  = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if(faceDetector.empty()){
                faceDetector = null;
            }else{
                cascadeDir.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mHeartRate = new HeartRate();
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentTime - startTime >= 1000) {
                    mFPS = 0;
                    startTime = System.currentTimeMillis();
                }
                currentTime = System.currentTimeMillis();
                mFPS += 1;

            }
        });
        Log.d("Calculated fps", Integer.toString(mFPS));
        Mat frame = inputFrame.rgba();
        Mat mGray = inputFrame.gray();

        frame = mHeartRate.faceDetection(mGray,frame,faceDetector,startDetect);
//        frame = mHeartRate.onFrame(frame);

        return frame;
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    initClassifier();

                    mOpenCvCameraView.setMinimumWidth(640);
                    mOpenCvCameraView.setMinimumHeight(480);
                    Log.i(TAG, "OpenCV loaded successfully");

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };



    /**
     * A native method that is implemented by the 'loomovital' native library,
     * which is packaged with this application.
     */
    //    public native String stringFromJNI();
}