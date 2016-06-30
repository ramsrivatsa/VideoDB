#include <string>
#include <stdexcept>
#include <jni.h>

#include "jni_helper.h"
#include "xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX.h"

#include "FeatureDetectorX.h"
#include "cv/converters.h"

using namespace std;
using namespace ucw;

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_empty
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1empty
(JNIEnv *env, jclass, jlong nativeObj)
{
    if (nativeObj) {
        auto &fd = *reinterpret_cast<FeatureDetectorX*>(nativeObj);

        try {
            return fd.empty();
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized FeatureDetectorX object");
    }
    return true;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_create
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1create
(JNIEnv *env, jclass, jint jDetectorType)
{
    try
    {
        auto *fd = FeatureDetectorX::create(jDetectorType);
        return reinterpret_cast<jlong>(fd);
    }
    catch (runtime_error *e)
    {
        throwJniException(env, e);
    }
    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_detect_0
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1detect_10__JJJJ
(JNIEnv *env, jclass, jlong nativeObj, jlong img_nativeObj, jlong keypoints_nativeObj, jlong mask_nativeObj)
{
    if (nativeObj) {
        auto &fd = *reinterpret_cast<FeatureDetectorX*>(nativeObj);
        auto &img = *reinterpret_cast<cv::Mat*>(img_nativeObj);
        auto &keypoints = *reinterpret_cast<cv::Mat*>(keypoints_nativeObj);
        auto &mask = *reinterpret_cast<cv::Mat*>(mask_nativeObj);

        std::vector<KeyPoint> keypoints_vec;
        try {
            fd.detect(img, keypoints_vec, mask);
            vector_KeyPoint_to_Mat(keypoints_vec, keypoints);
            return;
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized FeatureDetectorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_detect_0
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1detect_10__JJJ
(JNIEnv *env, jclass, jlong nativeObj, jlong img_nativeObj, jlong keypoints_nativeObj)
{
    if (nativeObj) {
        auto &fd = *reinterpret_cast<FeatureDetectorX*>(nativeObj);
        auto &img = *reinterpret_cast<cv::Mat*>(img_nativeObj);
        auto &keypoints = *reinterpret_cast<cv::Mat*>(keypoints_nativeObj);

        std::vector<KeyPoint> keypoints_vec;
        try {
            fd.detect(img, keypoints_vec);
            vector_KeyPoint_to_Mat(keypoints_vec, keypoints);
            return;
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized FeatureDetectorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_detect_1
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1detect_11__JJJJ
(JNIEnv *env, jclass, jlong nativeObj, jlong imgs_nativeObj, jlong keypoints_nativeObj, jlong masks_nativeObj)
{
    if (nativeObj) {
        auto &fd = *reinterpret_cast<FeatureDetectorX*>(nativeObj);
        auto &imgs = *reinterpret_cast<cv::Mat*>(imgs_nativeObj);
        auto &keypoints = *reinterpret_cast<cv::Mat*>(keypoints_nativeObj);
        auto &masks = *reinterpret_cast<cv::Mat*>(masks_nativeObj);

        std::vector<cv::Mat> imgs_vec;
        std::vector<std::vector<KeyPoint>> keypoints_vec;
        std::vector<cv::Mat> masks_vec;
        try {
            Mat_to_vector_Mat(imgs, imgs_vec);
            Mat_to_vector_Mat(masks, masks_vec);

            fd.detect(imgs_vec, keypoints_vec, masks_vec);

            vector_vector_KeyPoint_to_Mat(keypoints_vec, keypoints);
            return;
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized FeatureDetectorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_detect_1
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1detect_11__JJJ
(JNIEnv *env, jclass, jlong nativeObj, jlong imgs_nativeObj, jlong keypoints_nativeObj)
{
    if (nativeObj) {
        auto &fd = *reinterpret_cast<FeatureDetectorX*>(nativeObj);
        auto &imgs = *reinterpret_cast<cv::Mat*>(imgs_nativeObj);
        auto &keypoints = *reinterpret_cast<cv::Mat*>(keypoints_nativeObj);

        std::vector<cv::Mat> imgs_vec;
        std::vector<std::vector<KeyPoint>> keypoints_vec;
        try {
            Mat_to_vector_Mat(imgs, imgs_vec);

            fd.detect(imgs_vec, keypoints_vec);

            vector_vector_KeyPoint_to_Mat(keypoints_vec, keypoints);
            return;
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized FeatureDetectorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_read
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1read
(JNIEnv *env, jclass, jlong nativeObj, jstring jFileName)
{
    if (nativeObj) {
        auto &fd = *reinterpret_cast<FeatureDetectorX*>(nativeObj);

        try {
            fd.read(fromJString(env, jFileName));
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized FeatureDetectorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_write
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1write
(JNIEnv *env, jclass, jlong nativeObj, jstring jFileName)
{
    if (nativeObj) {
        auto &fd = *reinterpret_cast<FeatureDetectorX*>(nativeObj);

        try {
            fd.write(fromJString(env, jFileName));
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized FeatureDetectorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX
 * Method:    n_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_FeatureDetectorX_n_1delete
(JNIEnv *, jclass, jlong nativeObj)
{
    auto *fd = reinterpret_cast<FeatureDetectorX*>(nativeObj);
    delete fd;
}
