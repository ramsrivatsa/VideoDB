#include "ForwardNetCaffe.h"
#include <stdexcept>

using namespace cv;

namespace ucw { namespace caffe {
    ForwardNet::ForwardNet(const string& model_file,
                           const string& trained_file,
                           const string& mean_file,
                           const bool CPU,
                           const bool featExtractor) {

        if (CPU) Caffe::set_mode(Caffe::CPU);
        else Caffe::set_mode(Caffe::GPU);
        extractor(featExtractor);

        clock_t modelTime = clock();
        /*  Load the network. */
        net_.reset(new Net<float>(model_file, TEST));
        net_->CopyTrainedLayersFrom(trained_file);
        modelTime = clock() - modelTime;
        std::cout << "Loading the model + trained: " << double(modelTime) / CLOCKS_PER_SEC * 1000 << "ms" << std::endl;

        clock_t cvTime = clock();
        CHECK_EQ(net_->num_inputs(), 1) << "Network should have exactly one input.";
        CHECK_EQ(net_->num_outputs(), 1) << "Network should have exactly one output.";

        Blob<float>* input_layer = net_->input_blobs()[0];
        num_channels_ = input_layer->channels();
        CHECK(num_channels_ == 3 || num_channels_ == 1)
          << "Input layer should have 1 or 3 channels.";
        input_geometry_ = cv::Size(input_layer->width(), input_layer->height());
        cvTime = clock() - cvTime;
        std::cout << "Using cv::Size and some other stuff: " << double(cvTime) / CLOCKS_PER_SEC * 1000 << "ms" << std::endl;

        clock_t meanTime = clock();
        /*  Load the binaryproto mean file. */
        SetMean(mean_file);
        meanTime = clock() - meanTime;
        std::cout << "Loading the mean file: " << double(meanTime) / CLOCKS_PER_SEC * 1000 << "ms" << std::endl;
    }

    /*  Load the mean file in binaryproto format. */
    void ForwardNet::SetMean(const string& mean_file) {
        BlobProto blob_proto;
        ReadProtoFromBinaryFileOrDie(mean_file.c_str(), &blob_proto);

        /*  Convert from BlobProto to Blob<float> */
        Blob<float> mean_blob;
        mean_blob.FromProto(blob_proto);
        CHECK_EQ(mean_blob.channels(), num_channels_)
          << "Number of channels of mean file doesn't match input layer.";

        /*  The format of the mean file is planar 32-bit float BGR or grayscale. */
        std::vector<cv::Mat> channels;
        float* data = mean_blob.mutable_cpu_data();
        for (int i = 0; i < num_channels_; ++i) {
            /*  Extract an individual channel. */
            cv::Mat channel(mean_blob.height(), mean_blob.width(), CV_32FC1, data);
            channels.push_back(channel);
            data += mean_blob.height() * mean_blob.width();
        }

        /*  Merge the separate channels into a single image. */
        cv::Mat mean;
        cv::merge(channels, mean);

        /*  Compute the global mean pixel value and create a mean image
         *     * filled with this value. */
        cv::Scalar channel_mean = cv::mean(mean);
        mean_ = cv::Mat(input_geometry_, mean.type(), channel_mean);
    }

    Mat ForwardNet::forward(const Mat& img) {
        std::vector<Mat> input;
        input.push_back(img);
        return forward(input).front();
    }

    std::vector<Mat> ForwardNet::forward(const std::vector<cv::Mat>& imgs) {
        Blob<float>* input_layer = net_->input_blobs()[0];
        input_layer->Reshape(imgs.size(), num_channels_,
                             input_geometry_.height, input_geometry_.width);
        /*  Forward dimension change to all layers. */
        net_->Reshape();

        for (size_t i = 0; i < imgs.size(); ++i) {
            std::vector<cv::Mat> input_channels;
            WrapInputLayer(&input_channels, i);
            Preprocess(imgs[i], &input_channels);
        }
        net_->Forward();

        std::vector<Mat> outputs;

        Blob<float>* output_layer = net_->output_blobs()[0];
        const float* begin;
        const float* end;

        if(extractor) {
            const shared_ptr<Blob<float> >& fc7Layer = net_->blob_by_name("fc7");
            begin = fc7Layer->cpu_data();
            end = begin + fc7Layer->shape(1);
        }
        else {
            for (int i = 0; i < output_layer->num(); ++i) {
                begin = output_layer->cpu_data() + i * output_layer->channels();
                end = begin + output_layer->channels();
                /*  Copy the output layer to a std::vector */
            }
        }
        std::vector<float> result(begin, end);
        if (result.size() == 0) {
            throw std::runtime_error("Caffe forward pass returned empty result");
        }
        cv::Mat mat(result,true);
        Mat probMat = mat.reshape(1,1);

        if (probMat.elemSize() * probMat.cols * probMat.rows == 0) {
            throw std::runtime_error("Caffe forward pass result converted to empty cv::Mat");
        }

        outputs.push_back(probMat);

        return outputs;
    }

