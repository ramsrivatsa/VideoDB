#include "GPUUtils.h"

using namespace ucw;

int getNumGPUs() {
    int numGpus = 0;
    cudaGetDeviceCount(&numGpus);
    return numGpus;
}

void setGPU(int id) {
    cudaSetDevice(id);
}

bool hasCuda() {
    return true;
}