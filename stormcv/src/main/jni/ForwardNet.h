#ifndef FORWARDNET_H
#define FORWARDNET_H

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

#include <errno.h>
#include <sched.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <sys/types.h>

using std::string;
using namespace caffe;  // NOLINT(build/namespaces)

namespace ucw {
    class ForwardNet {
    public:
      ForwardNet(const string& model_file,
                 const string& trained_file,
                 const string& mean_file,
                 const string& label_file, const bool CPU = true);

      void SetMean(const string& mean_file);

      void setGPU();

      std::vector<cv::Mat> forward(const std::vector<cv::Mat>& imgs);

      cv::Mat forward(const cv::Mat &input);

      void WrapInputLayer(std::vector<cv::Mat>* input_channels, int n);

      void Preprocess(const cv::Mat& img,
                      std::vector<cv::Mat>* input_channels);

      static void setPriority(int priority = 0);

      static long getCurrentTid();

    private:
      shared_ptr<Net<float> > net_;
      cv::Size input_geometry_;
      int num_channels_;
      cv::Mat mean_;
      std::vector<string> labels_;
    };
}

#endif