    /*  Wrap the input layer of the network in separate cv::Mat objects
     *   * (one per channel). This way we save one memcpy operation and we
     *    * don't need to rely on cudaMemcpy2D. The last preprocessing
     *     * operation will write the separate channels directly to the input
     *      * layer. */
    void ForwardNet::WrapInputLayer(std::vector<cv::Mat>* input_channels, int n) {
        Blob<float>* input_layer = net_->input_blobs()[0];

        int width = input_layer->width();
        int height = input_layer->height();
        int channels = input_layer->channels();
        float* input_data = input_layer->mutable_cpu_data() + n * width * height * channels;
        for (int i = 0; i < channels; ++i) {
            cv::Mat channel(height, width, CV_32FC1, input_data);
            input_channels->push_back(channel);
            input_data += width * height;
        }
    }

    void ForwardNet::Preprocess(const cv::Mat& img,
                                std::vector<cv::Mat>* input_channels) {
        /*  Convert the input image to the input image format of the network. */
        cv::Mat sample;
        if (img.channels() == 3 && num_channels_ == 1)
          cv::cvtColor(img, sample, cv::COLOR_BGR2GRAY);
        else if (img.channels() == 4 && num_channels_ == 1)
          cv::cvtColor(img, sample, cv::COLOR_BGRA2GRAY);
        else if (img.channels() == 4 && num_channels_ == 3)
          cv::cvtColor(img, sample, cv::COLOR_BGRA2BGR);
        else if (img.channels() == 1 && num_channels_ == 3)
          cv::cvtColor(img, sample, cv::COLOR_GRAY2BGR);
        else
          sample = img;

        cv::Mat sample_resized;
        if (sample.size() != input_geometry_)
          cv::resize(sample, sample_resized, input_geometry_);
        else
          sample_resized = sample;

        cv::Mat sample_float;
        if (num_channels_ == 3)
          sample_resized.convertTo(sample_float, CV_32FC3);
        else
          sample_resized.convertTo(sample_float, CV_32FC1);

        cv::Mat sample_normalized;
        cv::subtract(sample_float, mean_, sample_normalized);

        cv::split(sample_normalized, *input_channels);

    }
}
}

//NOTE: comment this block out as a standalone app to test
/*  
    using namespace ucw; 

    int main(int argc, char** argv) {
    if (argc < 6) {
    std::cerr << "Usage: " << argv[0]
    << " deploy.prototxt network.caffemodel"
    << " mean.binaryproto labels.txt img.jpg" << std::endl;
    return 1;
    }

    ::google::InitGoogleLogging(argv[0]);

    string model_file   = argv[1];
    string trained_file = argv[2];
    string mean_file    = argv[3];
    string label_file   = argv[4];
    clock_t time = clock();
    ForwardNet classifier(model_file, trained_file, mean_file, label_file, false);
    time = clock() - time;
    std::cout << "Constructing: " << double(time) / CLOCKS_PER_SEC * 1000 << "ms" << std::endl;

    clock_t imgTime = clock();
    std::vector<cv::Mat> imgs;
    for (int i = 5; i < argc; ++i)
    {
    cv::Mat img = cv::imread(argv[i], -1);
    CHECK(!img.empty()) << "Unable to decode image " << argv[i];
    imgs.push_back(img);
    }
    imgTime = clock() - imgTime;
    std::cout << "Loading images: " << double(imgTime) / CLOCKS_PER_SEC * 1000 << "ms" << std::endl;

    clock_t elapsed = clock();
    if(argc == 6) classifier.forward(imgs[0]);
    else classifier.forward(imgs);
    elapsed = clock() - elapsed;
    std::cout << "Computation: " << double(elapsed) / CLOCKS_PER_SEC * 1000 << "ms" << std::endl;

    }
    */
