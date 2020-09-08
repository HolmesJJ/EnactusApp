//
// Created by 65937 on 2018/9/21.
//

#include <jni.h>
#include <opencv2/opencv.hpp>
#include <iostream>
#include <vector>
#include <sstream>
#include <android/log.h>

#define LOG_TAG    "HAARDETECTION"

#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

using namespace cv;
using namespace std;

extern"C"
{
    CascadeClassifier face_detector;
    JNIEXPORT void JNICALL Java_com_example_enactusapp_Fragment_MainFragment_initLoad(JNIEnv* env, jobject, jstring haarfilePath)
    {
        const char *nativeString = env->GetStringUTFChars(haarfilePath, 0);
        if(!face_detector.load(nativeString))
        {
            LOGI( "Method Description: %s", "failed...");
            return;
        }
        env->ReleaseStringUTFChars(haarfilePath, nativeString);
        LOGI( "Method Description: %s", "loaded haar files...");
    }

    JNIEXPORT void JNICALL Java_com_example_enactusapp_Fragment_MainFragment_faceDetect(JNIEnv* env, jobject, jlong addrRgba)
    {
        std::vector<Rect> faces;
        Mat mGray;
        Mat& mRgba = *(Mat*)addrRgba;
        cvtColor(mRgba, mGray, COLOR_BGR2GRAY);
        equalizeHist(mGray, mGray);

        face_detector.detectMultiScale(mGray, faces, 1.1, 1, 0, Size(50, 50), Size(1000, 1000));

        if(faces.empty())
        {
            return;
        }
        for (int i=0; i<faces.size(); i++)
        {
            rectangle(mRgba, faces[i], Scalar(255, 0, 0), 2, 8, 0);
            LOGI( "Face Detection: %s", "Found Face" );
        }
    }
}
