#include <jni.h>

#include "jni_helper.h"
#include "xyz_unlimitedcodeworks_operations_extra_CMTTracker.h"

#include "CppMT/ObjectTracking.h"

#include <stdexcept>

using namespace ucw;

using std::runtime_error;
using cmt::ObjectTracking;

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_CMTTracker
 * Method:    n_create
 * Signature: (IIIIJ)J
 */
JNIEXPORT jlong JNICALL Java_xyz_unlimitedcodeworks_operations_extra_CMTTracker_n_1create
(JNIEnv *env, jclass, jint jX, jint jY, jint jWidth, jint jHeight, jlong nativeFirstFrame)
{
    try
    {
        auto &firstFrame = *reinterpret_cast<cv::Mat*>(nativeFirstFrame);

        auto *ot = new ObjectTracking(jX, jY, jWidth, jHeight, firstFrame);
        return reinterpret_cast<jlong>(ot);
    }
    catch (runtime_error *e)
    {
        throwJniException(env, e);
    }
    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_CMTTracker
 * Method:    n_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_CMTTracker_n_1delete
(JNIEnv *, jclass, jlong nativeObj)
{
    auto *ot = reinterpret_cast<ObjectTracking*>(nativeObj);
    delete ot;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_CMTTracker
 * Method:    n_track
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_CMTTracker_n_1track
(JNIEnv *env, jclass, jlong nativeObj, jlong nativeFrame)
{
    if (nativeObj) {
        auto &ot = *reinterpret_cast<ObjectTracking*>(nativeObj);
        auto &frame = *reinterpret_cast<cv::Mat*>(nativeFrame);

        try {
            ot.trackImage(frame);
        } catch (runtime_error *e) {
            throwJniException(env, e);
        } catch (cv::Exception *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized CMTTracker object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_CMTTracker
 * Method:    n_currentPosition
 * Signature: (J)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_xyz_unlimitedcodeworks_operations_extra_CMTTracker_n_1currentPosition
(JNIEnv *env, jclass, jlong nativeObj)
{
    if (nativeObj) {
        auto &ot = *reinterpret_cast<ObjectTracking*>(nativeObj);

        try {
            auto rr = ot.currentPosition();

            double data[5] = {
                rr.center.x, rr.center.y,
                rr.size.width, rr.size.height,
                rr.angle
            };
            jdoubleArray outJNIArray = env->NewDoubleArray(5);  // allocate
            if (NULL == outJNIArray) return NULL;
            env->SetDoubleArrayRegion(outJNIArray, 0 , 5, data);  // copy
            return outJNIArray;
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized CMTTracker object");
    }
    return NULL;
}
