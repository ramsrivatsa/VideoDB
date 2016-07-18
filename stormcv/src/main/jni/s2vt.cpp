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

#include "fc7FrameSequenceGenerator.h"

using std::vector;
using std::map;
using std::cout;
using std::endl;
using namespace caffe;

void encodeVideoFrames(shared_ptr<Net<float> > net, vector<vector<float>> &vidFeatures, int prevWord=-1) {
    for(int i = 0; i < vidFeatures.size(); i++) {
        float contInput = 0 ? prevWord == -1 : 1;
        float cont = contInput;
        float dataEn = prevWord;
        float stageInd = 0;

        vector<float> currFrame = vidFeatures[i];

        Blob<float>* fc7 = net->input_blobs()[0];
        Blob<float>* cont_sentence = net->input_blobs()[1];
        Blob<float>* input_sentence = net->input_blobs()[2];
        Blob<float>* stage_indicator = net->input_blobs()[3];

        float* input_fc7 = fc7->mutable_cpu_data();
        memcpy(input_fc7, &currFrame[0],currFrame.size()*sizeof(float));

        float* input_cont_sentence = cont_sentence->mutable_cpu_data();
        memcpy(input_cont_sentence,&cont,sizeof(float));

        float* input_data_en = input_sentence->mutable_cpu_data();
        memcpy(input_data_en,&dataEn,sizeof(float));

        float* input_stage_indicator = stage_indicator->mutable_cpu_data();
        memcpy(input_stage_indicator,&stageInd,sizeof(float));

        net->Forward();
    } 
}

vector<float> predictSingleWord(shared_ptr<Net<float> > net, vector<float> &padImgFeatures, int prevWord) {

    float contInput = 1;
    float cont = contInput;
    float dataEn = prevWord;
    float stageInd = 1;

    Blob<float>* fc7 = net->input_blobs()[0];
    Blob<float>* cont_sentence = net->input_blobs()[1];
    Blob<float>* input_sentence = net->input_blobs()[2];
    Blob<float>* stage_indicator = net->input_blobs()[3];

    float* input_fc7 = fc7->mutable_cpu_data();
    memcpy(input_fc7, &padImgFeatures[0],padImgFeatures.size()*sizeof(float));

    float* input_cont_sentence = cont_sentence->mutable_cpu_data();
    memcpy(input_cont_sentence,&cont,sizeof(float));

    float* input_data_en = input_sentence->mutable_cpu_data();
    memcpy(input_data_en,&dataEn,sizeof(float));

    float* input_stage_indicator = stage_indicator->mutable_cpu_data();
    memcpy(input_stage_indicator,&stageInd,sizeof(float));

    net->Forward();

    Blob<float>* output_layer = net->output_blobs()[0];
    const float* begin = output_layer->cpu_data();
    const float* end = begin + output_layer->shape(2);

    vector<float> result(begin,end);

    return result;
}

vector<int> predictCaption(shared_ptr<Net<float> > net, 
                           vector<float> &padImgFeats,
                           vector<string> &vocabList,
                           map<string,string> &strategies,
                           int maxLength = 15) {
    vector<int> beam;
    int beamComplete = 0;

    int prevWord;

    //Only 1 vid, so 1 beam
    while(beamComplete < 1) {
        if(!beam.empty()) {
            prevWord = beam[beam.size() - 1];
        }
        else prevWord = 0;

        vector<float> probs = predictSingleWord(net, padImgFeats, prevWord);

        beam.push_back(std::distance(probs.begin(),max_element(probs.begin(), probs.end())));
        beamComplete = 0;

        if(beam[beam.size() - 1] == 0 || beam.size() >= maxLength) beamComplete++;
        //        std::sort(expansions.begin(), expansions.end());
    }

    return beam;
}

map<string,vector<int> > runPredIters(shared_ptr<Net<float> > net,
                                      string vidId,
                                      map<string, vector<float> > &videoGtPairs,
                                      fc7FrameSequenceGenerator fsg,
                                      map<string,string> &strategies,
                                      vector<string> &vocabList) {
    //Return the vector of probs and indices
    map<string,vector<int> > outputs;
    int numPairs = 0;
    string descriptorId = "";
    //TODO: For each video in chunk: put the following code in a loop

    vector<float> gtCaptions = videoGtPairs[vidId];
    numPairs++;
    vector<vector<float> > vidFeatures = fsg.vidFrameFeats[vidId];
    encodeVideoFrames(net, vidFeatures);
    vector<float> padImgFeature = vidFeatures[vidFeatures.size()-1];

    for(int i = 0; i < padImgFeature.size(); i++) {
        if(padImgFeature[i] > 0.0f) padImgFeature[i] = 0.0f;
    }

    outputs[vidId] = predictCaption(net, padImgFeature, vocabList, strategies);

    return outputs;
}

void convertToWords(map<string,vector<int> > &captionsId, vector<string> vocabList) {
    //    cout << "Caption for " << captionsId.begin()->first << endl;
    vector<int> captions = captionsId.begin()->second;
    for(int i = 0; i < captions.size() - 1; i++) 
      cout << vocabList[captions[i]] << " ";
    cout << endl;
}

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

/* NOTE: Modify this function as you need. If necessary, move all file parsing calls from main inside this function
 * call convertToWords to see output in complete sentence*/
void runCaptioner(vector<vector<float> > &allVidFeats, string FRAMEFEAT_FILE_PATTERN, string fileName) {
    string VOCAB_FILE = "/home/peifeng/tools/caffe-recurrent/examples/s2vt/yt_coco_mvad_mpiimd_vocabulary.txt";
    string LSTM_NET_FILE = "/home/peifeng/tools/caffe-recurrent/examples/s2vt/s2vt.words_to_preds.deploy.prototxt";
    string MODEL_FILE = "/home/peifeng/tools/caffe-recurrent/examples/s2vt/snapshots/s2vt_vgg_rgb.caffemodel";
    map<string,string> STRATEGIES = {{"type","beam"},{"beam_size","1"}};

    Caffe::set_mode(Caffe::GPU);

    shared_ptr<Net<float> > lstmNet;
    lstmNet.reset(new Net<float>(LSTM_NET_FILE, TEST));
    lstmNet->CopyTrainedLayersFrom(MODEL_FILE);

    vector<shared_ptr<Net<float> > > nets;
    nets.push_back(lstmNet);

    string vocab = VOCAB_FILE;

    vector<float> emp;

    fc7FrameSequenceGenerator fsg(fileName, vocab, allVidFeats);
    string vidId = fsg.vidId;
    map<string,vector<float> > videoGtPairs = {{vidId,emp}};

    map<string,vector<int> > outputs = runPredIters(lstmNet, vidId, videoGtPairs, fsg, STRATEGIES, fsg.vocabInverted);
//    convertToWords(outputs,fsg.vocabInverted);
}

/*  
int main(int argc, char** argv) {
    ::google::InitGoogleLogging(argv[0]);
    string fileName = argv[1];
    string FRAMEFEAT_FILE_PATTERN = "/home/peifeng/tools/caffe-recurrent/examples/s2vt/" + fileName + ".txt";
    vector<vector<float> > vidFeats = readFeatFromFiles(FRAMEFEAT_FILE_PATTERN);
    runCaptioner(vidFeats, FRAMEFEAT_FILE_PATTERN, fileName);
    return 0;
}
*/
