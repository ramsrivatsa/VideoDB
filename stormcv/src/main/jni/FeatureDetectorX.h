#ifndef FEATUREDETECTORX_H
#define FEATUREDETECTORX_H

#include <opencv2/core.hpp>
#include <opencv2/features2d.hpp>

#include <vector>

using cv::FeatureDetector;
using cv::Mat;
using cv::KeyPoint;
using cv::Ptr;
using cv::String;

namespace ucw {

class  FeatureDetectorX
{
public:
    void detect(const Mat& image, std::vector<KeyPoint>& keypoints,
                const Mat& mask = Mat()) const;

    void detect(const std::vector<Mat>& images, std::vector<std::vector<KeyPoint> >& keypoints,
                const std::vector<Mat>& masks = std::vector<Mat>()) const;

    bool empty() const;

    enum
    {
        SIFT          = 3,
        SURF          = 4,
    };

    static FeatureDetectorX* create(int detectorType);

    void write(const String& fileName) const;

    void read(const String& fileName);

private:
    FeatureDetectorX(Ptr<FeatureDetector> _wrapped);

    Ptr<FeatureDetector> wrapped;
};

} // namespace ucw

#endif // FEATUREDETECTORX_H