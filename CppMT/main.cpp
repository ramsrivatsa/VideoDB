#include "ObjectTracking.h"

using namespace cmt;
using cv::VideoCapture;

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


  VideoCapture cap;

  cap.open(input_path);

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

  //Initialize CMT

  int frame = skip_frames;

  //Main loop
  while (true)
    {
      frame++;

      Mat im;

      cap >> im; //Else use next image in stream
      Tracker.trackImage(im);

      if (im.empty()) break; //Exit at end of video stream

    }

  return 0;
}
