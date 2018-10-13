#include <iostream>
#include <sstream>
#include <time.h>
#include <stdio.h>
#include <fstream>
#include <string>
#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/aruco/charuco.hpp>
#include <opencv2/aruco/dictionary.hpp>
#include <opencv2/aruco.hpp>

using namespace cv;
using namespace std;

typedef Matx<double, 5, 1> Mat51d;
typedef Matx<double, 3, 3> Mat33d;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_vijaysankarbabu_slamnav_markerDetection_detectMarker(JNIEnv *env, jclass type,
                                                              jlong imgAddr, jlong dictionaryAddr,
                                                              jlong cornersAddr, jlong idsAddr) {

    cv::Mat* image = (cv::Mat*)imgAddr;
    Ptr<aruco::Dictionary> dictionary = *((Ptr<aruco::Dictionary>*)(dictionaryAddr));
    Ptr<aruco::DetectorParameters> detectorParams = aruco::DetectorParameters::create();

    vector< int > ids;
    vector< vector< Point2f > > corners, rejected;
    vector< Vec3d > rvecs, tvecs;

    aruco::detectMarkers(*image, dictionary, corners, ids, detectorParams, rejected);

    aruco::drawDetectedMarkers(*image, corners, ids, Scalar(255, 255, 0));

    //vector_int_to_Mat(ids, *((cv::Mat*)idsAddr));
    //vector_vector_Point2f_to_Mat(corners, *((cv::Mat*)cornersAddr));
}
