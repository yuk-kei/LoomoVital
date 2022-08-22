package com.yukkei.loomovital.heartrate;

import android.graphics.Bitmap;
import android.util.Log;

import com.yukkei.loomovital.tools.ImageIO;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


public class HeartRate implements Runnable{


    private final Scalar ZERO = Scalar.all(0);
    private final Scalar ALPHA_FACTOR = Scalar.all(200);
    private final Scalar  faceRectColor;

    private double RATE = 22;
    private final int PYRAMID_LEVEL = 4;
    private final double LOW_FREQUENCY = 0.85;//=45/60f;
    private final double HIGH_FREQUENCY = 3.33; // 240/60f;
    private final int BUFFER_SIZE = 30;

    private final int HIGH_FREQUENCY_FRAME = (int)(HIGH_FREQUENCY/RATE*BUFFER_SIZE + 1);
    private final int LOW_FREQUENCY_FRAME = (int)(LOW_FREQUENCY/RATE*BUFFER_SIZE + 1);

    private int counter;

    private Mat blurredImage;
    private Mat outputImage;
    private Mat outputFloat;
    private Mat frameFloat;
    private Mat bufferMat;
    private Mat filteredBufferMat;
    private List<Mat> bufferChannels;
    private Mat DFTBufferChannel;
    private Mat IDFTBufferChannel;

    private Rect faceRect;
    private Mat faceROI;

    private List<Mat> cl;
    private LinkedList<Double> markData;
    private List<Integer> sign;
    private List<Integer> signals;
    private LinkedList<Double> smoothedData;
    private ImageIO imageIO;

    private int mAbsoluteFaceSize = 0;
    private boolean isSignalBufferFull;
    private boolean screenshot;

    private MatOfRect faces;
    private int heartBeat;


    public HeartRate(){
        faceRectColor = new Scalar(0, 255, 0, 255);
        init();
        imageIO = new ImageIO();
        screenshot = false;
    }

    public Mat faceDetection(Mat mGray,Mat mRgba, CascadeClassifier faceDetector, boolean startDetect){

        if(startDetect && faceRect != null){
            mRgba = onFrameWithROI(mRgba,faceRect);
            Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), faceRectColor, 3);

            Imgproc.putText (mRgba,Integer.toString(heartBeat),new Point(50,100),Imgproc.FONT_HERSHEY_SIMPLEX,1,new Scalar(255,0,0),2);
            if(sign != null && isSignalBufferFull){
//                drawSignal(mRgba,sign);
                drawPulse(smoothedData,mRgba);
            }
            if ( getHeartRate() > 55 & getHeartRate() < 100){
                Imgproc.putText (mRgba,"Normal",new Point(50,80),Imgproc.FONT_HERSHEY_SIMPLEX,1,new Scalar(255,0,0),2);

            }

