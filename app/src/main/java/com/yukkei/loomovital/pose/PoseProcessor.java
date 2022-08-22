package com.yukkei.loomovital.pose;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

public class PoseProcessor {
    Pose pose;
    PoseLandmark nose;
    PoseLandmark leftEyeInner;
    PoseLandmark leftEye;
    PoseLandmark leftEyeOuter;
    PoseLandmark rightEyeInner;
    PoseLandmark rightEye;
    PoseLandmark rightEyeOuter;
    PoseLandmark leftEar;
    PoseLandmark rightEar;
    PoseLandmark leftMouth;
    PoseLandmark rightMouth;

    PoseLandmark leftShoulder;
    PoseLandmark rightShoulder;
    PoseLandmark leftElbow;
    PoseLandmark rightElbow;
    PoseLandmark leftWrist;
    PoseLandmark rightWrist;
    PoseLandmark leftHip;
    PoseLandmark rightHip;
    PoseLandmark leftKnee;
    PoseLandmark rightKnee;
    PoseLandmark leftAnkle;
    PoseLandmark rightAnkle;

    PoseLandmark leftPinky;
    PoseLandmark rightPinky;
    PoseLandmark leftIndex;
    PoseLandmark rightIndex;
    PoseLandmark leftThumb;
    PoseLandmark rightThumb;
    PoseLandmark leftHeel;
    PoseLandmark rightHeel;
    PoseLandmark leftFootIndex;
    PoseLandmark rightFootIndex;

    List<PoseLandmark> landmarks;

    public PoseProcessor(Pose pose){
        this.pose = pose;

        landmarks = pose.getAllPoseLandmarks();

        nose = pose.getPoseLandmark(PoseLandmark.NOSE);
        leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER);
        leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE);
        leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER);
        rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER);
        rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE);
        rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER);
        leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR);
        rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR);
        leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH);
        rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH);

        leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
        rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW);
        rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST);
        rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP);
        rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);
        leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE);
        rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE);
        leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE);
        rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE);

        leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY);
        rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY);
        leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX);
        rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX);
        leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB);
        rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB);
        leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL);
        rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL);
        leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX);
        rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX);
    }

    public boolean isStanding(){
        return (isHigherPosition(leftShoulder,rightHip) && isHigherPosition(rightShoulder, leftHip) && !isSeating());
    }

    public boolean isSeating(){
        return (isSimilarY(leftKnee, leftHip) && isSimilarY(rightKnee,rightHip)) ||isSeatingOnGround();
    }

    public boolean isSeatingOnGround(){
        return isHigherPosition(leftShoulder,rightHip) && isHigherPosition(rightShoulder,leftHip)
                && isHigherPosition(leftKnee,rightHip) && isHigherPosition(rightKnee,rightHip);
    }

    public boolean isLying() {
        return isSimilarY(leftShoulder,leftAnkle)|| isSimilarY(rightShoulder,rightAnkle);
    }

    public boolean isSimilarY(PoseLandmark startLandmark, PoseLandmark endLandmark){
        PointF3D start = startLandmark.getPosition3D();
        PointF3D end = endLandmark.getPosition3D();

        return Math.abs(start.getY() - end.getY()) < 30;
    }

    public boolean isHigherPosition (PoseLandmark startLandmark, PoseLandmark endLandmark){
        PointF3D start = startLandmark.getPosition3D();
        PointF3D end = endLandmark.getPosition3D();

        return start.getY() - end.getY() < 0;
    }
}
