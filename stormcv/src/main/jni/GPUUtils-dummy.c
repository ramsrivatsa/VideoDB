#include "GPUUtils.h"

// dummy implementation without cuda
int getNumGPUs()
{
    return -1;
}

void setGpuDevice(int)
{
}

bool hasCuda()
{
    return false;
}
