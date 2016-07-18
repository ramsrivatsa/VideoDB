/*
 * =====================================================================================
 *
 *       Filename:  fc7FrameSequenceGenerator.cpp
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  07/12/2016 04:57:45 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  Linh Nguyen (lvnguyen), lvnguyen@umich.edu
 *        Company:  
 *
 * =====================================================================================
 */

#include "fc7FrameSequenceGenerator.h"

namespace ucw{  

    fc7FrameSequenceGenerator::fc7FrameSequenceGenerator(const string videoId,
                                                         const string vocabFile,
                                                         const vector<vector<float> > &framesFeat) {


        vidId = videoId;

        vidFrameFeats.insert({vidId,framesFeat});

        initVocabFromFiles(vocabFile);
    }

    void fc7FrameSequenceGenerator::initVocabFromFiles(string vocabFile) {

        vocabulary = {{UNK_IDENTIFIER, 0}};
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

            vocabulary.insert(std::pair<string,int>(word,vocabInverted.size()));
            vocabInverted.push_back(word);
        }
    }
}
