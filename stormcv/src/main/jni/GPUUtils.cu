#include "GPUUtils.h"

int getNumGPUs() {
    int numGpus = 0;
    cudaGetDeviceCount(&numGpus);
    return numGpus;
}

void setGPU(int id) {
    cudaSetDevice(id);
}
