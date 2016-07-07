#ifndef OBJECTTRACKING_H 
#define OBJECTTRACKING_H

#include "CMT.h"

#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include <iostream>
#include <fstream>
#include <sstream>
#include <cstdio>

using cmt::CMT;
using cv::imread;
using cv::Scalar;
using std::cerr;
using std::cout;
using std::endl;
using ::atof;

namespace cmt {
    class ObjectTracking {
    public:
      //Pass in the location and the object to be tracked 
      ObjectTracking(const int x, 
                     const int y, 
                     const int width, 
                     const int height, 
                     const string input_path,
                     const Mat input);
      ~ObjectTracking() {};

      void trackImage(Mat im);

      void printCoords();

      Mat convertToGray(Mat in);

    private:
      CMT cmt;
      Rect rect;
      Mat im0;
    };
}

#endif
