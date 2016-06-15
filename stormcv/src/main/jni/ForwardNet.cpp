#include "ForwardNet.h"

#include <string>
#include <sstream>
#include <stdexcept>

#include <opencv2/dnn.hpp>
#include <opencv2/imgproc.hpp>

#include <errno.h>
#include <sched.h>
#include <string.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <sys/types.h>


using namespace std;
using namespace cv;
using namespace cv::dnn;

namespace ucw {

ForwardNet::ForwardNet(const string &modelTxt, const string &modelBin)
{
    Ptr<dnn::Importer> importer;
    try {
        importer = dnn::createCaffeImporter(modelTxt, modelBin);
    } catch (const cv::Exception &err) {
        ostringstream oss;
        oss << "Can't load network by using the following files: " << endl;
        oss << "prototxt:   " << modelTxt << endl;
        oss << "caffemodel: " << modelBin << endl;
        oss << "Underlaying exception: " << err.msg << endl;
        throw runtime_error(oss.str());
    }

    importer->populateNet(net);
}

Mat ForwardNet::forward(const cv::Mat &input)
{
    if (input.empty()) {
        return Mat();
    }

    Size acceptedSize(224, 224);
    Mat img = input;
    if (img.size() != acceptedSize) {
        //GoogLeNet accepts only 224x224 RGB-images
        img = img.clone();
        resize(img, img, Size(224, 224));
    }

    dnn::Blob inputBlob(img);   //Convert Mat to dnn::Blob image batch

    // TODO: input and output layer name should be set upon model loading
    net.setBlob(".data", inputBlob);        //set the network input
    net.forward();                          //compute output
    auto outputBlob = net.getBlob("prob");   //gather output of "prob" layer

    return outputBlob.matRefConst().reshape(1, 1);
}

void ForwardNet::setPriority(int priority)
{
    static char buf[512];
    sched_param param;
    param.sched_priority = priority;
    pid_t tid;

    tid = syscall(SYS_gettid);

    int sched_policy = SCHED_RR;
    if (priority == 0) {
        sched_policy = SCHED_OTHER;
    }

    if (sched_setscheduler(tid, sched_policy, &param) == -1) {
        int errsv = errno;
        const char *msg = strerror_r(errsv, buf, 512);
        throw runtime_error(msg);
    }
}

long ForwardNet::getCurrentTid()
{
    pid_t tid = syscall(SYS_gettid);
    return tid;
}

} // namespace ucw
