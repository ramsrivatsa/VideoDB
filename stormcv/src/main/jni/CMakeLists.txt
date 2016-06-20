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

if(NOT EXISTS ${JNI_CLASS_INCLUDE_DIR}/xyz_unlimitedcodeworks_opencv_dnn_ForwardNet.h)
  message(SEND_ERROR "Can't find xyz_unlimitedcodeworks_opencv_dnn_ForwardNet.h in ${JNI_CLASS_INCLUDE_DIR}")
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
#==============================================================================
# Actual compile target
#==============================================================================
aux_source_directory(. SRC_LIST)

add_library(stormcv SHARED ${SRC_LIST})
target_link_libraries(stormcv
    opencv_core
    opencv_imgproc
    opencv_dnn
    caffe
)

install(TARGETS stormcv DESTINATION native/x64)