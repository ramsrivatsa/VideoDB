#include "GPUUtils.h"

// dummy implementation without cuda
int getNumGPUs()
{
    return -1;
}

void setGpuDevice(int gpuId)
{
    (void) gpuId;
}

int hasCuda()
{
    return 0;
}
