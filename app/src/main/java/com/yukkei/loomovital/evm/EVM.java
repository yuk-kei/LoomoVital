package com.yukkei.loomovital.evm;

import static org.opencv.core.CvType.CV_32FC3;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class EVM {

    private static final double MIN_FACE_SIZE = 0.3;
    private static final int PYRAMIDS_LEVEL = 4;
    private static final double F_LOW = 50 / 60. / 10;
    private static final double F_HIGH = 60 / 60. / 10;
    private static final String spatialType = "GAUSSIAN";
    private static final Scalar ALPHA = Scalar.all(100);

    private int frameRate;
    private int frameLength;
    private List<Mat> frames;
//    private static final Scalar ALPHA           = Scalar.all(200);

    public EVM(List<Mat> frames, int frameRate) {
        this.frameRate = frameRate;
        this.frames = frames;
        this.frameLength = frames.size();

    }
    /*
     * Change an RGB color to YIQ color. The colour value conversion is
     * independent of the colour range. Colours could be 0-1 or 0-255.
     * @param rgb The array of RGB components to convert
     * @param yiq An array to return the colour values with
     */

    public static Mat rgb2ntsc(Mat origin) {
        Mat result = origin.clone();

        int channels = origin.channels();
        int blue, green, red, q, in, y;
        for (int j = 0; j < origin.height(); j++) {
            for (int i = 0; i < origin.width(); i++) {
                byte[] temp = new byte[channels];
                origin.get(j, i, temp);
                blue = temp[0] & 0xff;
                green = temp[1] & 0xff;
                red = temp[2] & 0xff;

                // 修改像素点
                q = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                in = (int) (0.596 * red - 0.274 * green - 0.322 * blue);
                y = (int) (-0.299 * red + 0.587 * green + 0.114 * blue);

                // 写入
                temp[0] = (byte) q;
                temp[1] = (byte) in;
                temp[2] = (byte) y;
                result.put(j, i, temp);
            }
        }
        return result;
    }

    public static Mat ntsc2rgb(Mat origin) {
        Mat result = origin.clone();

        int channels = origin.channels();
        int blue, green, red, q, in, y;
        for (int j = 0; j < origin.height(); j++) {
            for (int i = 0; i < origin.width(); i++) {
                byte[] temp = new byte[channels];
                origin.get(j, i, temp);
                q = temp[0] & 0xff;
                in = temp[1] & 0xff;
                y = temp[2] & 0xff;

                // 修改像素点
                blue = (int) (0.299 * y + 0.587 * in + 0.114 * q);
                green = (int) (0.596 * y - 0.274 * in - 0.322 * q);
                red = (int) (-0.299 * y + 0.587 * in + 0.114 * q);

                // 写入
                temp[0] = (byte) blue;
                temp[1] = (byte) green;
                temp[2] = (byte) red;
                result.put(j, i, temp);
            }
        }
        return result;
    }


    /**
     * spatialFilter	-	spatial filtering an image
     *
     * @param src -	source image
     */
    public List<Mat> spatialFilter(Mat src) {
        switch (spatialType) {
            case "LAPLACIAN":     // laplacian pyramid
                return buildLaplacianPyramid(src, PYRAMIDS_LEVEL);
            case "GAUSSIAN":      // gaussian pyramid
                return buildGaussianPyramid(src, PYRAMIDS_LEVEL);
            default:
                return null;
        }
    }

    /**
     * buildGaussianPyramid	-	construct a gaussian pyramid from a given image
     *
     * @param img    -	source image
     * @param levels -	levels of the destinate pyramids
     * @return true if success
     */
    public List<Mat> buildGaussianPyramid(Mat img, int levels) {
        Mat source = img.clone();
        List<Mat> pyramid = new ArrayList<>();
        pyramid.add(source);

        for (int i = 0; i < levels; i++) {
            Imgproc.pyrDown(source, source);
            pyramid.add(source);
        }

        return pyramid;
    }


    /**
     * buildLaplacianPyramid	-	construct a laplacian pyramid from given image
     *
     * @param img    -	source image
     * @param levels -	levels of the destinate pyramids
     * @return true if success
     */
    public List<Mat> buildLaplacianPyramid(Mat img, int levels) {
        List<Mat> gaussianPyramid = buildGaussianPyramid(img, levels);
        List<Mat> pyramid = new ArrayList<>();

        for (int i = levels - 1; i > 0; i--) {
            Mat currentImg = new Mat();
            Mat lap = new Mat();
            Imgproc.pyrUp(gaussianPyramid.get(i), currentImg);
            Core.subtract(gaussianPyramid.get(i - 1), currentImg, lap);
            pyramid.add(lap);
        }

        return pyramid;
    }

    /**
     * upsamplingFromGaussianPyramid	-	up-sampling an image from gaussian pyramid
     *
     * @param source		-	source image
     * @param levels	-	levels of the pyramid
     */
    private Mat upsamplingFromGaussianPyramid(Mat source,int levels) {
        Mat currentLevel = source.clone();
        for (int i = 0; i < levels; ++i) {
            Mat up = new Mat();
            Imgproc.pyrUp(currentLevel,up);
            currentLevel = up;
        }

        return currentLevel;
    }

    public Mat amplify(Mat source) {
        float currAlpha;
        switch (spatialType) {

            case "GAUSSIAN":
                Core.multiply(source, ALPHA, source);
                return source;
            default:
                return null;
        }

    }

    public Mat temporalIdealFilter(Mat source){
        List<Mat> channels = new ArrayList<>(3);
        Core.split(source,channels);
        Mat result = new Mat();

        for (int i=0; i < 2; i++) {
            Mat current = channels.get(i);
            Mat tempImg = new Mat();
            int width = Core.getOptimalDFTSize(current.cols());
            int height = Core.getOptimalDFTSize(current.rows());

            Core.copyMakeBorder(current, tempImg, 0, height - current.rows(), 0, width - current.cols(), Core.BORDER_CONSTANT, Scalar.all(0));

            Core.dft(tempImg, tempImg, Core.DFT_ROWS | Core.DFT_SCALE);

            Mat filter = tempImg.clone();


            createIdealBandpassFilter(filter, frameRate);

            Core.mulSpectrums(tempImg, filter, tempImg, Core.DFT_ROWS);

            Core.idft(tempImg, tempImg, Core.DFT_ROWS | Core.DFT_SCALE);
            Mat temp= new Mat(tempImg,new Rect(0, 0, current.cols(), current.rows()));

            channels.set(i,temp);
        }

        Core.merge(channels,result);
        Core.normalize(result,result,0,1,Core.NORM_MINMAX);

        return  result;

    }

    private void createIdealBandpassFilter(Mat filter, int frameRate) {
        int width = filter.cols();
        int height = filter.rows();

        double fl = 2 * F_LOW * width / frameRate;
        double fh = 2 * F_HIGH * width / frameRate;


        double response;
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                // filter response
                if (j >= fl && j <= fh)
                    response = 1.0;
                else
                    response = 0.0;

                filter.put(i,j,response);

            }
        }


    }


    /**
     * concat	-	concat all the frames into a single large Mat
     *              where each column is a reshaped single frame
     *
     * @param frames	-	frames of the video sequence
     */
    private Mat concat(List<Mat>  frames)
    {
        Size frameSize = frames.get(0).size();
        Mat temp = new Mat((int)(frameSize.width*frameSize.height),frames.size()- 1,CvType.CV_32F);
        for (int i = 0; i < frames.size() - 1; ++i) {
            // get a frame if any
            Mat input = frames.get(i);
            // reshape the frame into one column
            // 像素总数不变，但row变成总数，意味着column为1

            Mat reshaped = input.reshape(3, input.cols()*input.rows()).clone();
            Mat line = temp.col(i);
            // save the reshaped frame to one column of the destinate big image
            input.copyTo(line);
            reshaped.copyTo(line);
        }
        return temp;

    }

    /**
     * deConcat	-	de-concat the concatnate image into frames
     *
     * @param source       -   source concatnate image
     * @param frameSize	-	frame size

     */
    private List<Mat> deConcat(Mat source,Size frameSize,int frameLength) {
        List<Mat> result = new ArrayList<>();
        for (int i = 0; i < frameLength - 1; ++i) {    // get a line if any
            Mat line = source.col(i).clone();
            Mat reshaped = line.reshape(3, (int) frameSize.height).clone();

            result.add(reshaped);
        }
        return result;
    }


    /**
     * findPeaks	-	find peaks in the curve
     *
     */
    private int findPeaks(List<Double> smoothedData) {
        double difference;
        List<Integer>sign = new ArrayList<>();
        for (int i = 1; i < smoothedData.size(); ++i) {
            difference = smoothedData.get(i) - smoothedData.get(i-1);
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

    /**
     * smooth   - 	smooth the curve with Moving Average Filtering
     *
     */
    private List<Double> smooth(List<Double> inputData, int len, int span) {
        int i = 0, j = 0;
        int pn = 0, n = 0;
        double sum = 0.0;
        List<Double> outputData = new ArrayList<>();
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
                sum += inputData.get(i);
            }
            outputData.add(sum / (pn * 2 + 1));
        }

        return outputData;
    }


    /**
     * colorMagnify	-	color magnification
     *
     * @return
     */
    public int colorMagnify() {



        // motion image
        Mat output;
        // down-sampled frames
        List<Mat> downSampledFrames = new ArrayList<>();
        // filtered frames
        List<Mat> filteredFrames = new ArrayList<>();

        // concatenate image of all the down-sample frames
        // concatenate filtered image
        Mat filtered;
        // 1. spatial filtering
        for(Mat temp: frames){
            List<Mat> pyramid = spatialFilter(temp);
            downSampledFrames.add(pyramid.get(PYRAMIDS_LEVEL-1));
        }


        for (Mat frame : downSampledFrames){
            filtered = temporalIdealFilter(frame);
            filtered = amplify(filtered);
            filteredFrames.add(filtered);
        }
//        // 2. concat all the frames into a single large Mat
//        // where each column is a reshaped single frame
//        // (for processing convenience)
//        Mat videoMat = concat(downSampledFrames);
//
//        // 3. temporal filtering
//        filtered = temporalIdealFilter(videoMat);
//
//        // 4. amplify color motion
//        filtered =amplify(filtered);
//
//        // 5. de-concat the filtered image into filtered frames
//        filteredFrames = deConcat(filtered, downSampledFrames.get(0).size(), frameLength);

        List<Mat> cl = new ArrayList<>(3);
        List<Double> markData = new ArrayList<>();

        // 6. amplify each frame
        // by adding frame image and motions
        // and write into video
        int fnumber = 0;
        for (int i = 0; i < frameLength- 1 ; ++i) {
            // up-sample the motion image
            output = upsamplingFromGaussianPyramid(filteredFrames.get(i), PYRAMIDS_LEVEL);
            Imgproc.resize(output,output,frames.get(i).size());
            Core.add(frames.get(i),output,output);


            double minVal = Core.minMaxLoc(output).minVal;
            double maxVal = Core.minMaxLoc(output).maxVal;

            output.convertTo(output, CvType.CV_8UC3, 255.0 / (maxVal - minVal), -minVal * 255.0 / (maxVal - minVal));

            Core.split(output,cl);
            Scalar mean = Core.mean(cl.get(1));
            markData.add(mean.val[0]);
        }

        // Smooth the curve
        List<Double> smoothedData = new ArrayList<>();
        smoothedData = smooth(markData, markData.size(), 11);

        // Find Peaks
        return findPeaks(smoothedData);
    }

}
