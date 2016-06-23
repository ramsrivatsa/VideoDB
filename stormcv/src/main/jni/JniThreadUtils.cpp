#include <string>
#include <jni.h>

#include "jni_helper.h"
#include "xyz_unlimitedcodeworks_opencv_dnn_ForwardNet.h"
#include "ThreadUtils.h"

using namespace std;
using namespace ucw;

/*
 * Class:     xyz_unlimitedcodeworks_opencv_dnn_ForwardNet
 * Method:    n_setPriority
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_opencv_dnn_ForwardNet_n_1setPriority
(JNIEnv *env, jclass, jint priority)
{
    try {
        setPriority(priority);
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
        return getCurrentTid();
    } catch (runtime_error *e) {
        throwJniException(env, e);
        return 0; // won't reach here
    }
}
