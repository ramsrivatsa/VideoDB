#include <jni.h>

#include "jni_helper.h"
#include "xyz_unlimitedcodeworks_operations_extra_Captioner.h"

#include "s2vt.h"
#include "cv/converters.h"
#include "GPUUtils.h"

#include <stdexcept>

using namespace ucw;

using std::runtime_error;

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_Captioner
 * Method:    n_create
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZII)J
 */
JNIEXPORT jlong JNICALL Java_xyz_unlimitedcodeworks_operations_extra_Captioner_n_1create
(JNIEnv *env, jclass, jstring jVocabFile, jstring jLstmProto, jstring jModelBin,
 jboolean jUseGPU, jint jTaskId, jint jMaxGPUNum)
{
    if (jUseGPU && jTaskId != -1) {
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
        auto *ct = new Captioner(fromJString(env, jVocabFile),
                                 fromJString(env, jLstmProto),
                                 fromJString(env, jModelBin),
                                 jUseGPU);
        return reinterpret_cast<jlong>(ct);
    }
    catch (runtime_error *e)
    {
        throwJniException(env, e);
    }
    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_Captioner
 * Method:    n_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_Captioner_n_1delete
(JNIEnv *, jclass, jlong nativeObj)
{
    auto *ct = reinterpret_cast<Captioner*>(nativeObj);
    delete ct;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_Captioner
 * Method:    n_captioning
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_xyz_unlimitedcodeworks_operations_extra_Captioner_n_1captioning
(JNIEnv *env, jclass, jlong nativeObj, jlong frameFeatures_nativeObj)
{
    if (nativeObj) {
        auto &ct = *reinterpret_cast<Captioner*>(nativeObj);
        auto &frameFeatures = *reinterpret_cast<cv::Mat*>(frameFeatures_nativeObj);

        std::vector<std::vector<float>> frameFeatures_vv;
        try {
            Mat_to_vector_vector_float(frameFeatures, frameFeatures_vv);

            auto res = toJString(env, ct.runCaptioner(frameFeatures_vv));
            vector_vector_float_to_Mat(frameFeatures_vv, frameFeatures);

            return res;
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized Captioner object");
    }

    return toJString(env, "");
}
