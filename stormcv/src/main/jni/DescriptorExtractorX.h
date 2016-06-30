#ifndef DESCRIPTOREXTRACTORX_H
#define DESCRIPTOREXTRACTORX_H

#include <opencv2/core.hpp>
#include <opencv2/features2d.hpp>

#include <vector>

using cv::DescriptorExtractor;
using cv::Mat;
using cv::KeyPoint;
using cv::Ptr;
using cv::String;

namespace ucw {
class DescriptorExtractorX
{
public:
    void compute(const Mat& image,
                 std::vector<KeyPoint>& keypoints, Mat& descriptors) const;

    void compute(const std::vector<Mat>& images,
                 std::vector<std::vector<KeyPoint> >& keypoints,
                 std::vector<Mat>& descriptors) const;

    int descriptorSize() const;

    int descriptorType() const;

    bool empty() const;

    enum
    {
        SIFT  = 1,
        SURF  = 2,
    };

    static DescriptorExtractorX* create(int extractorType);

    void write(const String& fileName) const;

    void read(const String& fileName);

private:
    DescriptorExtractorX(Ptr<DescriptorExtractor> _wrapped);

    Ptr<DescriptorExtractor> wrapped;
};

} // end of namespace ucw

#endif // DESCRIPTOREXTRACTORX_H