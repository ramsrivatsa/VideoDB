#ifndef CMT_H

#define CMT_H

#include "Consensus.h"
#include "Fusion.h"
#include "Matcher.h"
#include "Tracker.h"

#include <opencv2/features2d/features2d.hpp>

#include <string>
#include <vector>

namespace cmt
{

class CMT
{
public:
    CMT() : str_detector("FAST"), str_descriptor("BRISK") {};
    void initialize(const cv::Mat &im_gray, const cv::Rect &rect);
    void processFrame(const cv::Mat &im_gray);

    Fusion fusion;
    Matcher matcher;
    Tracker tracker;
    Consensus consensus;

    std::string str_detector;
    std::string str_descriptor;

    std::vector<cv::Point2f> points_active; //public for visualization purposes
    cv::RotatedRect bb_rot;

private:
    cv::Ptr<cv::FeatureDetector> detector;
    cv::Ptr<cv::DescriptorExtractor> descriptor;

    cv::Size2f size_initial;

    std::vector<int> classes_active;

    float theta;

    cv::Mat im_prev;
};

} /* namespace CMT */

#endif /* end of include guard: CMT_H */
