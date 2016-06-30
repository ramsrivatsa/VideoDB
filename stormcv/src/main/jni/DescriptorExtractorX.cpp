#include "DescriptorExtractorX.h"

#include <opencv2/xfeatures2d.hpp>

using namespace std;
using cv::FileStorage;
using cv::xfeatures2d::SIFT;
using cv::xfeatures2d::SURF;

namespace ucw {
void DescriptorExtractorX::compute(const Mat& image,
                                   vector<KeyPoint>& keypoints,
                                   Mat& descriptors) const
{
    return wrapped->compute(image, keypoints, descriptors);
}

void DescriptorExtractorX::compute(const vector<Mat>& images,
                                   vector<vector<KeyPoint> >& keypoints,
                                   vector<Mat>& descriptors) const
{
    return wrapped->compute(images, keypoints, descriptors);
}

int DescriptorExtractorX::descriptorSize() const
{
    return wrapped->descriptorSize();
}

int DescriptorExtractorX::descriptorType() const
{
    return wrapped->descriptorType();
}

bool DescriptorExtractorX::empty() const
{
    return wrapped->empty();
}

//supported SIFT, SURF, ORB, BRIEF, BRISK, FREAK, AKAZE, Opponent(XXXX)
//not supported: Calonder
DescriptorExtractorX* DescriptorExtractorX::create(int extractorType)
{
    Ptr<DescriptorExtractor> de;
    switch(extractorType)
    {
    case SIFT:
        de = SIFT::create();
        break;
    case SURF:
        de = SURF::create();
        break;
    default:
        throw runtime_error("Unsupported descriptor extractor type.");
        break;
    }

    return new DescriptorExtractorX(de);
}

void DescriptorExtractorX::write(const String& fileName) const
{
    FileStorage fs(fileName, FileStorage::WRITE);
    wrapped->write(fs);
}

void DescriptorExtractorX::read(const String& fileName)
{
    FileStorage fs(fileName, FileStorage::READ);
    wrapped->read(fs.root());
}

DescriptorExtractorX::DescriptorExtractorX(Ptr<DescriptorExtractor> _wrapped)
    : wrapped(_wrapped)
{}

} // end of namespace ucw
