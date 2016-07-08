#include <string>
#include <iostream>
#include <jni.h>

#include "jni_helper.h"
#include "xyz_unlimitedcodeworks_operations_extra_ForwardNet.h"

#ifdef FN_USE_CAFFE
#include "GPUUtils.h"
#include "ForwardNetCaffe.h"
using CaffeForwardNet = ucw::caffe::ForwardNet;
#else
#include "ForwardNetCV.h"
using CVForwardNet = ucw::opencv::ForwardNet;
#endif

#include "cv/converters.h"

#undef UNUSED
#define UNUSED(x) (void)(x)

using namespace std;
using namespace ucw;

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_ForwardNet
 * Method:    create
 * Signature: (Ljava/lang/String;Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_xyz_unlimitedcodeworks_operations_extra_ForwardNet_create__Ljava_lang_String_2Ljava_lang_String_2
  (JNIEnv *env, jclass, jstring jModelTxt, jstring jModelBin)
{
    UNUSED(env);
    UNUSED(jModelTxt);
    UNUSED(jModelBin);

#ifndef FN_USE_CAFFE
    try
    {
        IForwardNet *net = new CVForwardNet(fromJString(env, jModelTxt),
                                            fromJString(env, jModelBin));
        return reinterpret_cast<jlong>(net);
    }
    catch (runtime_error *e)
    {
        throwJniException(env, e);
    }
#endif
    throwJniError(env, "Not implemented");
    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_ForwardNet
 * Method:    create_1
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZII)J
 */
JNIEXPORT jlong JNICALL Java_xyz_unlimitedcodeworks_operations_extra_ForwardNet_create_11
(JNIEnv *env, jclass, jstring jModelTxt, jstring jModelBin, jstring jMeanBin,
 jboolean jCaffeOnCPU, jint jTaskId, jint jMaxGPUNum)
{
    UNUSED(env);
    UNUSED(jModelTxt);
    UNUSED(jModelBin);
    UNUSED(jMeanBin);
    UNUSED(jCaffeOnCPU);
    UNUSED(jTaskId);
    UNUSED(jMaxGPUNum);

#ifdef FN_USE_CAFFE
    if (jTaskId != -1) {
        if (!hasCuda()) {
            cerr << "WARNING: cuda not found in compile time, setting current GPU will not work."
                 << endl;
        }
        int numGPUs = jMaxGPUNum == -1 ? getNumGPUs() : jMaxGPUNum;
        if (numGPUs != -1) {
            int gpuId = jTaskId % numGPUs;
            setGpuDevice(gpuId);
        }
    }

    try
    {
        IForwardNet *net = new CaffeForwardNet(fromJString(env, jModelTxt),
                                               fromJString(env, jModelBin),
                                               fromJString(env, jMeanBin),
                                               jCaffeOnCPU);
        return reinterpret_cast<jlong>(net);
    }
    catch (runtime_error *e)
    {
        throwJniException(env, e);
    }
#endif
    throwJniError(env, "Not implemented");
    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_ForwardNet
 * Method:    n_forward
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_ForwardNet_n_1forward
(JNIEnv *env, jclass, jlong nativeObj, jlong inputObj, jlong outputObj)
{
    if (!nativeObj) {
        throwJniError(env, "Method called on an uninitialized ForwardNet object");
    }
    if (nativeObj) {
        auto &fn = *reinterpret_cast<IForwardNet*>(nativeObj);
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
 * Class:     xyz_unlimitedcodeworks_operations_extra_ForwardNet
 * Method:    n_forwardBatch
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_ForwardNet_n_1forwardBatch
(JNIEnv *env, jclass, jlong nativeObj, jlong imgs_nativeObj, jlong outputs_nativeObj)
 {
    if (nativeObj) {
        auto &fn = *reinterpret_cast<IForwardNet*>(nativeObj);
        auto &imgs = *reinterpret_cast<cv::Mat*>(imgs_nativeObj);
        auto &outputs = *reinterpret_cast<cv::Mat*>(outputs_nativeObj);

        std::vector<cv::Mat> imgs_vec;
        try {
            Mat_to_vector_Mat(imgs, imgs_vec);

            auto outputs_vec = fn.forward(imgs_vec);

            vector_Mat_to_Mat(outputs_vec, outputs);
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized ForwardNet object");
    }
 }

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_ForwardNet
 * Method:    n_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_ForwardNet_n_1delete
(JNIEnv *, jclass, jlong nativeObj)
{
    auto *fn = reinterpret_cast<IForwardNet*>(nativeObj);
    delete fn;
}
