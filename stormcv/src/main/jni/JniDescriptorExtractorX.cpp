#include <string>
#include <stdexcept>
#include <jni.h>

#include "jni_helper.h"
#include "xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX.h"

#include "DescriptorExtractorX.h"
#include "cv/converters.h"

using namespace std;
using namespace ucw;

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_empty
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1empty
(JNIEnv *env, jclass, jlong nativeObj)
{
    if (nativeObj) {
        auto &de = *reinterpret_cast<DescriptorExtractorX*>(nativeObj);

        try {
            return de.empty();
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized DescriptorExtractorX object");
    }
    return true;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_descriptorSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1descriptorSize
(JNIEnv *env, jclass, jlong nativeObj)
{
    if (nativeObj) {
        auto &de = *reinterpret_cast<DescriptorExtractorX*>(nativeObj);

        try {
            return de.descriptorSize();
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized DescriptorExtractorX object");
    }
    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_descriptorType
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1descriptorType
(JNIEnv *env, jclass, jlong nativeObj)
{
    if (nativeObj) {
        auto &de = *reinterpret_cast<DescriptorExtractorX*>(nativeObj);

        try {
            return de.descriptorType();
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized DescriptorExtractorX object");
    }
    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_create
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1create
(JNIEnv *env, jclass, jint jExtractorType)
{
    try
    {
        auto *de = DescriptorExtractorX::create(jExtractorType);
        return reinterpret_cast<jlong>(de);
    }
    catch (runtime_error *e)
    {
        throwJniException(env, e);
    }
    return 0;
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_compute_0
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1compute_10
(JNIEnv *env, jclass, jlong nativeObj, jlong img_nativeObj, jlong keypoints_nativeObj, jlong descriptors_nativeObj)
{
    if (nativeObj) {
        auto &de = *reinterpret_cast<DescriptorExtractorX*>(nativeObj);
        auto &img = *reinterpret_cast<cv::Mat*>(img_nativeObj);
        auto &keypoints = *reinterpret_cast<cv::Mat*>(keypoints_nativeObj);
        auto &descriptors = *reinterpret_cast<cv::Mat*>(descriptors_nativeObj);

        std::vector<KeyPoint> keypoints_vec;
        try {
            Mat_to_vector_KeyPoint(keypoints, keypoints_vec);
            de.compute(img, keypoints_vec, descriptors);
            vector_KeyPoint_to_Mat(keypoints_vec, keypoints);
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized DescriptorExtractorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_compute_1
 * Signature: (JJJJ)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1compute_11
(JNIEnv *env, jclass, jlong nativeObj, jlong imgs_nativeObj, jlong keypoints_nativeObj, jlong descriptors_nativeObj)
{
    if (nativeObj) {
        auto &de = *reinterpret_cast<DescriptorExtractorX*>(nativeObj);
        auto &imgs = *reinterpret_cast<cv::Mat*>(imgs_nativeObj);
        auto &keypoints = *reinterpret_cast<cv::Mat*>(keypoints_nativeObj);
        auto &descriptors = *reinterpret_cast<cv::Mat*>(descriptors_nativeObj);

        std::vector<cv::Mat> imgs_vec;
        std::vector<std::vector<cv::KeyPoint>> keypoints_vec;
        std::vector<cv::Mat> descriptors_vec;
        try {
            Mat_to_vector_Mat(imgs, imgs_vec);
            Mat_to_vector_vector_KeyPoint(keypoints, keypoints_vec);
            de.compute(imgs_vec, keypoints_vec, descriptors_vec);
            vector_Mat_to_Mat(descriptors_vec, descriptors);
            vector_vector_KeyPoint_to_Mat(keypoints_vec, keypoints);
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized DescriptorExtractorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_read
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1read
(JNIEnv *env, jclass, jlong nativeObj, jstring jFileName)
{
    if (nativeObj) {
        auto &de = *reinterpret_cast<DescriptorExtractorX*>(nativeObj);

        try {
            de.read(fromJString(env, jFileName));
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized DescriptorExtractorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_write
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1write
(JNIEnv *env, jclass, jlong nativeObj, jstring jFileName)
{
    if (nativeObj) {
        auto &de = *reinterpret_cast<DescriptorExtractorX*>(nativeObj);

        try {
            de.write(fromJString(env, jFileName));
        } catch (runtime_error *e) {
            throwJniException(env, e);
        }
    } else {
        throwJniError(env, "Method called on an uninitialized DescriptorExtractorX object");
    }
}

/*
 * Class:     xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX
 * Method:    n_delete
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_xyz_unlimitedcodeworks_operations_extra_DescriptorExtractorX_n_1delete
(JNIEnv *, jclass, jlong nativeObj)
{
    auto *de = reinterpret_cast<DescriptorExtractorX*>(nativeObj);
    delete de;
}