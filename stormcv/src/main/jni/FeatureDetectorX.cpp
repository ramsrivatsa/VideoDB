#include "FeatureDetectorX.h"

#include <opencv2/xfeatures2d.hpp>

using namespace std;
using cv::FileStorage;
using cv::xfeatures2d::SIFT;
using cv::xfeatures2d::SURF;

namespace ucw {
void FeatureDetectorX::detect(const Mat& image, std::vector<KeyPoint>& keypoints,
                              const Mat& mask) const
{
    return wrapped->detect(image, keypoints, mask);
}

void FeatureDetectorX::detect(const vector<Mat>& images, vector<vector<KeyPoint> >& keypoints,
            const vector<Mat>& masks) const
{
    return wrapped->detect(images, keypoints, masks);
}

bool FeatureDetectorX::empty() const
{
    return wrapped->empty();
}

FeatureDetectorX* FeatureDetectorX::create(int detectorType)
{
    Ptr<FeatureDetector> fd;
    switch(detectorType)
    {
    //case STAR:
    //    fd = xfeatures2d::StarDetector::create();
    //    break;
    case SIFT:
        fd = SIFT::create();
        break;
    case SURF:
        fd = SURF::create();
        break;
    default:
        throw std::runtime_error("Unsupported feature detector type.");
        break;
    }

    return new FeatureDetectorX(fd);
}

void FeatureDetectorX::write(const String& fileName) const
{
    FileStorage fs(fileName, FileStorage::WRITE);
    wrapped->write(fs);
}

void FeatureDetectorX::read(const String& fileName)
{
    FileStorage fs(fileName, FileStorage::READ);
    wrapped->read(fs.root());
}

FeatureDetectorX::FeatureDetectorX(Ptr<FeatureDetector> _wrapped)
    : wrapped(_wrapped)
{}

} // namespace ucw