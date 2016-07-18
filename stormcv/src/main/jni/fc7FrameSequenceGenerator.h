#ifndef FC7FRAMESEQUENCEGENERATOR_H
#define FC7FRAMESEQUENCEGENERATOR_H

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
namespace ucw{

    class fc7FrameSequenceGenerator{

    public:
      fc7FrameSequenceGenerator(const string videoId,
                                const string vocabFile,
                                const vector<vector<float> > &frameFeat); 

      void initVocabFromFiles(string vocabFile);
      //members
      vector<string> lines;

      int numEmptyLines = 0;
      map<string,vector<vector<float>> > vidFrameFeats;
      map<string,int> vocabulary;
      vector<string> vocabInverted;
      string vidId;
    };

}

#endif
