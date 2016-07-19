/*
 * =====================================================================================
 *
 *       Filename:  main.cpp
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  07/11/2016 04:40:37 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  Linh Nguyen (peifeng), lvnguyen@umich.edu
 *        Company:  
 *
 * =====================================================================================
 */

#include "s2vt.h"
#include <sstream>

namespace ucw {
    Captioner::Captioner(const string& VOCAB_FILE,
                         const string& LSTM_NET_FILE,
                         const string& MODEL_FILE, bool useGPU) {
        if (useGPU)
            Caffe::set_mode(Caffe::GPU);
        else
            Caffe::set_mode(Caffe::CPU);

        lstmNet.reset(new Net<float>(LSTM_NET_FILE, TEST));
        lstmNet->CopyTrainedLayersFrom(MODEL_FILE);
        strategies={{"type","beam"},{"beam_size","1"}};
        initVocabFromFiles(VOCAB_FILE);
    }

    std::string Captioner::runCaptioner(vector<vector<float> >& frameFeats){
        vidFrameFeats = frameFeats;
        runPredIters();
        return convertToWords();
    }

    void Captioner::initVocabFromFiles(string vocabFile) {
        vocabInverted.push_back("<EOS>");
        vocabInverted.push_back(UNK_IDENTIFIER);
        int numWordsDataset = 0;
        std::ifstream vocab(vocabFile);
        string word;
        while(getline(vocab,word)) {
            //            std::cout << word << std::endl;
            if(!word.compare(UNK_IDENTIFIER))
              continue;

            numWordsDataset++;

            vocabInverted.push_back(word);
        }
    }

    void Captioner::runPredIters() {
        vector<vector<float> > vidFeatures = vidFrameFeats;
        encodeVideoFrames(-1);
        vector<float> padImgFeature = vidFeatures[vidFeatures.size()-1];

        for (auto &item : padImgFeature) {
            if (item > 0.0f) item = 0.0f;
        }

        predictCaption(padImgFeature,20);
    }

    void Captioner::encodeVideoFrames(int prevWord) {
        for(const auto &currFrame : vidFrameFeats) {
            float contInput = 0 ? prevWord == -1 : 1;
            float cont = contInput;
            float dataEn = prevWord;
            float stageInd = 0;

            Blob<float>* fc7 = lstmNet->input_blobs()[0];
            Blob<float>* cont_sentence = lstmNet->input_blobs()[1];
            Blob<float>* input_sentence = lstmNet->input_blobs()[2];
            Blob<float>* stage_indicator = lstmNet->input_blobs()[3];

            float* input_fc7 = fc7->mutable_cpu_data();
            memcpy(input_fc7, &currFrame[0],currFrame.size()*sizeof(float));

            float* input_cont_sentence = cont_sentence->mutable_cpu_data();
            memcpy(input_cont_sentence,&cont,sizeof(float));

            float* input_data_en = input_sentence->mutable_cpu_data();
            memcpy(input_data_en,&dataEn,sizeof(float));

            float* input_stage_indicator = stage_indicator->mutable_cpu_data();
            memcpy(input_stage_indicator,&stageInd,sizeof(float));

            lstmNet->Forward();
        } 
    }

    void Captioner::predictCaption(vector<float> &padImgFeats,
                                   size_t maxLength) {
        int beamComplete = 0;

        int prevWord;

        //Only 1 vid, so 1 beam
        while(beamComplete < 1) {
            if(!beam.empty()) {
                prevWord = beam[beam.size() - 1];
            }
            else prevWord = 0;

            vector<float> probs = predictSingleWord(padImgFeats, prevWord);

            beam.push_back(std::distance(probs.begin(),max_element(probs.begin(), probs.end())));
            beamComplete = 0;

            if(beam[beam.size() - 1] == 0 || beam.size() >= maxLength) beamComplete++;
        }
    }

    vector<float> Captioner::predictSingleWord(vector<float> &padImgFeatures, int prevWord) {
        float contInput = 1;
        float cont = contInput;
        float dataEn = prevWord;
        float stageInd = 1;

        Blob<float>* fc7 = lstmNet->input_blobs()[0];
        Blob<float>* cont_sentence = lstmNet->input_blobs()[1];
        Blob<float>* input_sentence = lstmNet->input_blobs()[2];
        Blob<float>* stage_indicator = lstmNet->input_blobs()[3];

        float* input_fc7 = fc7->mutable_cpu_data();
        memcpy(input_fc7, &padImgFeatures[0],padImgFeatures.size()*sizeof(float));

        float* input_cont_sentence = cont_sentence->mutable_cpu_data();
        memcpy(input_cont_sentence,&cont,sizeof(float));

        float* input_data_en = input_sentence->mutable_cpu_data();
        memcpy(input_data_en,&dataEn,sizeof(float));

        float* input_stage_indicator = stage_indicator->mutable_cpu_data();
        memcpy(input_stage_indicator,&stageInd,sizeof(float));

        lstmNet->Forward();

        Blob<float>* output_layer = lstmNet->output_blobs()[0];
        const float* begin = output_layer->cpu_data();
        const float* end = begin + output_layer->shape(2);

        vector<float> result(begin,end);

        return result;
    }

    std::string Captioner::convertToWords() {
        ostringstream oss;
        for (const auto &item : beam)
            oss << vocabInverted[item] << " ";
        auto res = oss.str();
        res.pop_back();
        return res;
    }
}

/*  
    vector<vector<float> > readFeatFromFiles(string fileName) {
    std::ifstream featfd(fileName);
    vector<vector<float> > retVals;
    string line;
    while(getline(featfd,line)) {
    stringstream lineStream(line);
    string cell;
    string idFrameNum;
    getline(lineStream,idFrameNum,',');

    vector<float> fc7Features;
    while(getline(lineStream,cell,',')) 
    fc7Features.push_back(atof(cell.c_str()));
    retVals.push_back(fc7Features);
    }
    return retVals;
    }

    int main(int argc, char** argv) {
    ::google::InitGoogleLogging(argv[0]);
    string fileName = argv[1];
    string VOCAB_FILE = "/home/peifeng/tools/caffe-recurrent/examples/s2vt/yt_coco_mvad_mpiimd_vocabulary.txt";
    string LSTM_NET_FILE = "/home/peifeng/tools/caffe-recurrent/examples/s2vt/s2vt.words_to_preds.deploy.prototxt";
    string MODEL_FILE = "/home/peifeng/tools/caffe-recurrent/examples/s2vt/snapshots/s2vt_vgg_rgb.caffemodel";
    string FRAMEFEAT_FILE_PATTERN = "/home/peifeng/tools/caffe-recurrent/examples/s2vt/" + fileName;
    vector<vector<float> > vidFeats = readFeatFromFiles(FRAMEFEAT_FILE_PATTERN);
    Captioner captioner(VOCAB_FILE, LSTM_NET_FILE, MODEL_FILE, vidFeats);
    captioner.runCaptioner();
//    runCaptioner(vidFeats, FRAMEFEAT_FILE_PATTERN, fileName);
return 0;
}
*/
