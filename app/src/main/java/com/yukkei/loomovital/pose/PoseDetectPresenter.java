package com.yukkei.loomovital.pose;


import android.graphics.Bitmap;

import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import com.segway.robot.sdk.vision.Vision;

import com.segway.robot.sdk.vision.stream.StreamType;
import com.yukkei.loomovital.tools.ImageIO;

public class PoseDetectPresenter {

    private static final String TAG = "PreviewPresenter";

    private Vision mVision;
    private SurfaceView mSurfaceView;
    private GraphicOverlay overlay;

    private float ratio;
    private ViewGroup.LayoutParams layout;
    private PoseDetector poseDetector;
    private boolean isDetecting;
    private ImageIO imageIO;
    private boolean screenShot;


    public PoseDetectPresenter(Vision mVision, SurfaceView mSurfaceView, GraphicOverlay overlay){
        this.mVision = mVision;
        this.mSurfaceView = mSurfaceView;
        this.overlay = overlay;
        this.screenShot = false;
        this.imageIO = new ImageIO();

    }


    public synchronized void start() {
        initPoseDetection();
        startPoseDetection();
        isDetecting = true;
    }

    private synchronized void initPoseDetection() {
        // Base pose detector with streaming frames, when depending on the pose-detection sdk
        PoseDetectorOptions options =
                new PoseDetectorOptions.Builder()
                        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                        .build();
        poseDetector = PoseDetection.getClient(options);
    }

    public synchronized void startPreview() {
        Log.d(TAG, "previewing");
        // preview color stream
        if (this.mVision != null) {
            ratio = (float) 320 / 240;
//            ViewGroup.LayoutParams layout;
            mSurfaceView.getHolder().setFixedSize(320, 240);
            layout = mSurfaceView.getLayoutParams();
            layout.width = (int) (mSurfaceView.getHeight() * ratio);
            mSurfaceView.setLayoutParams(layout);

            // Get activated stream info from Vision Service. Streams are pre-config.
            mVision.startPreview(StreamType.COLOR, mSurfaceView.getHolder().getSurface());
        }


    }

    public void startPoseDetection(){
        Bitmap mBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

        mVision.startListenFrame(StreamType.COLOR, (streamType, frame) -> {
            mBitmap.copyPixelsFromBuffer(frame.getByteBuffer());
            if (screenShot){
                imageIO.saveBitmap(mBitmap);
                screenShot = false;
                Log.e("Screenshot","Take screen shots success!");
            }
            InputImage image = InputImage.fromBitmap(mBitmap,0);
            overlay.setLayoutParams(layout);
            overlay.setImageSourceInfo(640,480,false);
            Task<Pose> result =
                    poseDetector.process(image)
                            .addOnSuccessListener(
                                    pose -> {
//                                            Log.e(TAG, "DETECT SUCCESS");
                                        overlay.clear();

                                        overlay.add(
                                                new PoseGraphic(
                                                        overlay,
                                                        pose,
                                                        true,
                                                        false)
                                        );

                                    })
                            .addOnFailureListener(
                                    e -> Log.e(TAG, "Pose detection failed!", e)
                            );

        });

    }

    public void setScreenShot(boolean screenShot){
        this.screenShot = screenShot;
    }

    public boolean getStatus(){
        return  isDetecting;
    }

    public synchronized void stop() {
        Log.d(TAG, "stop() called");

//        mVision.stopPreview(StreamType.COLOR);
        mVision.stopListenFrame(StreamType.COLOR);
        overlay.clear();

        isDetecting = false;
    }

}
