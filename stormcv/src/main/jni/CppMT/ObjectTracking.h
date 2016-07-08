#ifndef OBJECTTRACKING_H 
#define OBJECTTRACKING_H


#include "CMT.h"

#include <opencv2/core.hpp>

namespace cmt {
    class ObjectTracking {
    public:
      //Pass in the location and the object to be tracked 
      ObjectTracking(const int x, 
                     const int y, 
                     const int width, 
                     const int height, 
                     const cv::Mat &input);
      ~ObjectTracking() {};

      void trackImage(const cv::Mat &im);

      cv::RotatedRect currentPosition() const;

      static cv::Mat convertToGray(const cv::Mat &in);

    private:
      CMT cmt;
      cv::Rect rect;
      cv::Mat im0;
    };
}

#endif
