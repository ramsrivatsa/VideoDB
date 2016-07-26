/*
 * =====================================================================================
 *
 *       Filename:  mdnet.cpp
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  07/25/2016 01:22:04 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  Linh Nguyen (lvnguyen), lvnguyen@umich.edu
 *        Company:  
 *
 * =====================================================================================
 */

#include "libmdnet.h"
#include <iostream>
#include <dirent.h>
#include <algorithm>
#include <vector>

using std::string;
using std::vector;

int run_mdnet() {
    if(!mclInitializeApplication(NULL,0)) {
        std::cerr << "Cannot initialized application" << std::endl;
        return -1;
    }

    if(!libmdnetInitialize()) {
        std::cerr << "Cannot initialized the library" << std::endl;
        return -1;
    }

    string rootDir = "/home/lvnguyen/MDNet/dataset/";
    string subDir = "OTB/Diving/img/";

    vector<string> imgFiles;

    DIR *dpdf;
    struct dirent *epdf;
    dpdf = opendir((rootDir + subDir).c_str());
    if (dpdf != NULL) {
        while(epdf = readdir(dpdf)) {
            if(strcmp(epdf->d_name,".") && strcmp(epdf->d_name,"..")) {
                imgFiles.push_back(rootDir + subDir + epdf->d_name);
            }
        }
    }

    std::sort(imgFiles.begin(),imgFiles.end());

    mwArray images(1,215,mxCELL_CLASS);
    imgFiles.erase(imgFiles.begin()+215,imgFiles.begin()+231);
    for(int i = 0; i < imgFiles.size(); i++) {
        mwArray img((char*)imgFiles[i].c_str());
        mwSize dirSize = imgFiles[i].size();
        images.Get(1,i+1).Set(img);
    }


    int bbox[] = {177, 51, 21, 129};


    closedir(dpdf);

    mwSize numImgs = imgFiles.size();
    mwArray result;
    mwArray region(1,4,mxDOUBLE_CLASS,mxREAL);
    region.SetData(bbox,4);
    mwArray net("/home/lvnguyen/MDNet/models/mdnet_vot-otb.mat");

    mdnet_run(1,result,images,region,net);

    libmdnetTerminate();
    mclTerminateApplication();

    std::cout << "SUCCESS" << std::endl;
}

