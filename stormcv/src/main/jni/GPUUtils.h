/*
 * =====================================================================================
 *
 *       Filename:  GPUUtils.h
 *
 *    Description:  
 *
 *        Version:  1.0
 *        Created:  07/06/2016 01:21:29 PM
 *       Revision:  none
 *       Compiler:  gcc
 *
 *         Author:  Linh Nguyen (lvnguyen), lvnguyen@umich.edu
 *        Company:  
 *
 * =====================================================================================
 */

#ifndef GPUUTILS_H
#define GPUUTILS_H

namespace ucw {

int getNumGPUs();

void setGpuDevice(int id);

bool hasCuda();

#ifndef HAS_CUDA
// dummy implementation without cuda
    int getNumGPUs() { return -1; }
    void setGpuDevice(int) {}
    bool hasCuda() { return false; }
#endif

}

#endif
