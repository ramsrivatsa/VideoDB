#include "jni_helper.h"

using namespace std;

namespace ucw {

jint throwJniError(JNIEnv *env, const char *message)
{
    static const char *className = "nl/tno/stormcv/util/JniUtils$JNIError";

    jclass exClass = env->FindClass(className);
    if (exClass == NULL) {
        env->FatalError("Exception class not found");
    }

    return env->ThrowNew(exClass, message);
}

jint throwSomething(JNIEnv *env, const char *className, const char *message)
{
    jclass exClass = env->FindClass(className);
    if (exClass == NULL) {
        return throwJniError(env, "Exception class not found");
    }

    return env->ThrowNew(exClass, message);
}

jint throwFileNotFoundException(JNIEnv *env, const char *message)
{
    static const char *className = "java/io/FileNotFoundException";

    return throwSomething(env, className, message);
}

jint throwJniException(JNIEnv *env, const std::exception *e)
{
    static const char *className = "nl/tno/stormcv/util/JniUtils$JNIException";

    string what = "unknown exception";

    if(e) {
        string exception_type = "std::exception";

        if(dynamic_cast<const JniException*>(e)) {
            exception_type = "JniException";
        }

        what = exception_type + ": " + e->what();
    }

    return throwSomething(env, className, what.c_str());
}

string fromJString(JNIEnv *env, jstring jstr)
{
    if (jstr)
    {
        const char * c_str = env->GetStringUTFChars(jstr, NULL);
        if (c_str)
        {
            return string(c_str);
        }
    }
    return string();
}

} // namespace ucw