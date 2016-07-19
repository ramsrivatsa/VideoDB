cmake_minimum_required(VERSION 3.1)

project(stormcv)

#==============================================================================
# General configure
#==============================================================================

# Include custom cmake modules
include(cmake/Utilities.cmake)
list(APPEND CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake)

# Ensure out-of-source build
if(${CMAKE_SOURCE_DIR} STREQUAL ${CMAKE_BINARY_DIR})
    message(FATAL_ERROR "Usage: mkdir build; cmake ..")
endif()

# Default build type
if(NOT CMAKE_BUILD_TYPE)
    set(CMAKE_BUILD_TYPE Debug)
endif()

# Use C++11 standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add some basic compiler flags
if ("${CMAKE_CXX_COMPILER_ID}" MATCHES "Clang")
    # using Clang or AppleClang

    # reasonable and standard
    add_compile_options_with_check(-Werror)

    add_compile_options_with_check(-Wno-c++98-compat)

    add_compile_options_with_check(-Wold-style-cast)
    # helps catch hard to track down memory errors
    add_compile_options_with_check(-Wnon-virtual-dtor)
    # warn for potential performance problem casts
    add_compile_options_with_check(-Wcast-align)
    # warn if you overload (not override) a virtual function
    add_compile_options_with_check(-Woverloaded-virtual)
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "GNU")
    # using GCC

    # reasonable and standard
    add_compile_options_with_check(-Wall)
    add_compile_options_with_check(-Wextra)
    add_compile_options_with_check(-Werror)

    # helps catch hard to track down memory errors
    add_compile_options_with_check(-Wnon-virtual-dtor)
    # warn for potential performance problem casts
    add_compile_options_with_check(-Wcast-align)
    # warn if you overload (not override) a virtual function
    add_compile_options_with_check(-Woverloaded-virtual)
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "Intel")
    # using Intel C++
elseif ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "MSVC")
    # using Visual Studio C++

    # all reasonable warnings
    add_compile_options_with_check(/W4)
    # treat warnings as errors
    add_compile_options_with_check(/Wx)
    # enable warning on thread un-safe static member initialization
    add_compile_options_with_check(/W44640)
endif()

#==============================================================================
# Dependencies
#==============================================================================
## JNI generated headers
set(JNI_CLASS_INCLUDE_DIR "" CACHE FILEPATH "Path to generated jni class includes")

if(NOT EXISTS ${JNI_CLASS_INCLUDE_DIR}/xyz_unlimitedcodeworks_operations_extra_ForwardNet.h)
  message(SEND_ERROR "Can't find xyz_unlimitedcodeworks_operations_extra_ForwardNet.h in ${JNI_CLASS_INCLUDE_DIR}")
endif()

include_directories(SYSTEM ${JNI_CLASS_INCLUDE_DIR})

## Thirdy party packages
find_package(JNI REQUIRED)
include_directories(SYSTEM ${JNI_INCLUDE_DIRS})

find_package(OpenCV 3.1 REQUIRED)
# make sure opencv headers are treaded as system header
include_directories(SYSTEM ${OpenCV_INCLUDE_DIRS})

find_package(Caffe REQUIRED)
# make sure caffe headers are treaded as system header
include_directories(SYSTEM ${Caffe_INCLUDE_DIRS})
# use imported targets
# this is totally hack, caffe should populate imported target properties
# with proper definitions
set(caffe_defs)
foreach(defpair IN LISTS Caffe_DEFINITIONS)
    string(REGEX REPLACE "^-D" "" defname ${defpair})
    list(APPEND caffe_defs ${defname})
endforeach(defpair)
set_property(TARGET caffe PROPERTY
    INTERFACE_COMPILE_DEFINITIONS ${caffe_defs}
)
message("Caffe defs: ${caffe_defs}")
unset(caffe_defs)

message("Using JNI from ${JNI_INCLUDE_DIRS}")
message("Using OpenCV from ${OpenCV_INCLUDE_DIRS}")
message("Using Caffe from ${Caffe_INCLUDE_DIRS}")

## CppMT built from source
add_subdirectory(CppMT)

#==============================================================================
# Actual compile target
#==============================================================================

#------------------------------------------------------------------------------
# Shared Library stormcv_common
#------------------------------------------------------------------------------
add_library(stormcv_common SHARED
    jni_helper.cpp
    cv/converters.cpp
    cv/features2d_converters.cpp
)
target_link_libraries(stormcv_common
    PUBLIC
    opencv_core
)

#------------------------------------------------------------------------------
# Shared Library gpu_utils
#------------------------------------------------------------------------------
find_package(CUDA QUIET)
if(CUDA_FOUND)
    cuda_add_library(gpu_utils SHARED GPUUtils.cu)
else()
    message("CUDA not found, gpu_utils will only have dummy implementation")
    add_library(gpu_utils SHARED GPUUtils-dummy.c)
endif()


#------------------------------------------------------------------------------
# Shared Library stormcv_cv
#------------------------------------------------------------------------------
add_library(stormcv_cv SHARED
    ForwardNetCV.cpp
    JniForwardNet.cpp
)
target_link_libraries(stormcv_cv
    stormcv_common

    opencv_imgproc
    opencv_dnn
)

#------------------------------------------------------------------------------
# Shared Library stormcv_caffe
#------------------------------------------------------------------------------
add_library(stormcv_caffe SHARED
    ForwardNetCaffe.cpp
    JniForwardNet.cpp
)
target_link_libraries(stormcv_caffe
    stormcv_common
    gpu_utils

    opencv_imgproc
    caffe
)
target_compile_definitions(stormcv_caffe
PUBLIC
    FN_USE_CAFFE
)

#------------------------------------------------------------------------------
# Shared Library stormcv_thread
#------------------------------------------------------------------------------
add_library(stormcv_thread SHARED
    ThreadUtils.cpp
    JniThreadUtils.cpp
)
target_link_libraries(stormcv_thread
    stormcv_common
)

#------------------------------------------------------------------------------
# Shared Library stormcv_detectorx
#------------------------------------------------------------------------------
add_library(stormcv_detectorx SHARED
    FeatureDetectorX.cpp
    JniFeatureDetectorX.cpp
)
target_link_libraries(stormcv_detectorx
    stormcv_common

    opencv_xfeatures2d
)

#------------------------------------------------------------------------------
# Shared Library stormcv_extractorx
#------------------------------------------------------------------------------
add_library(stormcv_extractorx SHARED
    DescriptorExtractorX.cpp
    JniDescriptorExtractorX.cpp
)
target_link_libraries(stormcv_extractorx
    stormcv_common

    opencv_xfeatures2d
)

#------------------------------------------------------------------------------
# Shared Library stormcv_tracker
#------------------------------------------------------------------------------
add_library(stormcv_tracker SHARED
    JniCMTTracker.cpp
)
target_link_libraries(stormcv_tracker
    stormcv_common
    cmt
)

#------------------------------------------------------------------------------
# Shared Library stormcv_s2vt
#------------------------------------------------------------------------------
add_library(stormcv_s2vt SHARED
    s2vt.cpp
    JniS2VT.cpp
)
target_link_libraries(stormcv_s2vt
    stormcv_common
    caffe
)

#==============================================================================
# Target installation
#==============================================================================
install(TARGETS
    stormcv_common
    stormcv_cv
    stormcv_caffe
    stormcv_thread
    stormcv_detectorx
    stormcv_extractorx
    stormcv_tracker
    stormcv_s2vt
    gpu_utils
    DESTINATION native/x64)
