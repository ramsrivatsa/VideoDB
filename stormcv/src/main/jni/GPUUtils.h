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

#ifdef __cplusplus
extern "C" {
#endif

int getNumGPUs();

void setGpuDevice(int id);

bool hasCuda();


#ifdef __cplusplus
}
#endif

#endif // GPUUTILS_H
