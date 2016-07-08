#ifndef TRACKER_H
#define TRACKER_H

#include <opencv2/core.hpp>
#include <vector>

namespace cmt {

class Tracker
{
public:
    Tracker() : thr_fb(30) {};
    void track(const cv::Mat &im_prev, const cv::Mat &im_gray,
               const std::vector<cv::Point2f> & points_prev,
               std::vector<cv::Point2f> & points_tracked, std::vector<unsigned char> & status);

private:
    float thr_fb;
};

} /* namespace CMT */

#endif /* end of include guard: TRACKER_H */
