#ifndef JNI_HELPER_H
#define JNI_HELPER_H

#include <string>
#include <stdexcept>
#include <jni.h>

namespace ucw {

    class JniException : public std::runtime_error
    {
    public:
        JniException(const std::string &what_arg) : runtime_error(what_arg) {}
    };

    jint throwJniError(JNIEnv *env, const char *message);

    jint throwSomething(JNIEnv *env, const char *className, const char *message);

    jint throwFileNotFoundException(JNIEnv *env, const char *message);

    jint throwJniException(JNIEnv *env, const std::exception *e = nullptr);

    std::string fromJString(JNIEnv *env, jstring jstr);

} // namespace ucw

#endif // JNI_HELPER_H