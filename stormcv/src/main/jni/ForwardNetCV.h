#ifndef FORWARDNET_CV_H
#define FORWARDNET_CV_H

#include "IForwardNet.h"

#include <opencv2/core.hpp>
#include <opencv2/dnn.hpp>

#include <string>

namespace ucw { namespace opencv {

class ForwardNet : public IForwardNet
{
    cv::dnn::Net net;

public:
    ForwardNet(const std::string &modelTxt, const std::string &modelBin,
               const std::string &outputName = "");
    ~ForwardNet() override {}

    cv::Mat forward(const cv::Mat &input) override;

    std::vector<cv::Mat> forward(const std::vector<cv::Mat>& imgs) override;

private:
    std::string outputBlobName;
};

}
}
#endif // FORWARDNET_CV_H
