#ifndef IFORWARDNET_H
#define IFORWARDNET_H

#include <opencv2/core/core.hpp>

#include <vector>

namespace ucw {
class IForwardNet {
public:
    std::vector<cv::Mat> forward(const std::vector<cv::Mat>& imgs) = 0;

    cv::Mat forward(const cv::Mat &input) = 0;

    virtual ~IForwardNet() {}
};
}

#endif // IFORWARDNET_H
