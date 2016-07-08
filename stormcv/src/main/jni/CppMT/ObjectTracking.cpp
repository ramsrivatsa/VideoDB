#include "ObjectTracking.h"

#include <opencv2/imgproc.hpp>

using cv::Mat;
using cv::Rect;
using cv::RotatedRect;
using std::string;

namespace cmt {
    //Pass in the location and the object to be tracked 
    ObjectTracking::ObjectTracking(const int x, 
                                   const int y, 
                                   const int width, 
                                   const int height, 
                                   const Mat &input) {
        rect = Rect(x,y,width,height);
        im0 = convertToGray(input);
        cmt.initialize(im0, rect);
    }

    void ObjectTracking::trackImage(const Mat &im) {
        Mat im_gray = convertToGray(im);
        cmt.processFrame(im_gray);
    }

    Mat ObjectTracking::convertToGray(const Mat &in) {
        Mat out;
        if (in.channels() > 1)
          cv::cvtColor(in, out, cv::COLOR_BGR2GRAY);
        else 
          out = in;
        return out;
    }

    RotatedRect ObjectTracking::currentPosition() const {
        return cmt.bb_rot;
    }

}

/*  
    using namespace cmt;

    int main(int argc, char **argv)
    {
//Create a CMT object
CMT cmt;

//Initialization bounding box
Rect rect;

//Parse args
int skip_frames = 0;
int verbose_flag = 0;
string input_path = "test.avi";

rect = Rect(128,49,13,17);
//Set up logging
FILELog::ReportingLevel() = verbose_flag ? logDEBUG : logINFO;
Output2FILE::Stream() = stdout; //Log to stdout

//Normal mode

VideoCapture cap;

//  bool show_preview = true;

//Else open the video specified by input_path
cap.open(input_path);


// Now which frame are we on?
skip_frames = (int) cap.get(CV_CAP_PROP_POS_FRAMES);
std::cout << skip_frames << std::endl;

//If it doesn't work, stop
if(!cap.isOpened())
{
cerr << "Unable to open video capture." << endl;
return -1;
}

//Get initial image
Mat im0;
cap >> im0;
ObjectTracking Tracker(128,49,13,17,input_path,im0);

FILE_LOG(logINFO) << "Using " << rect.x << "," << rect.y << "," << rect.width << "," << rect.height
<< " as initial bounding box.";

//Convert im0 to grayscale
Mat im0_gray;
if (im0.channels() > 1) {
cvtColor(im0, im0_gray, CV_BGR2GRAY);
} else {
im0_gray = im0;
}

//Initialize CMT
cmt.initialize(im0_gray, rect);

int frame = skip_frames;

//Main loop
while (true)
{
frame++;

Mat im;

cap >> im; //Else use next image in stream
Tracker.process(im);

if (im.empty()) break; //Exit at end of video stream

Mat im_gray;
if (im.channels() > 1) {
    cvtColor(im, im_gray, CV_BGR2GRAY);
} else {
    im_gray = im;
}

//      FILE_LOG(logINFO) << "#" << frame << " active: " << cmt.points_active.size();

//Let CMT process the frame
cmt.processFrame(im_gray);

}


return 0;
}
*/
