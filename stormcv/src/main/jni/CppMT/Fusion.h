#ifndef FUSION_H
#define FUSION_H

#include <vector>
#include <opencv2/core.hpp>

namespace cmt {

class Fusion
{
public:
    void preferFirst(const std::vector<cv::Point2f> & firstPoints,
                     const std::vector<int> & firstClasses,
                     const std::vector<cv::Point2f> & secondPoints,
                     const std::vector<int> & secondClasses,
                     std::vector<cv::Point2f> & fusedPoints, std::vector<int> & fusedClasses);
};

} /* namespace CMT */

#endif /* end of include guard: FUSION_H */
