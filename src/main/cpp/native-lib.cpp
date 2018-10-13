#include <jni.h>
#include <string>
#include <opencv2/core.hpp>

extern "C" {

JNIEXPORT jstring
JNICALL
Java_com_example_vijaysankarbabu_slamnav_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
jstring
Java_com_example_vijaysankarbabu_slamnav_MainActivity_validate(JNIEnv *env, jobject, jlong addrGray, jlong addrRgba) {
    cv::Rect();
    cv::Mat();
    std::string hello2 = "Hello from validate";
    return env->NewStringUTF(hello2.c_str());
}

}
