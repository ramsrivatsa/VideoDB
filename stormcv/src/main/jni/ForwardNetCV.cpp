#include "ForwardNetCV.h"

#include <string>
#include <sstream>
#include <stdexcept>

#include <opencv2/dnn.hpp>
#include <opencv2/imgproc.hpp>

using namespace std;
using namespace cv;
using namespace cv::dnn;

namespace ucw { namespace opencv {

    ForwardNet::ForwardNet(const string &modelTxt, const string &modelBin,
                           const string &outputName)
        : outputBlobName(outputName)
    {
        if (outputBlobName.empty()) {
            outputBlobName = "prob"; // backward compatibale
        }
        Ptr<dnn::Importer> importer;
        try {
            importer = dnn::createCaffeImporter(modelTxt, modelBin);
        } catch (const cv::Exception &err) {
            ostringstream oss;
            oss << "Can't load network by using the following files: " << endl;
            oss << "prototxt:   " << modelTxt << endl;
            oss << "caffemodel: " << modelBin << endl;
            oss << "Underlaying exception: " << err.msg << endl;
            throw runtime_error(oss.str());
        }

        importer->populateNet(net);
    }

    Mat ForwardNet::forward(const cv::Mat &input)
    {
        if (input.empty()) {
            return Mat();
        }

        Size acceptedSize(224, 224);
        Mat img = input;
        if (img.size() != acceptedSize) {
            //GoogLeNet accepts only 224x224 RGB-images
            img = img.clone();
            resize(img, img, Size(224, 224));
        }

        dnn::Blob inputBlob(img);   //Convert Mat to dnn::Blob image batch

        // TODO: input and output layer name should be set upon model loading
        net.setBlob(".data", inputBlob);        //set the network input
        net.forward();                          //compute output
        // auto outputBlob = net.getBlob("prob");   //gather output of "prob" layer
        auto outputBlob = net.getBlob(outputBlobName); //gather output of "prob" layer

        return outputBlob.matRefConst().reshape(1, 1);
    }

    vector<Mat> ForwardNet::forward(const vector<Mat>& imgs)
    {
        vector<Mat> res;
        res.reserve(imgs.size());
        for (auto img : imgs) {
            res.push_back(forward(img));
        }
        return res;
    }

} // namespace opencv
} // namespace ucw
