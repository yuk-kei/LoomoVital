package com.yukkei.loomovital.tools;

import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class ImageIO{
    private String TAG = "Image IO";

    /* fields for IO operation */
    private int index;
    private String colorPath = "/color/";
    private String depthPath = "/depth/";
    private String filterPath = "/filterDepth/";
    private String pgmPath = "/pgmDepth/";
    private String posFilePath = "/pose.txt";


    /* -------------------------------  Storage path generate start ----------------------------  *
     *
     *  This part has two method:
     *      1. initDir(): called once to generate the dir to store file
     *
     *
     * -----------------------------------------------------------------------------------------  */
    /**
     * create dir to store images
     * Run once
     */
    private void initDir(){
        /* set up root dir  */
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR) + 1;
        int min = calendar.get(Calendar.MINUTE);

        String prefix = "0" +String.valueOf(month) + "_" + String.valueOf(day) + "_HR" + String.valueOf(hour) + "_" + String.valueOf(min);
        prefix = Environment.getExternalStorageDirectory().getPath() + "/Capture/" + prefix;

        /* --------------- set up the dir to store each picture ---------------------- */
        colorPath = prefix + colorPath;
        depthPath = prefix + depthPath;
        filterPath = prefix + filterPath;
        pgmPath = prefix + pgmPath;
        posFilePath = prefix + posFilePath;

        File cDir = new File(colorPath);
        File dDir = new File(depthPath);
        File fDir = new File(filterPath);
        File pDir = new File(pgmPath);

        if(!cDir.exists()) cDir.mkdirs();
        if(!dDir.exists()) dDir.mkdirs();
        if(!fDir.exists()) fDir.mkdirs();
        if(!pDir.exists()) pDir.mkdirs();
    }

    /* ======================= Storage path generate Finish  ========================== */


    /* ----------------------------- Image Handler Start  ---------------------------------------------  *
     *  This part has one field:
     *      1. realsenseHandler: used to inform realsense of save is complete
     *
     * ------------------------------------------------------------------------------------------------  */
    /**
     * This handler is used to inform realsense of save is complete
     */
    private Handler realsenseHandler;
    public void setSenseHandler(Handler handler){
        this.realsenseHandler = handler;
    }


    /**
     * Save frame as a bitmap
     * @param bitmap target bitmap need to be saved
     */
    public void saveBitmap(Bitmap bitmap){
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.US);
        String path = Environment.getExternalStorageDirectory().getPath() + "/Capture/" + format.format(new Date())+ ".png";
        try {
            File colorFile = new File(path);
            FileOutputStream cOut = new FileOutputStream(colorFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, cOut);

            cOut.flush();
            cOut.close();
        }catch (Exception exception){
            exception.printStackTrace();
            Log.e(TAG, "saveBitmap: ERROR");
        }
    }

    /**
     * Method to resize map to a satisfied scale
     * @param input
     * @param targetWidth
     * @param targetHeight
     * @return
     */
    private Bitmap resize(Bitmap input, float targetWidth, float targetHeight){
        float scaleW = targetWidth / input.getWidth();
        float scaleH = targetHeight / input.getHeight();

        Matrix scaleMat = new Matrix();
        scaleMat.postScale(scaleW, scaleH);
        return Bitmap.createBitmap(input, 0, 0, input.getWidth(), input.getHeight(), scaleMat, true);
    }
}
