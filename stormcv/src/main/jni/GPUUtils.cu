#include "GPUUtils.h"

int getNumGPUs() {
    int numGpus = 0;
    cudaGetDeviceCount(&numGpus);
    return numGpus;
}

void setGpuDevice(int id) {
    cudaSetDevice(id);
}

bool hasCuda() {
    return true;
}