#include <string>
#include <jni.h>

#include "jni_helper.h"
#include "xyz_unlimitedcodeworks_opencv_dnn_ForwardNet.h"
#include "ForwardNet.h"

using namespace std;
using namespace ucw;

/*
 * ==========================================================================================================
 */

/*
 * Class:     xyz_unlimitedcodeworks_opencv_dnn_ForwardNet
 * Method:    create
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_xyz_unlimitedcodeworks_opencv_dnn_ForwardNet_create
(JNIEnv *env, jclass, jstring jModelTxt, jstring jModelBin)
{
    try
    {
        return reinterpret_cast<jlong>(new ForwardNet(fromJString(env, jModelTxt),
                                                      fromJString(env, jModelBin)));
    }
    catch (runtime_error *e)
    {
        throwJniException(env, e);
    }

    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_opencv_dnn_ForwardNet
 * Method:    n_forward
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_opencv_dnn_ForwardNet_n_1forward
(JNIEnv *env, jclass, jlong nativeObj, jlong inputObj, jlong outputObj)
{
    if (nativeObj) {
        auto &fn = *reinterpret_cast<ForwardNet*>(nativeObj);
        auto &input = *reinterpret_cast<cv::Mat*>(inputObj);
        auto &output = *reinterpret_cast<cv::Mat*>(outputObj);

        try {
            output = fn.forward(input);
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_opencv_dnn_ForwardNet
 * Method:    n_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_opencv_dnn_ForwardNet_n_1delete
(JNIEnv *, jclass, jlong nativeObj)
{
    auto *fn = reinterpret_cast<ForwardNet*>(nativeObj);
    delete fn;
}

/*
 * Class:     xyz_unlimitedcodeworks_opencv_dnn_ForwardNet
 * Method:    n_setPriority
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_opencv_dnn_ForwardNet_n_1setPriority
(JNIEnv *env, jclass, jint priority)
{
    try {
        ForwardNet::setPriority(priority);
    } catch (runtime_error *e) {
        throwJniException(env, e);
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_opencv_dnn_ForwardNet
 * Method:    n_getCurrentTid
 * Signature: (V)J
 */
JNIEXPORT jlong JNICALL Java_xyz_unlimitedcodeworks_opencv_dnn_ForwardNet_n_1getCurrentTid
(JNIEnv *env, jclass)
{
    try {
        return ForwardNet::getCurrentTid();
    } catch (runtime_error *e) {
        throwJniException(env, e);
        return 0; // won't reach here
    }
}
