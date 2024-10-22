package com.example.vijaysankarbabu.slamnav;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.aruco.Aruco;
import org.opencv.aruco.Dictionary;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point;
import org.opencv.core.Point3;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.calib3d.Calib3d.CV_P3P;
import static org.opencv.core.CvType.CV_64FC;
import static org.opencv.core.CvType.CV_64FC3;

public class slamCameraTabFragment extends Fragment implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "slamCameraFragment";
    private static final int CAM_INIT_COUNT = 5;
    private static final int MAX_MARKER_ID = 40;
    private final Size mImageSize = new Size(1280 , 720);
    private  float x_fixed;
    private  float y_fixed;
    private  float z_fixed;
    public  int count=0;
    private static final boolean MISMATCH_LANDMARK = false; // set it to true to enable mixpoints

    JavaCameraView camSlam;
    private Mat mCameraMatrix = new Mat();
    private Mat mDistortionCoefficients = new Mat();
    //private Mat result1 = new Mat();
    DecimalFormat df = new DecimalFormat("#.###");
    Scalar colorRed = new Scalar(255, 255, 0);
    private boolean CALIB_DONE = false;
    View rootView;
    Mat mrgba, mOutput;

    public static Map<Integer, float[]> landmarkPoints;
    public static Map<Integer, float[]> setLandmarkPoints;

    public static Map<Integer, Integer> setCount= null;
    int setLandmarkPointsCount=0;
    public static Map<Integer, float[]> cameraPoints= null;
    int countcamera=0;
    private boolean camTrackInitFlag = false;
    private int camTrackInitCount[] = null;
    public static String CoordinateDump="";

    MatOfPoint2f imagepointsF0 = new MatOfPoint2f();
    MatOfPoint2f imagepointsTemp = new MatOfPoint2f();
    Mat rvecs_frame0 = new Mat();
    Mat tvecs_frame0 = new Mat();


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    camSlam.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_basic_slam_tab_camera, container, false);

        //check for calibration
        Mat.eye(3, 3, CvType.CV_64FC1).copyTo(mCameraMatrix);
        mCameraMatrix.put(0, 0, 1.0);
        Mat.zeros(5, 1, CvType.CV_64FC1).copyTo(mDistortionCoefficients);
        if (CalibrationResult.tryLoad(getActivity(), mCameraMatrix, mDistortionCoefficients)) {
            CALIB_DONE = true;
        } else {
            TextView tv1 = (TextView) rootView.findViewById(R.id.cameraCoordinatesTextSlam);
            tv1.setText("CALIBRATION NOT DONE");
            CALIB_DONE = false;
        }

        //initialising variables
        landmarkPoints = basicSlam.knownLandmarkPoints;
        setLandmarkPoints=new HashMap<>(basicSlam.knownLandmarkPoints);
        //Log.i(TAG, "oncreate: setLandmarkPoints" + setLandmarkPoints.toString());
        cameraPoints= new HashMap<>();
        setCount=new HashMap<>();
        camTrackInitCount = new int[MAX_MARKER_ID]; //MAX_MARKER_ID refers to number of markers used, we need a count for each marker;
        camTrackInitFlag = false;

        //intialising Camera
        camSlam = (JavaCameraView) rootView.findViewById(R.id.cameraViewBasicSlam);
        camSlam.setVisibility(SurfaceView.VISIBLE);
        camSlam.setCvCameraViewListener(this);
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (camSlam != null)
            camSlam.disableView();
    }

    @Override
    public void onDestroy() {
        //File file = new File("/storage/emulated/0/Download/myfile.txt");
        //printtoTextFile(result1, file);
        /*Log.i(TAG, "finallyDumped" + basicSlam.landmarkDump);
        File file1 = new File("/storage/emulated/0/Download/knownlandmarks.txt");
        printToTextFileKnownLandmarks(basicSlam.landmarkDump, file1);
        File file2 = new File("/storage/emulated/0/Download/myfile2.txt");
        printToTextFileKnownLandmarks(CoordinateDump, file2);*/
        super.onDestroy();
        if (camSlam != null)
            camSlam.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV not working Marker detection activity");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, getActivity(), mLoaderCallback);
        } else {
            Log.i(TAG, "OpenCV working Marker detection activity");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mrgba = new Mat(height, width, CvType.CV_8UC4);
        mOutput = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mrgba.release();
        mOutput.release();
    }

    private Mat getWorldCoordinatesOfCam(int id, Mat rvec, Mat tvec) {
        Mat result = new Mat();
        Mat worldCoordinates = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(worldCoordinates);
        float[] worldCoord = landmarkPoints.get(id);
        //converting float [] to Mat; this can be eliminated by saving it in MAT in basicSlam.java
        worldCoordinates.put(0, 0, worldCoord[0]);
        worldCoordinates.put(1, 0, worldCoord[1]);
        worldCoordinates.put(2, 0, worldCoord[2]);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(result);
        Mat rotMatrix = new Mat();
        Calib3d.Rodrigues(rvec, rotMatrix);
        markerDetection.debugMatrix("worldC", worldCoordinates);
        markerDetection.debugMatrix("tvec", tvec);
        markerDetection.debugMatrix("rvec", rvec);
        //rotMatrix= rotMatrix.inv();
        markerDetection.debugMatrix("rotMatrix", rotMatrix);
        //Core.subtract(worldCoordinates,tvec,tvec);
        //markerDetection.debugMatrix("tvec",tvec);
        //Core.multiply(rotMatrix, Scalar.all(-1),rotMatrix);
        Core.gemm(rotMatrix, worldCoordinates, 1, worldCoordinates, 0, worldCoordinates, 0);
        markerDetection.debugMatrix("worldC", worldCoordinates);
        Core.add(worldCoordinates, tvec, result);
        markerDetection.debugMatrix("result", result);
        Log.i(TAG, "getWorldCoordinatesOfCam: coordinates found using id " + Integer.toString(id) + " are "
                + Double.toString(result.get(0, 0)[0]) + ","
                + Double.toString(result.get(1, 0)[0]) + ","
                + Double.toString(result.get(2, 0)[0]));
        return result;
    }

    private Mat estimateCameraCoordinates(Mat camCoordinates) {
        //This function is currently just averaging the obtained coordinates.Can later be modified
        Mat result = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(result);
        double xSum, ySum, zSum;
        xSum = ySum = zSum = 0;
        for (int i = 0; i < camCoordinates.size().height; i++) {
            xSum += camCoordinates.get(i, 0)[0];
            ySum += camCoordinates.get(i, 1)[0];
            zSum += camCoordinates.get(i, 2)[0];
        }
        result.put(0, 0, xSum / camCoordinates.size().height);
        result.put(1, 0, ySum / camCoordinates.size().height);
        result.put(2, 0, zSum / camCoordinates.size().height);
        // File file = new File("/storage/emulated/0/Download/myfile.txt");
        //printtoTextFile(result, file);
        //FileOutputStream out = new FileOutputStream(file);
// Write your data
       /* DataOutputStream dos = new DataOutputStream(out);
        result.get(0,0);
        result.get(1,0);
        result.get(2,0);
        result.get(3,0);
        out.close();*/
        return result;
    }


    // TODO: 21-06-2018 Change the below algorithm
    private Mat getCameraCoordinates(Mat ids, Mat rvecs, Mat tvecs) {
        Mat result = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(result);
        Mat rvec = new Mat();
        Mat tvec = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(rvec);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tvec);
        Mat camCoordiantes = new Mat();
        int foundKnownMarkersCount = 0;
        for (int i = 0; i < ids.size().height; i++) {
            int id = (int) ids.get(i, 0)[0];
            if (landmarkPoints.get(id) != null) {
                rvec.put(0, 0, rvecs.get(i, 0)[0]);
                rvec.put(1, 0, rvecs.get(i, 0)[1]);
                rvec.put(2, 0, rvecs.get(i, 0)[2]);
                tvec.put(0, 0, tvecs.get(i, 0)[0]);
                tvec.put(1, 0, tvecs.get(i, 0)[1]);
                tvec.put(2, 0, tvecs.get(i, 0)[2]);
                result = getWorldCoordinatesOfCam(id, rvec, tvec);
                Log.i(TAG, "getCameraCoordinates: Id:" + Integer.toString(id) + ":" + result.dump());
                camCoordiantes.put(foundKnownMarkersCount, 0, result.get(0, 0));
                camCoordiantes.put(foundKnownMarkersCount, 1, result.get(1, 0));
                camCoordiantes.put(foundKnownMarkersCount, 2, result.get(2, 0));
                foundKnownMarkersCount++;
            }
        }
        return estimateCameraCoordinates(camCoordiantes);
    }

    private Mat findCameraCoordinates(int len, Mat rvecs, Mat tvecs) {
        Mat result = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(result);
        Mat rotMatrix = new Mat();
        Mat rvec = new Mat();
        Mat cameraCoord=new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(cameraCoord);
        Mat tvec = new Mat();
        Mat tempResult = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(rvec);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tvec);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tempResult);
        for (int i = 0; i < len; i++) {
            rvec.put(0, 0, rvecs.get(i, 0)[0]);
            rvec.put(1, 0, rvecs.get(i, 0)[1]);
            rvec.put(2, 0, rvecs.get(i, 0)[2]);
            tvec.put(0, 0, tvecs.get(i, 0)[0]);
            tvec.put(1, 0, tvecs.get(i, 0)[1]);
            tvec.put(2, 0, tvecs.get(i, 0)[2]);

            //Camera coordinates can be obtained using 'camera position = - rot_matrix.transpose() * tvec'
            //Here rotation matrix can be obtained by Rodrigues function

            Calib3d.Rodrigues(rvec, rotMatrix);
            //rotMatrix= rotMatrix.t();
            rotMatrix = rotMatrix.inv();
            Core.multiply(rotMatrix, Scalar.all(-1), rotMatrix);
            Core.gemm(rotMatrix, tvec, 1, tvec, 0, tempResult, 0);
            Core.add(result, tempResult, result);
            //debugMatrix("result",result);
        }
        Core.divide(result, Scalar.all(len), result);
        //result1 = result;
        //float[] cameraCoord = new float[3];
        /*cameraCoord.put(0,0,result.get(0, 0)[0]);
        cameraCoord.put(0,0,result.get(1, 0)[0]);
        cameraCoord.put(0,0,result.get(2, 0)[0]);*/
        //cameraPoints.put(countcamera, cameraCoord);
        //countcamera++;
        if(setLandmarkPointsCount==1)
        {
            cameraCoord.put(0,0,result.get(0, 0)[0]);
            cameraCoord.put(1,0,result.get(1, 0)[0]);
            cameraCoord.put(2,0,result.get(2, 0)[0]);
        }
        if(setLandmarkPointsCount==2)
        {
            cameraCoord.put(0,0,x_fixed-result.get(2, 0)[0]);
            cameraCoord.put(1,0,result.get(1, 0)[0]);
            cameraCoord.put(2,0,result.get(0, 0)[0]);
        }
        float[] cameraCoord1 = new float[3];
        cameraCoord1[0] = (float) cameraCoord.get(0, 0)[0];
        cameraCoord1[1] = (float) cameraCoord.get(1, 0)[0];
        cameraCoord1[2] = (float) cameraCoord.get(2, 0)[0];
        cameraPoints.put(countcamera, cameraCoord1);
        countcamera++;
        double[] a = new double[3];
        cameraCoord.get(0, 0, a);//I get byte array here for the whole image
        CoordinateDump+= "\n"+String.format("%.2f",a[0])+","+String.format("%.2f",a[1])+","+String.format("%.2f",a[2]);

        //File file = new File("/storage/emulated/0/Download/myfile.txt");
        //printtoTextFile(result, file);
        //System.out.println("camera coordinates " + result);
        return cameraCoord;
    }

    /**
     * public void printtoTextFile(Mat d, File file_g)  {
     * Size size = d.size();
     * byte [] a = new byte[ ( d.width() * d.height())];
     * d.get(0, 0, a);//I get byte array here for the whole image
     * FileOutputStream fos_g = null;
     * OutputStreamWriter ow = null;
     * BufferedWriter fwriter = null;
     * try {
     * fos_g = new FileOutputStream(file_g);
     * ow = new OutputStreamWriter(fos_g);
     * fwriter = new BufferedWriter(ow);
     * } catch (FileNotFoundException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * }
     * for (int y = 0; y < size.width; y++){
     * for (int x = 0; x < size.height; x++){
     * try {
     * fwriter.write(String.valueOf(a[(int) (y * size.width + x)]));
     * ow.flush();
     * fwriter.write(",");
     * ow.flush();
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * <p>
     * }
     * try {
     * fwriter.write("\n");
     * ow.flush();
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * <p>
     * //fos_g.flush();
     * }
     * <p>
     * try {
     * fos_g.flush();
     * fos_g.close();
     * } catch (IOException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * }
     * <p>
     * }
     **/
    public void printtoTextFile(Mat d, File file_g) {
        Size size = d.size();
        double[] a = new double[(d.width() * d.height())];
        d.get(0, 0, a);//I get byte array here for the whole image
        FileOutputStream fos_g = null;
        DataOutputStream dos = null;
        //OutputStreamWriter ow = null;
        //BufferedWriter fwriter = null;
        try {
            fos_g = new FileOutputStream(file_g);
            dos = new DataOutputStream(fos_g);
            //ow = new OutputStreamWriter(fos_g);
            //fwriter = new BufferedWriter(ow);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for (int y = 0; y < size.width; y++) {
            for (int x = 0; x < size.height; x++) {
                try {
                    double k = (a[(int) (y * size.width + x)]);
                    dos.writeUTF("" + k);
                    dos.flush();
                    //fwriter.write(String.valueOf(k));
                    //ow.flush();
                    //fwriter.write(",");
                    //ow.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            /*try {
                fwriter.write("\n");
                ow.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            //fos_g.flush();
        }

        try {
            fos_g.flush();
            fos_g.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private boolean knownMarkerDetection(Mat ids) {
        int prevCamTrackInitCount[] = camTrackInitCount.clone();
        camTrackInitCount = new int[MAX_MARKER_ID]; //initialises every element to 0, hopefully
        Log.i(TAG, "knownMarkerDetection: Check ids for same marker");
        for (int i = 0; i < ids.size().height; i++) {
            int id = (int) ids.get(i, 0)[0];
            Log.i(TAG, "knownMarkerDetection: Check for marker with ID " + Integer.toString(id));
            if (landmarkPoints.get(id) != null) {
                camTrackInitCount[id] = prevCamTrackInitCount[id] + 1;
                if (camTrackInitCount[id] == CAM_INIT_COUNT) {
                    Log.i(TAG, "knownMarkerDetection: Yup same marker found in required frames");
                    return true;
                }
            }
        }
        Log.i(TAG, "knownMarkerDetection: camInit: marker not repeated until now");
        return false;
    }

    public void findMarkerCorner(int id, float a, MatOfPoint3f cornerWorldPoints) {
        float[] worldCoord = landmarkPoints.get(id);
        if(setLandmarkPointsCount==1)
        {
            cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[0] - (a / 2), worldCoord[1] + (a / 2), 0)));
            cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[0] + (a / 2), worldCoord[1] + (a / 2), 0)));
            cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[0] + (a / 2), worldCoord[1] - (a / 2), 0)));
            cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[0] - (a / 2), worldCoord[1] - (a / 2), 0)));
        }
        if(setLandmarkPointsCount==2)
        {
            cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[2] - (a / 2), worldCoord[1] + (a / 2), 0)));
            cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[2] + (a / 2), worldCoord[1] + (a / 2), 0)));
            cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[2] + (a / 2), worldCoord[1] - (a / 2), 0)));
            cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[2] - (a / 2), worldCoord[1] - (a / 2), 0)));
        }
        /*cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[0] - (a / 2), worldCoord[1] + (a / 2), 0)));
        cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[0] + (a / 2), worldCoord[1] + (a / 2), 0)));
        cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[0] + (a / 2), worldCoord[1] - (a / 2), 0)));
        cornerWorldPoints.push_back(new MatOfPoint3f(new Point3(worldCoord[0] - (a / 2), worldCoord[1] - (a / 2), 0)));*/
        Log.i(TAG, "findMarkerCorner: cornerworldpoints" + cornerWorldPoints.dump());
        x_fixed=worldCoord[0];
        y_fixed=worldCoord[1];
        z_fixed=worldCoord[2];
    }
    public void findPoseSingleMarker(List<Mat> corners, Mat ids, float length, Mat mCameraMatrix, Mat mDistortionCoefficients, Mat rvecs, Mat tvecs) {
        int knownLandmarkCount = 0;
        for (int i = 0; i < ids.size().height; i++) {
            int id = (int) ids.get(i, 0)[0];
            if (landmarkPoints.get(id) != null) { //if known marker found and mismatch is false
                //finding the world coordinaates of known marker corners
                if (setLandmarkPoints.get(id) != null)
                {   Log.i(TAG, "findPoseSingleMarker: setLandmarkPoints" + setLandmarkPoints.toString());
                    Log.i(TAG, "findPoseSingleMarker: known" + basicSlam.knownLandmarkPoints.toString());
                    if(setCount.get(id) == null) {
                        setCount.put(id, 1);
                        setLandmarkPointsCount++;
                        Log.i(TAG, "findPoseSingleMarker: id" + Integer.toString(id));
                        Log.i(TAG, "findPoseSingleMarker: setLandmarkPointsCount" + Integer.toString(setLandmarkPointsCount));
                        Log.i(TAG, "findPoseSingleMarker: setCount" + setCount.toString());

                    }
                }

                MatOfPoint3f cornerWorldPoints = new MatOfPoint3f();
                findMarkerCorner(id, length, cornerWorldPoints);
                Mat rvec = new Mat();
                Mat tvec = new Mat();
                Log.i(TAG, "findPoseSingleMarker: corner2d" + corners.get(i).dump());
                MatOfPoint2f imagepoints = new MatOfPoint2f();
                Mat ithCorners = corners.get(i);
                imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 0)[0], ithCorners.get(0, 0)[1])));
                imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 1)[0], ithCorners.get(0, 1)[1])));
                imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 2)[0], ithCorners.get(0, 2)[1])));
                imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 3)[0], ithCorners.get(0, 3)[1])));
                MatOfDouble distcoeff = new MatOfDouble();
                mDistortionCoefficients.convertTo(distcoeff, CvType.CV_64F);
                Calib3d.solvePnP(cornerWorldPoints, imagepoints, mCameraMatrix, distcoeff, rvec, tvec, true,CV_P3P);
                //Log.i(TAG, "findPoseSingleMarker: cornerworld"+ cornerWorldPoints.dump());
                Log.i(TAG, "findPoseSingleMarker: rvec" + Integer.toString(i) + rvec.dump());
                Log.i(TAG, "findPoseSingleMarker: tvec" + Integer.toString(i) + tvec.dump());
                List<Mat> rvec3c = Arrays.asList(rvec.row(0), rvec.row(1), rvec.row(2));
                Core.merge(rvec3c, rvec);
                List<Mat> tvec3c = Arrays.asList(tvec.row(0), tvec.row(1), tvec.row(2));
                Core.merge(tvec3c, tvec);
                Log.i(TAG, "findPoseSingleMarker: rvec" + Integer.toString(i) + rvec.dump());
                rvecs.push_back(rvec);
                tvecs.push_back(tvec);
                knownLandmarkCount++;

                //find the distant corner and get tvec, rvec using mix points
                if (MISMATCH_LANDMARK) {
                    int maxIdPosition = i;
                    double maxDistance = 0;
                    ithCorners = corners.get(i);
                    for (int j = 0; j < ids.size().height; j++) {
                        if (i != j) {
                            Mat jthCorners = corners.get(j);
                            double xcoordinate1 = (ithCorners.get(0, 0)[0] + ithCorners.get(0, 2)[0]) / 2;
                            double ycoordinate1 = (ithCorners.get(0, 0)[1] + ithCorners.get(0, 2)[1]) / 2;
                            double xcoordinate2 = (jthCorners.get(0, 0)[0] + jthCorners.get(0, 2)[0]) / 2;
                            double ycoordinate2 = (jthCorners.get(0, 0)[1] + jthCorners.get(0, 2)[1]) / 2;
                            double distance = Math.sqrt((xcoordinate1 - xcoordinate2) * (xcoordinate1 - xcoordinate2) + (ycoordinate1 - ycoordinate2) * (ycoordinate1 - ycoordinate2));
                            if (distance > maxDistance) {
                                maxIdPosition = j;
                                maxDistance = distance;
                            }
                        }
                    }
                    if (maxIdPosition != i) {
                        MatOfPoint3f cornerWorldPoints1 = new MatOfPoint3f();
                        MatOfPoint3f cornerWorldPoints2 = new MatOfPoint3f();
                        cornerWorldPoints = new MatOfPoint3f();
                        rvec = new Mat();
                        tvec = new Mat();
                        imagepoints = new MatOfPoint2f();
                        ithCorners = corners.get(i);
                        int id2 = (int) ids.get(maxIdPosition, 0)[0];
                        Mat jthCorners = corners.get(maxIdPosition);
                        findMarkerCorner(id, length, cornerWorldPoints1);
                        findMarkerCorner(id2, length, cornerWorldPoints2);
                        if (ithCorners.get(0, 0)[0] < jthCorners.get(0, 1)[0]) {
                            imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 0)[0], ithCorners.get(0, 0)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(jthCorners.get(0, 1)[0], jthCorners.get(0, 1)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(jthCorners.get(0, 2)[0], jthCorners.get(0, 2)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 3)[0], ithCorners.get(0, 3)[1])));
                            cornerWorldPoints.push_back(cornerWorldPoints1.row(0));
                            cornerWorldPoints.push_back(cornerWorldPoints2.row(1));
                            cornerWorldPoints.push_back(cornerWorldPoints2.row(2));
                            cornerWorldPoints.push_back(cornerWorldPoints1.row(3));
                        } else {
                            imagepoints.push_back(new MatOfPoint2f(new Point(jthCorners.get(0, 0)[0], jthCorners.get(0, 0)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 1)[0], ithCorners.get(0, 1)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 2)[0], ithCorners.get(0, 2)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(jthCorners.get(0, 3)[0], jthCorners.get(0, 3)[1])));
                            cornerWorldPoints.push_back(cornerWorldPoints2.row(0));
                            cornerWorldPoints.push_back(cornerWorldPoints1.row(1));
                            cornerWorldPoints.push_back(cornerWorldPoints1.row(2));
                            cornerWorldPoints.push_back(cornerWorldPoints2.row(3));
                        }
                        Calib3d.solvePnP(cornerWorldPoints, imagepoints, mCameraMatrix, distcoeff, rvec, tvec);
                        rvec3c = Arrays.asList(rvec.row(0), rvec.row(1), rvec.row(2));
                        Core.merge(rvec3c, rvec);
                        tvec3c = Arrays.asList(tvec.row(0), tvec.row(1), tvec.row(2));
                        Core.merge(tvec3c, tvec);
                        Log.i(TAG, "findPoseSingleMarker: rvec" + Integer.toString(i) + "," + Integer.toString(maxIdPosition) + rvec.dump());
                        Log.i(TAG, "findPoseSingleMarker: rvec" + Integer.toString(i) + "," + Integer.toString(maxIdPosition) + rvec.dump());
                        rvecs.push_back(rvec);
                        tvecs.push_back(tvec);
                        knownLandmarkCount++;
                    }
                }
            }
        }
        Log.i(TAG, "findPoseSingleMarker: rvecs123" + rvecs.dump());
        Log.i(TAG, "findPoseSingleMarker: tvecs123" + tvecs.dump());
        Mat coordinates = findCameraCoordinates(knownLandmarkCount, rvecs, tvecs);
        TextView coordinatesText = rootView.findViewById(R.id.cameraCoordinatesTextSlam);
        if (count==9) {
            String coordText = "(" + df.format(coordinates.get(0, 0)[0]) + "," + df.format(coordinates.get(1, 0)[0]) + "," + df.format(coordinates.get(2, 0)[0]) + "," + df.format(setLandmarkPointsCount) + "," + df.format(x_fixed) + "," + df.format(knownLandmarkCount) + ")";
            coordinatesText.setText(coordText);
        }
        //finding coordinates of the remaining landmarks
        for (int i = 0; i < ids.size().height; i++) {
            int id = (int) ids.get(i, 0)[0];
            if (landmarkPoints.get(id) == null) {
                // TODO: 28-06-2018 complete this
                if (coordinates.empty() || mDistortionCoefficients.empty() || rvecs.empty() || corners.get(i).empty() || mCameraMatrix.empty())
                    break;
                if(count==9)
                    findNewMarkerCoordinates(id, coordinates, rvecs.row(0),tvecs.row(0), corners.get(i), mCameraMatrix, mDistortionCoefficients);
            }
        }
    }

    /*public void findPoseSingleMarker(List<Mat> corners, Mat ids, float length, Mat mCameraMatrix, Mat mDistortionCoefficients, Mat rvecs, Mat tvecs) {
        int knownLandmarkCount = 0;

        for (int i = 0; i < ids.size().height; i++) {
            int id = (int) ids.get(i, 0)[0];
            if (landmarkPoints.get(id) != null) { //if known marker found and mismatch is false
                //finding the world coordinaates of known marker corners
                MatOfPoint3f cornerWorldPoints = new MatOfPoint3f();
                findMarkerCorner(id, length, cornerWorldPoints);
                Mat rvec = new Mat();
                Mat tvec = new Mat();
                Log.i(TAG, "findPoseSingleMarker: corner2d" + corners.get(i).dump());
                MatOfPoint2f imagepoints = new MatOfPoint2f();
                Mat ithCorners = corners.get(i);
                imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 0)[0], ithCorners.get(0, 0)[1])));
                imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 1)[0], ithCorners.get(0, 1)[1])));
                imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 2)[0], ithCorners.get(0, 2)[1])));
                imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 3)[0], ithCorners.get(0, 3)[1])));
                //imagepoints.copyTo(imagepointsTemp);
                MatOfDouble distcoeff = new MatOfDouble();
                mDistortionCoefficients.convertTo(distcoeff, CvType.CV_64F);
                Calib3d.solvePnP(cornerWorldPoints, imagepoints, mCameraMatrix, distcoeff, rvec, tvec);
                //Log.i(TAG, "findPoseSingleMarker: cornerworld"+ cornerWorldPoints.dump());
                Log.i(TAG, "findPoseSingleMarker: rvec" + Integer.toString(i) + rvec.dump());
                Log.i(TAG, "findPoseSingleMarker: tvec" + Integer.toString(i) + tvec.dump());
                List<Mat> rvec3c = Arrays.asList(rvec.row(0), rvec.row(1), rvec.row(2));
                Core.merge(rvec3c, rvec);
                List<Mat> tvec3c = Arrays.asList(tvec.row(0), tvec.row(1), tvec.row(2));
                Core.merge(tvec3c, tvec);
                Log.i(TAG, "findPoseSingleMarker: rvec" + Integer.toString(i) + rvec.dump());
                rvecs.push_back(rvec);
                tvecs.push_back(tvec);
                knownLandmarkCount++;

                //find the distant corner and get tvec, rvec using mix points
                if (MISMATCH_LANDMARK) {
                    int maxIdPosition = i;
                    double maxDistance = 0;
                    ithCorners = corners.get(i);
                    for (int j = 0; j < ids.size().height; j++) {
                        if (i != j) {
                            Mat jthCorners = corners.get(j);
                            double xcoordinate1 = (ithCorners.get(0, 0)[0] + ithCorners.get(0, 2)[0]) / 2;
                            double ycoordinate1 = (ithCorners.get(0, 0)[1] + ithCorners.get(0, 2)[1]) / 2;
                            double xcoordinate2 = (jthCorners.get(0, 0)[0] + jthCorners.get(0, 2)[0]) / 2;
                            double ycoordinate2 = (jthCorners.get(0, 0)[1] + jthCorners.get(0, 2)[1]) / 2;
                            double distance = Math.sqrt((xcoordinate1 - xcoordinate2) * (xcoordinate1 - xcoordinate2) + (ycoordinate1 - ycoordinate2) * (ycoordinate1 - ycoordinate2));
                            if (distance > maxDistance) {
                                maxIdPosition = j;
                                maxDistance = distance;
                            }
                        }
                    }
                    if (maxIdPosition != i) {
                        MatOfPoint3f cornerWorldPoints1 = new MatOfPoint3f();
                        MatOfPoint3f cornerWorldPoints2 = new MatOfPoint3f();
                        cornerWorldPoints = new MatOfPoint3f();
                        rvec = new Mat();
                        tvec = new Mat();
                        imagepoints = new MatOfPoint2f();
                        ithCorners = corners.get(i);
                        int id2 = (int) ids.get(maxIdPosition, 0)[0];
                        Mat jthCorners = corners.get(maxIdPosition);
                        findMarkerCorner(id, length, cornerWorldPoints1);
                        findMarkerCorner(id2, length, cornerWorldPoints2);
                        if (ithCorners.get(0, 0)[0] < jthCorners.get(0, 1)[0]) {
                            imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 0)[0], ithCorners.get(0, 0)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(jthCorners.get(0, 1)[0], jthCorners.get(0, 1)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(jthCorners.get(0, 2)[0], jthCorners.get(0, 2)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 3)[0], ithCorners.get(0, 3)[1])));
                            cornerWorldPoints.push_back(cornerWorldPoints1.row(0));
                            cornerWorldPoints.push_back(cornerWorldPoints2.row(1));
                            cornerWorldPoints.push_back(cornerWorldPoints2.row(2));
                            cornerWorldPoints.push_back(cornerWorldPoints1.row(3));
                        } else {
                            imagepoints.push_back(new MatOfPoint2f(new Point(jthCorners.get(0, 0)[0], jthCorners.get(0, 0)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 1)[0], ithCorners.get(0, 1)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 2)[0], ithCorners.get(0, 2)[1])));
                            imagepoints.push_back(new MatOfPoint2f(new Point(jthCorners.get(0, 3)[0], jthCorners.get(0, 3)[1])));
                            cornerWorldPoints.push_back(cornerWorldPoints2.row(0));
                            cornerWorldPoints.push_back(cornerWorldPoints1.row(1));
                            cornerWorldPoints.push_back(cornerWorldPoints1.row(2));
                            cornerWorldPoints.push_back(cornerWorldPoints2.row(3));
                        }
                        Calib3d.solvePnP(cornerWorldPoints, imagepoints, mCameraMatrix, distcoeff, rvec, tvec);
                        rvec3c = Arrays.asList(rvec.row(0), rvec.row(1), rvec.row(2));
                        Core.merge(rvec3c, rvec);
                        tvec3c = Arrays.asList(tvec.row(0), tvec.row(1), tvec.row(2));
                        Core.merge(tvec3c, tvec);
                        Log.i(TAG, "findPoseSingleMarker: rvec" + Integer.toString(i) + "," + Integer.toString(maxIdPosition) + rvec.dump());
                        Log.i(TAG, "findPoseSingleMarker: rvec" + Integer.toString(i) + "," + Integer.toString(maxIdPosition) + rvec.dump());
                        rvecs.push_back(rvec);
                        tvecs.push_back(tvec);
                        knownLandmarkCount++;
                    }
                }
            }
        }
        Log.i(TAG, "findPoseSingleMarker: rvecs" + rvecs.dump());
        Log.i(TAG, "findPoseSingleMarker: tvecs" + tvecs.dump());
        Mat coordinates = findCameraCoordinates(knownLandmarkCount, rvecs, tvecs);
        TextView coordinatesText = rootView.findViewById(R.id.cameraCoordinatesTextSlam);
        String coordText = "(" + df.format(coordinates.get(0, 0)[0]) + "," + df.format(coordinates.get(1, 0)[0]) + "," + df.format(coordinates.get(2, 0)[0]) + ")";
        coordinatesText.setText(coordText);



        //finding coordinates of the remaining landmarks
        for (int i = 0; i < ids.size().height; i++) {
            int id = (int) ids.get(i, 0)[0];
            if (landmarkPoints.get(id) == null) {
                MatOfPoint2f imagepoints1 = new MatOfPoint2f();
                Mat ithCorners = corners.get(i);
                imagepoints1.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 0)[0], ithCorners.get(0, 0)[1])));
                imagepoints1.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 1)[0], ithCorners.get(0, 1)[1])));
                imagepoints1.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 2)[0], ithCorners.get(0, 2)[1])));
                imagepoints1.push_back(new MatOfPoint2f(new Point(ithCorners.get(0, 3)[0], ithCorners.get(0, 3)[1])));
                imagepoints1.copyTo(imagepointsTemp);
                Log.i(TAG, "Testing3: id" + Integer.toString(id));
                Log.i(TAG, "Testing3: count" + Integer.toString(count));
                // TODO: 28-06-2018 complete this
                if (coordinates.empty() || mDistortionCoefficients.empty() || rvecs.empty() || corners.get(i).empty() || mCameraMatrix.empty())
                    break;
                if(count==3){

                    Mat.zeros(1, 3, CvType.CV_64F).copyTo(rvecs_frame0);
                    Mat.zeros(1, 3, CvType.CV_64F).copyTo(tvecs_frame0);
                    /*rvecs_frame0.put(0, 0, rvecs.get(0, 0)[0]);
                    rvecs_frame0.put(0, 1, rvecs.get(0, 0)[1]);
                    rvecs_frame0.put(0, 2, rvecs.get(0, 0)[2]);
                    tvecs_frame0.put(0, 0, tvecs.get(0, 0)[0]);
                    tvecs_frame0.put(0, 1, tvecs.get(0, 0)[1]);
                    tvecs_frame0.put(0, 2, tvecs.get(0, 0)[2]);*/
                    /*2rvecs.copyTo(rvecs_frame0);
                    tvecs.copyTo(tvecs_frame0);
                    Log.i(TAG, "Testing: rvecs" + rvecs.dump());
                    Log.i(TAG, "Testing: tvecs" + tvecs.dump());
                    Log.i(TAG, "Testing: rvecs" + rvecs_frame0.dump());
                    Log.i(TAG, "Testing: tvecs" + tvecs_frame0.dump());
                    imagepointsTemp.copyTo(imagepointsF0);
                    Log.i(TAG, "Testing: ip" + imagepointsTemp.dump());
                    Log.i(TAG, "Testing: ip" + imagepointsF0.dump());

                }
                Log.i(TAG, "Testing2: rvecs"+ Integer.toString(count) +"@@@"+ rvecs.dump());
                Log.i(TAG, "Testing2: tvecs" +Integer.toString(count) +"@@@"+ tvecs.dump());
                if(count==69) {
                    //findNewMarkerCoordinates(id, coordinates, rvecs.row(0), tvecs.row(0), corners.get(i), mCameraMatrix, mDistortionCoefficients);
                    Log.i(TAG, "findPoseSingleMarker: count" + Integer.toString(count));
                    Log.i(TAG, "findPoseSingleMarker: tvecs at frame0" + tvecs_frame0.dump());
                    Log.i(TAG, "findPoseSingleMarker: rvecs at frame0" + rvecs_frame0.dump());
                    Log.i(TAG, "findPoseSingleMarker: tvecs at frame30" + tvecs.dump());
                    Log.i(TAG, "findPoseSingleMarker: rvecs at frame30" + rvecs.dump());
                    if (coordinates.empty() || mDistortionCoefficients.empty() || rvecs.empty() || corners.get(i).empty() || mCameraMatrix.empty()||rvecs_frame0.empty()||tvecs_frame0.empty())
                        break;
                    findNewMarkerCoordinates(id, coordinates, rvecs.row(0), tvecs.row(0),rvecs_frame0.row(0),tvecs_frame0.row(0), corners.get(i), mCameraMatrix, mDistortionCoefficients);
                }
                count++;
            }
        }
    }*/

    /**private void findNewMarkerCoordinates(int id, Mat camCoord, Mat rvec3c,Mat tvec3c, Mat corner, Mat mCameraMatrix, Mat mDistortionCoefficients) {
        Mat rvec = new Mat();
        Mat tvec = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F);
        Mat result = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(result);
        Mat rotMatrix = new Mat();
        Mat tempResult = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(rvec);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tvec);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tempResult);
        rvec.put(0, 0, rvec3c.get(0, 0)[0]);
        rvec.put(1, 0, rvec3c.get(0, 0)[1]);
        rvec.put(2, 0, rvec3c.get(0, 0)[2]);

        tvec.put(0, 0, tvec3c.get(0, 0)[0]);
        tvec.put(1, 0, tvec3c.get(0, 0)[1]);
        tvec.put(2, 0, tvec3c.get(0, 0)[2]);
        Mat newMarkerPoint = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(newMarkerPoint);
        newMarkerPoint.put(0, 0, corner.get(0, 0)[0]);
        newMarkerPoint.put(1, 0, corner.get(0, 0)[1]);
        newMarkerPoint.put(2, 0, 1);
        //Camera coordinates can be obtained using 'camera position = - rot_matrix.transpose() * tvec'
        //Here rotation matrix can be obtained by Rodrigues function
        Log.i(TAG, "findNewMarkerCoordinates: corners" + corner.dump());
        Log.i(TAG, "findNewMarkerCoordinates: marker 2D point" + Integer.toString(id) + newMarkerPoint.dump());
        Calib3d.Rodrigues(rvec, rotMatrix);
        rotMatrix = rotMatrix.inv();
        Log.i(TAG, "Camera matrix1: " + mCameraMatrix.dump());
        mCameraMatrix = mCameraMatrix.inv();
        Log.i(TAG, "Camera matrix2: " + mCameraMatrix.dump());
        Core.gemm(mCameraMatrix, newMarkerPoint, 1, newMarkerPoint, 0, newMarkerPoint, 0);
        Core.subtract(newMarkerPoint,tvec,tempResult);
        tempResult.copyTo(newMarkerPoint);
        Log.i(TAG, "findNewMarkerCoordinates: first Mult" + newMarkerPoint.dump());
        Core.gemm(rotMatrix, newMarkerPoint, 1, camCoord, 0, result, 0);
        //result.put(2, 0,result.get(2, 0)[0]-camCoord.get(2, 0)[0]);
        Log.i(TAG, "findNewMarkerCoordinates: result" + result.dump());
        float[] landmarkCoord = new float[3];
        landmarkCoord[0] = (float) result.get(0, 0)[0];
        landmarkCoord[1] = (float) result.get(1, 0)[0];
        //landmarkCoord[2] = (float) result.get(2, 0)[0];
        landmarkCoord[2] = z_fixed;

        double[] a2 = new double[3];
        result.get(0, 0, a2);//I get byte array here for the whole image
        basicSlam.landmarkDump+= "\n"+String.format("%.2f",a2[0])+","+String.format("%.2f",a2[1])+","+String.format("%.2f",a2[2]);
        //basicSlam.landmarkDump+= Float.toString(landmarkCoord[0])+","+Float.toString(landmarkCoord[1])+","+Float.toString(landmarkCoord[2])+"\n";


        landmarkPoints.put(id, landmarkCoord);
    }*/

    private void findNewMarkerCoordinates(int id, Mat camCoord, Mat rvec3c, Mat tvec3c, Mat corner, Mat mCameraMatrix, Mat mDistortionCoefficients) {
        Mat rvec = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F);
        Mat result = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(result);
        Mat rotMatrix1 = new Mat();
        Mat tempResult = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(rvec);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tempResult);
        rvec.put(0, 0, rvec3c.get(0, 0)[0]);
        rvec.put(1, 0, rvec3c.get(0, 0)[1]);
        rvec.put(2, 0, rvec3c.get(0, 0)[2]);
        Mat newMarkerPoint = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(newMarkerPoint);
        newMarkerPoint.put(0, 0, corner.get(0, 0)[0]);
        newMarkerPoint.put(1, 0, corner.get(0, 0)[1]);
        newMarkerPoint.put(2, 0, 1);
        //Camera coordinates can be obtained using 'camera position = - rot_matrix.transpose() * tvec'
        //Here rotation matrix can be obtained by Rodrigues function
        Log.i(TAG, "findNewMarkerCoordinates: corners" + corner.dump());
        Log.i(TAG, "findNewMarkerCoordinates: marker 2D point" + Integer.toString(id) + newMarkerPoint.dump());
        Calib3d.Rodrigues(rvec, rotMatrix1);
        //rotMatrix = rotMatrix.inv();
        rotMatrix1.put(0, 2, tvec3c.get(0, 0)[0]);
        rotMatrix1.put(1, 2, tvec3c.get(0, 0)[1]);
        rotMatrix1.put(2, 2, tvec3c.get(0, 0)[2]);
        Log.i(TAG, "H matrix: " + rotMatrix1.dump());
        mCameraMatrix = mCameraMatrix.inv();
        rotMatrix1 = rotMatrix1.inv();
        Log.i(TAG, "Camera matrix: " + mCameraMatrix.dump());
        Core.gemm( rotMatrix1,mCameraMatrix, 1, newMarkerPoint, 0, rotMatrix1, 0);
        Log.i(TAG, "findNewMarkerCoordinates: first Mult" + rotMatrix1.dump());
        Core.gemm(rotMatrix1, newMarkerPoint, 1, camCoord, 0, result, 0);
        //result.put(2, 0,result.get(2, 0)[0]-camCoord.get(2, 0)[0]);
        
        Log.i(TAG, "findNewMarkerCoordinates: result" + result.dump());
        float[] landmarkCoord = new float[3];
        /*landmarkCoord[0] = (float) (result.get(0, 0)[0]/result.get(2, 0)[0]);
        landmarkCoord[1] = (float) (result.get(1, 0)[0]/result.get(2, 0)[0]);
        landmarkCoord[2] = z_fixed;*/
        if(setLandmarkPointsCount==1)
        {
        landmarkCoord[0] = (float) (result.get(0, 0)[0]/result.get(2, 0)[0]);
        landmarkCoord[1] = (float) (result.get(1, 0)[0]/result.get(2, 0)[0]);
        landmarkCoord[2] = z_fixed;
        landmarkCoord[0]+=(9.1/2);
        landmarkCoord[1]-=(9.1/2);
        }
        if(setLandmarkPointsCount==2)
        {
            landmarkCoord[0] = x_fixed;
            landmarkCoord[1] = (float) (result.get(1, 0)[0]/result.get(2, 0)[0]);
            landmarkCoord[2] = (float) (result.get(0, 0)[0]/result.get(2, 0)[0]);
            landmarkCoord[2]+=(9.1/2);
            landmarkCoord[1]-=(9.1/2);
        }
        else
        {
            landmarkCoord[0] = (float) (result.get(0, 0)[0]/result.get(2, 0)[0]);
            landmarkCoord[1] = (float) (result.get(1, 0)[0]/result.get(2, 0)[0]);
            landmarkCoord[2] = z_fixed;
            landmarkCoord[0]+=(9.1/2);
            landmarkCoord[1]-=(9.1/2);
        }
        double[] a2 = new double[3];
        result.get(0, 0, a2);//I get byte array here for the whole image
        basicSlam.landmarkDump+= "\n"+String.format("%.2f",a2[0])+","+String.format("%.2f",a2[1])+","+String.format("%.2f",a2[2]);
        //basicSlam.landmarkDump+= Float.toString(landmarkCoord[0])+","+Float.toString(landmarkCoord[1])+","+Float.toString(landmarkCoord[2])+"\n";


        landmarkPoints.put(id, landmarkCoord);
    }

    /*private void findNewMarkerCoordinates(int id, Mat camCoord, Mat rvec3c, Mat tvec3c,Mat rvecf0, Mat tvecf0, Mat corner, Mat mCameraMatrix, Mat mDistortionCoefficients) {
        Mat Points4D= new Mat();
        Mat R1 = new Mat();
        Mat R2 = new Mat();
        Mat P1 = new Mat();
        Mat P2 = new Mat();
        Mat Q= new Mat();
        Mat temp= new Mat();
        Mat rvec = new Mat();
        Mat dist= new Mat();
        Mat.zeros(3, 1, CvType.CV_64F);
        Mat result = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(result);
        Mat rotMatrix1 = new Mat();
        Mat tempResult = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(temp);
        Mat.zeros(4, 1, CvType.CV_64F).copyTo(dist);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(rvec);
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tempResult);
        rvec.put(0, 0, rvec3c.get(0, 0)[0]);
        rvec.put(1, 0, rvec3c.get(0, 0)[1]);
        rvec.put(2, 0, rvec3c.get(0, 0)[2]);
        Mat tvec = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tvec);
        tvec.put(0, 0, tvec3c.get(0, 0)[0]);
        tvec.put(1, 0, tvec3c.get(0, 0)[1]);
        tvec.put(2, 0, tvec3c.get(0, 0)[2]);

        Mat rvecf = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F);
        Mat rotMatrix2 = new Mat();
        Mat homoMatrix = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(rvecf);

        rvecf.put(0, 0, rvecf0.get(0, 0)[0]);
        rvecf.put(1, 0, rvecf0.get(0, 0)[1]);
        rvecf.put(2, 0, rvecf0.get(0, 0)[2]);
        Calib3d.Rodrigues(rvecf, rotMatrix1);

        Mat tvecf = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(tvecf);
        tvecf.put(0, 0, tvecf0.get(0, 0)[0]);
        tvecf.put(1, 0, tvecf0.get(0, 0)[1]);
        tvecf.put(2, 0, tvecf0.get(0, 0)[2]);

        Mat rotMatrixRel = new Mat();
        Mat.zeros(3, 3, CvType.CV_64F).copyTo(rotMatrixRel);
        Mat transVecRel = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(transVecRel);

        Mat newMarkerPoint = new Mat();
        Mat.zeros(3, 1, CvType.CV_64F).copyTo(newMarkerPoint);
        newMarkerPoint.put(0, 0, corner.get(0, 0)[0]);
        newMarkerPoint.put(1, 0, corner.get(0, 0)[1]);
        newMarkerPoint.put(2, 0, 1);

        //Camera coordinates can be obtained using 'camera position = - rot_matrix.transpose() * tvec'
        //Here rotation matrix can be obtained by Rodrigues function

        Log.i(TAG, "findNewMarkerCoordinates: corners" + corner.dump());
        Log.i(TAG, "findNewMarkerCoordinates: marker 2D point" + Integer.toString(id) + newMarkerPoint.dump());
        Calib3d.Rodrigues(rvec, rotMatrix2);
        rotMatrix1.copyTo(homoMatrix);
        Log.i(TAG, "findNewMarkerCoordinates: rotMatrix1" + rotMatrix1.dump());

        //rotMatrix = rotMatrix.inv();
       /* rotMatrix1.put(0, 2, tvec3c.get(0, 0)[0]);
        rotMatrix1.put(1, 2, tvec3c.get(0, 0)[1]);
        rotMatrix1.put(2, 2, tvec3c.get(0, 0)[2]);*/

        /*2Log.i(TAG, "H matrix1: " + homoMatrix.dump());
        //zmCameraMatrix = mCameraMatrix.inv();
        rotMatrix1 = rotMatrix1.inv();


        Log.i(TAG, "findNewMarkerCoordinates: rvecs at frame0" + rvecf0.dump());
        Log.i(TAG, "findNewMarkerCoordinates: rvecs at frame0" + rvecf.dump());
        Log.i(TAG, "findNewMarkerCoordinates: rvecs at frame30" + rvec3c.dump());
        Log.i(TAG, "findNewMarkerCoordinates: rvecs at frame30" + rvec.dump());
        Log.i(TAG, "findNewMarkerCoordinates: tvecs at frame0" + tvecf0.dump());
        Log.i(TAG, "findNewMarkerCoordinates: tvecs at frame0" + tvecf.dump());
        Log.i(TAG, "findNewMarkerCoordinates: tvecs at frame30" + tvec3c.dump());
        Log.i(TAG, "findNewMarkerCoordinates: tvecs at frame30" + tvec.dump());
        Core.gemm( rotMatrix1,rotMatrix2, 1, newMarkerPoint, 0, rotMatrixRel, 0);
        Log.i(TAG, "Relative rot matrix: " + rotMatrixRel.dump());
        Core.gemm( rotMatrixRel,tvecf, -1, tvec, 1, transVecRel, 0);

        Log.i(TAG, "Relative Trans vector: " + transVecRel.dump());
        /*Log.i(TAG, "Camera matrix: " + mCameraMatrix.dump());
        Core.gemm( rotMatrix1,mCameraMatrix, 1, newMarkerPoint, 0, rotMatrix1, 0);
        Log.i(TAG, "findNewMarkerCoordinates: first Mult" + rotMatrix1.dump());
        Core.gemm(rotMatrix1, newMarkerPoint, 1, camCoord, 0, result, 0);*/
        //result.put(2, 0,result.get(2, 0)[0]-camCoord.get(2, 0)[0]);
        //Size mImageSize = new Size(mrgba.width() , mrgba.height());
        /*3MatOfDouble distcoeff1 = new MatOfDouble();
        mDistortionCoefficients.convertTo(distcoeff1, CvType.CV_64F);
        Log.i(TAG, "findNewMarkerCoordinates: Size" + mrgba.size());
        Log.i(TAG, "findNewMarkerCoordinates: Size" + mOutput.size());
        Calib3d.stereoRectify(mCameraMatrix,distcoeff1,mCameraMatrix,distcoeff1,mImageSize,rotMatrixRel,transVecRel,R1,R2,P1,P2,Q);
        MatOfPoint2f iF0 = new MatOfPoint2f();
        MatOfPoint2f iTemp = new MatOfPoint2f();
        Log.i(TAG, "mDistortionCoefficients" + mDistortionCoefficients.dump());
        Log.i(TAG, "distcoeff1" + distcoeff1.dump());

        dist.put(0, 0, distcoeff1.get(0, 0)[0]);
        dist.put(1, 0, distcoeff1.get(1, 0)[0]);
        dist.put(2, 0, distcoeff1.get(4, 0)[0]);
        dist.put(3, 0, 0.0);
        Log.i(TAG, "dist" + dist.dump());

        /*Calib3d.undistortPoints(imagepointsF0,iF0,mCameraMatrix,dist,R1,P1);
        Calib3d.undistortPoints(imagepointsTemp,iTemp,mCameraMatrix,dist,R2,P2);
        Calib3d.triangulatePoints(P1,P2,iF0,iTemp,Points4D);*/
        /*4Calib3d.triangulatePoints(P1,P2,imagepointsF0,imagepointsTemp,Points4D);
        /*float[] firstCornerCoord = new float[3];
        firstCornerCoord[0] = (float) (Points4D.get(0, 0)[0]/Points4D.get(3, 0)[0]);
        firstCornerCoord[1] = (float) (Points4D.get(1, 0)[0]/Points4D.get(3, 0)[0]);
        firstCornerCoord[2] = (float) (Points4D.get(2, 0)[0]/Points4D.get(3, 0)[0]);*/
        /*5Log.i(TAG,"Tem4D"+ Points4D.dump() );
        temp.put(0,0,(Points4D.get(0, 0)[0]/Points4D.get(3, 0)[0]));
        temp.put(1,0,(Points4D.get(1, 0)[0]/Points4D.get(3, 0)[0]));
        temp.put(2,0,(Points4D.get(2, 0)[0]/Points4D.get(3, 0)[0]));
        Log.i(TAG,"TempResult"+ temp.dump() );
        //Log.i(TAG, "findNewMarkerCoordinates: result" + result.dump());
        /*float[] landmarkCoord = new float[3];
        landmarkCoord[0] = (float) (result.get(0, 0)[0]/result.get(2, 0)[0]);
        landmarkCoord[1] = (float) (result.get(1, 0)[0]/result.get(2, 0)[0]);
        landmarkCoord[2] = z_fixed;

        double[] a2 = new double[3];
        result.get(0, 0, a2);//I get byte array here for the whole image
        basicSlam.landmarkDump+= "\n"+String.format("%.2f",a2[0])+","+String.format("%.2f",a2[1])+","+String.format("%.2f",a2[2]);*/
        //basicSlam.landmarkDump+= Float.toString(landmarkCoord[0])+","+Float.toString(landmarkCoord[1])+","+Float.toString(landmarkCoord[2])+"\n";
        /*6homoMatrix.put(0, 2, tvecf.get(0, 0)[0]);
        homoMatrix.put(1, 2, tvecf.get(1, 0)[0]);
        homoMatrix.put(2, 2, tvecf.get(2, 0)[0]);
        float[] landmarkCoord = new float[3];
        //Log.i(TAG, "Temp: " + temp.dump());
        Log.i(TAG, "findNewMarkerCoordinates: homoMatrix2" + homoMatrix.dump());
        homoMatrix = homoMatrix.inv();
        Core.gemm(homoMatrix,temp,1,camCoord,0,result);
        Log.i(TAG, "findNewMarkerCoordinates: result1" + result.dump());

        landmarkCoord[0] = (float) (result.get(0, 0)[0]);
        landmarkCoord[1] = (float) (result.get(1, 0)[0]);
        landmarkCoord[2] = (float)(result.get(2, 0)[0]);
        landmarkCoord[0]+=(9.1/2);
        landmarkCoord[1]-=(9.1/2);
        landmarkPoints.put(id, landmarkCoord);
        /*landmarkCoord[0] = (float) (temp.get(0, 0)[0]);
        landmarkCoord[1] = (float) (temp.get(1, 0)[0]);
        landmarkCoord[2] = (float)(temp.get(2, 0)[0]);
        landmarkCoord[0]+=(9.1/2);
        landmarkCoord[1]-=(9.1/2);
        landmarkPoints.put(id, landmarkCoord);*/

    //}

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mrgba = inputFrame.rgba();
        Mat ids = new Mat();
        List<Mat> corners = new ArrayList<>();
        Mat rvecs = new Mat();
        Mat tvecs = new Mat();
        Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_250);
        //detectMarker(mrgba.getNativeObjAddr(), mOutput.getNativeObjAddr(), );
        Aruco.detectMarkers(inputFrame.gray(), dictionary, corners, ids);
        if (!ids.empty()) {
            if (!camTrackInitFlag) {
                camTrackInitFlag = knownMarkerDetection(ids);
            } else {
                Aruco.drawDetectedMarkers(inputFrame.gray(), corners, ids, colorRed);
                if (CALIB_DONE) {
                    //Aruco.estimatePoseSingleMarkers(corners, 6.3f, mCameraMatrix, mDistortionCoefficients, rvecs, tvecs);
                    //Log.i(TAG, "findPoseSingleMarker: rvecs real" +rvecs.dump());
                    //Log.i(TAG, "findPoseSingleMarker: tvecs real" +tvecs.dump());
                    if (count%10==0)
                        count=0;
                    findPoseSingleMarker(corners, ids, 9.1f, mCameraMatrix, mDistortionCoefficients, rvecs, tvecs);
                    count++;
                    //Aruco.drawAxis(inputFrame.rgba(), mCameraMatrix, mDistortionCoefficients, rvecs, tvecs, 0.1f);
                    //Mat coordinates = getCameraCoordinates(ids, rvecs, tvecs);
                }
            }
        }
        return inputFrame.rgba();
    }


    public void printToTextFileKnownLandmarks(String d, File file_g) {
        /*Size size = d.size();
        double [] a = new double[ ( d.width() * d.height())];
        d.get(0, 0, a);//I get byte array here for the whole image*/
        FileOutputStream fos_g1 = null;
        DataOutputStream dos1 = null;
        //OutputStreamWriter ow = null;
        //BufferedWriter fwriter = null;
        try {
            fos_g1 = new FileOutputStream(file_g);
            dos1 = new DataOutputStream(fos_g1);
            //ow = new OutputStreamWriter(fos_g);
            //fwriter = new BufferedWriter(ow);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            dos1.writeUTF(d);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            dos1.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fos_g1.flush();
            fos_g1.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