            if (screenshot){
                takeScreenshot(mRgba);
                screenshot = false;
            }
        } else {
            float mRelativeFaceSize = 0.3f;
            faces = new MatOfRect();

            if (mAbsoluteFaceSize == 0) {
                int height = mGray.rows();
                if (Math.round(height * mRelativeFaceSize) > 0) {
                    mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                }
            }

            if (faceDetector != null)
                faceDetector.detectMultiScale(mGray, faces, 1.1, 2, 2,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
            Rect[] facesArray = faces.toArray();

            for (Rect face : facesArray){

                Imgproc.rectangle(mRgba, face.tl(), face.br(), faceRectColor, 3);
                this.faceRect = face;

            }
        }

        return mRgba;
    }

    public Mat onFrameWithROI(Mat frame, Rect roi){

        faceROI = frame.submat(roi).clone();
        faceROI = EVM(faceROI);
        faceROI.copyTo(frame.submat(roi));

        return frame;
    }


    public Mat EVM(Mat frame){

        frame.convertTo(frameFloat, CvType.CV_32F);
        Imgproc.cvtColor(frameFloat,frameFloat,Imgproc.COLOR_RGBA2RGB);

        spatialFiltering();

        if(counter < BUFFER_SIZE){
            frame.copyTo(outputImage);

            if(bufferMat.empty()){
                bufferMat.create(blurredImage.width()*blurredImage.height(),BUFFER_SIZE,blurredImage.type());
            }
            concat(blurredImage,bufferMat,counter);

        } else {
            bufferMat.colRange(1,BUFFER_SIZE).copyTo(bufferMat.colRange(0,BUFFER_SIZE - 1));
            concat(blurredImage,bufferMat,BUFFER_SIZE - 1 );

            temporalIdealFilter();

            amplifyColor();

            addBack2NewFrame(frame);

            Imgproc.cvtColor(outputFloat, outputFloat, Imgproc.COLOR_RGB2RGBA);

            outputFloat.convertTo(outputImage, CvType.CV_8U);
//            Core.mean(outputFloat).val.toString();

            calculateHeartRate();

        }
        counter ++;
        if (screenshot){
            takeScreenshot(outputImage);
            screenshot = false;
            Log.e("Screenshot", "Heart rate screenshot!");
        }
        return outputImage;
    }

    private void addBack2NewFrame(Mat frame) {
        for(int i = 0; i < PYRAMID_LEVEL; i++){
            Imgproc.pyrUp(blurredImage,blurredImage);
        }

        Imgproc.resize(blurredImage, outputFloat, frame.size());
        Core.add(frameFloat, outputFloat, outputFloat);
    }

    private void amplifyColor() {
        getLastFromBuffer(filteredBufferMat, blurredImage);
        Core.multiply(blurredImage, ALPHA_FACTOR, blurredImage);
    }

    private void spatialFiltering() {
        frameFloat.copyTo(blurredImage);
        for (int i = 0; i < PYRAMID_LEVEL; i++){
            Imgproc.pyrDown(blurredImage,blurredImage);
        }
    }

    private void calculateHeartRate() {
        Core.split(outputImage,cl);
        Scalar mean = Core.mean(cl.get(1));
//        int lag = 10;
//        double threshold = 5;
//        double influence = 0.2;
//        int peaks = 0;


        if(markData.size() > BUFFER_SIZE *10 ){
            isSignalBufferFull = true;
            markData.poll();

//            signals = analyzeDataForSignals(markData,lag,threshold,influence).get("signals");
//            for (int i = 1; i < signals.size(); i++) {
//                int difference =  (signals.get(i) - signals.get(i-1));
//                if (difference > 0) {
//                    peaks++;
//                }
//            }
//            Log.e("Debug",Integer.toString(peaks));
//            heartBeat = (int)(peaks * 60 / (markData.size() /RATE));
            smoothedData = smooth(markData, markData.size(), 15);
               Log.d("Green signal value 2: ",Integer.toString(smoothedData.size()));
            heartBeat = (int) (findPeaks(smoothedData) *60/(smoothedData.size()/RATE));
        }

        markData.offer(mean.val[0]);
    }

    private void temporalIdealFilter() {
        Core.split(bufferMat, bufferChannels);
        for(Mat channel: bufferChannels){
            Core.dft(channel, DFTBufferChannel, Core.DFT_ROWS, 0);

            // ideal temporal filter
            DFTBufferChannel.colRange(0, LOW_FREQUENCY_FRAME).setTo(ZERO);
            DFTBufferChannel.colRange(HIGH_FREQUENCY_FRAME,BUFFER_SIZE).setTo(ZERO);

            Core.idft(DFTBufferChannel,IDFTBufferChannel,Core.DFT_ROWS + Core.DFT_SCALE, 0);

            IDFTBufferChannel.copyTo(channel);
        }

        Core.merge(bufferChannels, filteredBufferMat);
    }

    private void getLastFromBuffer(Mat filteredBufferMat, Mat blurredImage) {
        int rows = blurredImage.rows();
        int columns = blurredImage.cols();


        for(int j = 0; j < rows; j++){
            for (int i = 0; i< columns; i++){
                blurredImage.put(j, i, filteredBufferMat.get(j * columns + i, BUFFER_SIZE - 1));

            }
        }
    }

    private void concat(Mat blurredImage, Mat bufferMat, int counter) {
        int rows = blurredImage.rows();
        int columns = blurredImage.cols();

        for(int j = 0; j < rows; j++){
            for (int i = 0; i< columns; i++){
                bufferMat.put(j * columns + i, counter, blurredImage.get(j,i));
            }
        }
    }

    public void init() {
        blurredImage = new Mat();
        outputImage = new Mat();
        outputFloat = new Mat();
        frameFloat = new Mat();
        bufferMat = new Mat();
        filteredBufferMat = new Mat();
        DFTBufferChannel = new Mat();
        IDFTBufferChannel = new Mat();
        faceROI = new Mat();
        isSignalBufferFull = false;


        bufferChannels = new ArrayList<Mat>();
        cl = new ArrayList<>(3);

        markData = new LinkedList<Double>();

        counter = 0;

    }

    public void reset(){

        blurredImage.release();
        outputImage.release();
        outputFloat.release();
        frameFloat.release();
        bufferMat.release();
        DFTBufferChannel.release();
        IDFTBufferChannel.release();
        filteredBufferMat.release();
        faceROI.release();

        bufferChannels.clear();
        cl.clear();
        sign.clear();
//        signals.clear();
        markData.clear();
        heartBeat = 0;
        counter = 0;
        isSignalBufferFull = false;
    }

    public int getHeartRate(){
        return heartBeat;
    }


    /**
     * smooth   - 	smooth the curve with Moving Average Filtering
     *
     */
    private LinkedList<Double> smooth(LinkedList<Double> inputData, int len, int span) {
        int i, j;
        int pn, n;
        double sum = 0.0;
        LinkedList<Double> outputData = new LinkedList<>();
        if (span % 2 == 1) {
            n = (span - 1) / 2;
        }
        else{
            n = (span - 2) / 2;
        }

        for (i = 0; i < len; ++i) {
            pn = n;

            if (i < n) {
                pn = i;
            }
            else if ((len - 1 - i) < n) {
                pn = len - i - 1;
            }

            sum = 0.0;
            for (j = i - pn; j <= i + pn; ++j) {
                sum += inputData.get(j);
            }
            outputData.add(sum / (pn * 2 + 1));
        }

        return outputData;
    }

    /**
     * findPeaks	-	find peaks in the curve
     *
     */
    private int findPeaks(List<Double> smoothedData) {
        double difference;
        sign = new ArrayList<>();
        for (int i = 1; i < smoothedData.size(); ++i) {
            if (smoothedData.get(i) > 150){
                smoothedData.set(i, 150.0);
            }
            difference =  smoothedData.get(i) - smoothedData.get(i-1);
            if (difference > 0) {
                sign.add(1);
            }
            else if (difference < 0) {
                sign.add(-1);
            }
            else {
                sign.add(0);
            }
        }
        int peaks = 0;
        for (int j = 1; j < sign.size(); j++) {
            difference = sign.get(j) - sign.get(j-1);
            if (difference < 0) {
                peaks++;
            }
        }
        return peaks;
    }

    public HashMap<String, List> analyzeDataForSignals(List<Double> data, int lag, Double threshold, Double influence) {

        // init stats instance
        SummaryStatistics stats = new SummaryStatistics();

        // the results (peaks, 1 or -1) of our algorithm
        List<Integer> signals = new ArrayList<Integer>(Collections.nCopies(data.size(), 0));

        // filter out the signals (peaks) from our original list (using influence arg)
        List<Double> filteredData = new ArrayList<Double>(data);

        // the current average of the rolling window
        List<Double> avgFilter = new ArrayList<Double>(Collections.nCopies(data.size(), 0.0d));

        // the current standard deviation of the rolling window
        List<Double> stdFilter = new ArrayList<Double>(Collections.nCopies(data.size(), 0.0d));

        // init avgFilter and stdFilter
        for (int i = 0; i < lag; i++) {
            stats.addValue(data.get(i));
        }
        avgFilter.set(lag - 1, stats.getMean());
        stdFilter.set(lag - 1, Math.sqrt(stats.getPopulationVariance())); // getStandardDeviation() uses sample variance
        stats.clear();

        // loop input starting at end of rolling window
        for (int i = lag; i < data.size(); i++) {

            // if the distance between the current value and average is enough standard deviations (threshold) away
            if (Math.abs((data.get(i) - avgFilter.get(i - 1))) > threshold * stdFilter.get(i - 1)) {

                // this is a signal (i.e. peak), determine if it is a positive or negative signal
                if (data.get(i) > avgFilter.get(i - 1)) {
                    signals.set(i, 1);
                } else {
                    signals.set(i, -1);
                }

                // filter this signal out using influence
                filteredData.set(i, (influence * data.get(i)) + ((1 - influence) * filteredData.get(i - 1)));
            } else {
                // ensure this signal remains a zero
                signals.set(i, 0);
                // ensure this value is not filtered
                filteredData.set(i, data.get(i));
            }

            // update rolling average and deviation
            for (int j = i - lag; j < i; j++) {
                stats.addValue(filteredData.get(j));
            }
            avgFilter.set(i, stats.getMean());
            stdFilter.set(i, Math.sqrt(stats.getPopulationVariance()));
            stats.clear();
        }

        HashMap<String, List> returnMap = new HashMap<String, List>();
        returnMap.put("signals", signals);
        returnMap.put("filteredData", filteredData);
        returnMap.put("avgFilter", avgFilter);
        returnMap.put("stdFilter", stdFilter);

        return returnMap;

    }

    private void drawSignal(Mat frame, List<Integer> signals){
        int x = 20;
        int y = 200;
        for (Integer signal: signals){

            if (signal == 1){
                x += 2;
                y += 2;
                Imgproc.circle(frame,new Point(x,y),1,new Scalar(255, 0,0),1);

            } else if(signal== -1){
                x += 2;
                y -= 2;
                Imgproc.circle(frame,new Point(x,y),1,new Scalar(255, 0,0),1);
            } else{
                Imgproc.circle(frame,new Point(x,y),1,new Scalar(255, 0,0),1);
            }
        }
    }

    private void drawPulse(List<Double> markData, Mat frame){


        int x = 20;
        String data = "Smoothed Green value: ";
        for (int i = 1; i < markData.size(); ++i) {
            x +=1;
            data = data +", " + Double.toString(markData.get(i));
            int y = (int) (300 - markData.get(i));
            Imgproc.circle(frame,new Point(x,y),1,new Scalar(255, 0,0),1);
        }
        Log.e("Data", data);
    }

    public void setScreenShot(boolean takeScreenshot){
        this.screenshot = takeScreenshot;
    }

    private void takeScreenshot(Mat image){
        Bitmap bitmap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image,bitmap);
        imageIO.saveBitmap(bitmap);
    }

    @Override
    public void run() {

    }
}
