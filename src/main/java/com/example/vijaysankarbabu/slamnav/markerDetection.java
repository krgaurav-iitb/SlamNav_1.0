package com.example.vijaysankarbabu.slamnav;

import android.renderscript.Double3;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Scalar;
import org.opencv.calib3d.Calib3d;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.aruco.Aruco.DICT_4X4_50;


public class markerDetection extends MainActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "markerDetection";
    JavaCameraView camMarkerDetection;
    private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients= new Mat();
    DecimalFormat df = new DecimalFormat("#.###");
    Scalar colorRed = new Scalar(255, 255, 0);
    private boolean CALIB_DONE= false;

    Mat mrgba, mOutput;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    camMarkerDetection.enableView();
                    break;
                }
                default:
                {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_marker_detection);

        // Initialising camera and dist matrix
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);

        // Trying to load camera and dist matrix
        if(CalibrationResult.tryLoad(this, mCameraMatrix, mDistortionCoefficients)){
            CALIB_DONE=true;
        }else {
            TextView tv1= (TextView) findViewById(R.id.cameraCoordinatesTextMarkerDetection);
            tv1.setText("CALIBRATION NOT DONE");
            CALIB_DONE=false;
        }

        // Initialising camera view ( done even when Calibration results are not loaded )
        camMarkerDetection= (JavaCameraView) findViewById(R.id.markerDetectionCameraView);
        camMarkerDetection.setVisibility(SurfaceView.VISIBLE);
        camMarkerDetection.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(camMarkerDetection!=null)
            camMarkerDetection.disableView();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(camMarkerDetection!=null)
            camMarkerDetection.disableView();
    }
    @Override
    protected void onResume(){
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV not working Marker detection activity");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        } else {
            Log.i(TAG, "OpenCV working Marker detection activity");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public static void debugMatrix(String s,Mat m){
        //this method is just for debegging. I have difficulty viewing MAT
        Log.i(TAG, "debugMatrix: "+s+"\n"+m.dump());
    }

    private Mat getCoordinates(int len, Mat rvecs, Mat tvecs){
        Mat result= new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(result);
        Mat rotMatrix= new Mat();
        Mat rvec = new Mat();
        Mat tvec= new Mat();
        Mat tempResult= new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(rvec);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tvec);
        Mat.zeros(3,1,CvType.CV_64F).copyTo(tempResult);
        //TODO: 20-06-2018 "change 1 below to len";
        len=1;
        for(int i=0;i<len;i++){
            rvec.put(0,0, rvecs.get(i,0)[0]);
            rvec.put(1,0, rvecs.get(i,0)[1]);
            rvec.put(2,0, rvecs.get(i,0)[2]);
            tvec.put(0,0, tvecs.get(i,0)[0]);
            tvec.put(1,0, tvecs.get(i,0)[1]);
            tvec.put(2,0, tvecs.get(i,0)[2]);

            // Camera coordinates can be obtained using 'camera position = - rot_matrix.inverse() * tvec'
            // Rotation matrix can be obtained by Rodrigues function
            Calib3d.Rodrigues(rvec,rotMatrix);
            //rotMatrix= rotMatrix.t();
            rotMatrix = rotMatrix.inv();
            Core.multiply(rotMatrix,Scalar.all(-1),rotMatrix);
            Core.gemm(rotMatrix,tvec, 1, tvec, 0, tempResult, 0);
            Core.add(result,tempResult,result);
            //debugMatrix("result",result);
        }
        Core.divide(result, Scalar.all(len),result);
        return result;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mrgba = inputFrame.rgba();
        Mat ids = new Mat();
        List<Mat> corners = new ArrayList<>();
        Mat rvecs= new Mat();
        Mat tvecs= new Mat();
        Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250);
        Aruco.detectMarkers(inputFrame.gray(), dictionary, corners, ids); // Finding corners and ids from image frame
        if(!ids.empty()) {
            Aruco.drawDetectedMarkers(inputFrame.gray(), corners, ids, colorRed); // Drawing the ID and the Rectangle for each marker
            if(CALIB_DONE) {
                // Estimating the rvec and tvec from corners (this function uses solvepnp of OpenCV)
                Aruco.estimatePoseSingleMarkers(corners, 6.3f, mCameraMatrix, mDistortionCoefficients, rvecs, tvecs);
                ////Aruco.drawAxis(inputFrame.rgba(), mCameraMatrix, mDistortionCoefficients, rvecs, tvecs, 0.1f);
                Mat coordinates = getCoordinates((int) (ids.size().height), rvecs, tvecs);
                TextView coordinatesText = findViewById(R.id.cameraCoordinatesTextMarkerDetection);
                String coordText = "(" + df.format(coordinates.get(0, 0)[0]) + "," + df.format(coordinates.get(1, 0)[0]) + "," + df.format(coordinates.get(2, 0)[0]) + ")";
                coordinatesText.setText(coordText);
            }
        }
        return inputFrame.rgba();
    }
    @Override
    public void onCameraViewStarted(int width, int height){
        mrgba = new Mat(height,width, CvType.CV_8UC4);
        mOutput = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped(){
        mrgba.release();
    }
    //public native void detectMarker(Mat input, Dictionary dict, List<Mat> corners, Mat ids);
}
