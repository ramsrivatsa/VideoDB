#ifndef FORWARDNET_H
#define FORWARDNET_H

#include <string>

#include <opencv2/core.hpp>
#include <opencv2/dnn.hpp>

namespace ucw {

    class ForwardNet
      {
        cv::dnn::Net net;

    public:
        ForwardNet(const std::string &modelTxt, const std::string &modelBin);

        std::pair<uint32_t, double> processImage(cv::Mat &frame);

        cv::Mat forward(const cv::Mat &input);

        static void setPriority(int priority = 0);

        static long getCurrentTid();
      };

}
#endif // FORWARDNET_H
