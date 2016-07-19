#ifndef FORWARDNET_CAFFE_H
#define FORWARDNET_CAFFE_H

#include "IForwardNet.h"

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include <caffe/caffe.hpp>

#include <algorithm>
#include <iosfwd>
#include <memory>
#include <utility>
#include <vector>
#include <stdexcept>

using std::string;
using namespace caffe;  // NOLINT(build/namespaces)

namespace ucw { namespace caffe {
    class ForwardNet : public IForwardNet {
    public:
      ForwardNet(const string& model_file,
                 const string& trained_file,
                 const string& mean_file,
                 bool CPU = true,
                 bool featExtractor = false);
      ~ForwardNet() override {}

      void SetMean(const string& mean_file);

      std::vector<cv::Mat> forward(const std::vector<cv::Mat>& imgs) override;

      cv::Mat forward(const cv::Mat &input) override;

      void WrapInputLayer(std::vector<cv::Mat>* input_channels, int n);

      void Preprocess(const cv::Mat& img,
                      std::vector<cv::Mat>* input_channels);

      static void setPriority(int priority = 0);

      static long getCurrentTid();

    private:
      shared_ptr<Net<float> > net_;
      bool extractor;
      cv::Size input_geometry_;
      int num_channels_;
      cv::Mat mean_;
    };
}
}

#endif // FORWARDNET_CAFFE_H
