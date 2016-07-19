/*
 * =====================================================================================
 *
 *       Filename:  s2vt.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  07/18/2016 04:22:48 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  Linh Nguyen (lvnguyen), lvnguyen@umich.edu
 *        Company:  
 *
 * =====================================================================================
 */

#ifndef S2VT_H
#define S2VT_H

#include <iostream>
#include <string>
#include <fstream>
#include <sstream>
#include <caffe/caffe.hpp>

using std::string;
using std::map;
using std::stringstream;
using std::getline;
using std::vector;
using std::cout;
using std::endl;

#define UNK_IDENTIFIER ("<en_unk>")

using namespace caffe;

namespace ucw{
    class Captioner{
    public:
      Captioner(const string& VOCAB_FILE,
                const string& LSTM_NET_FILE,
                const string& MODEL_FILE, bool useGPU);

      std::string runCaptioner(vector<vector<float> > &framesFeat);

    private:
      void encodeVideoFrames(int prevWord); 

      vector<float> predictSingleWord(vector<float> &padImgFeatures, 
                                      int prevWord);

      void predictCaption(vector<float> &padImgFeats,
                          size_t maxLength);

      void runPredIters();
      std::string convertToWords();

      void initVocabFromFiles(string vocabFile);

    private:
      shared_ptr<Net<float> > lstmNet;
      vector<vector<float> > vidFrameFeats;
      map<string,string> strategies;
      vector<string> vocabInverted;
      vector<int> beam;
    };
}

#endif