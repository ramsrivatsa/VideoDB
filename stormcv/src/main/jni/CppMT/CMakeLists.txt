project(CppMT)

message("Build CppMT from source")

set(cmt_SRC
    common.cpp
    ObjectTracking.cpp
    CMT.cpp
    Consensus.cpp
    Fusion.cpp
    Matcher.cpp
    Tracker.cpp
    fastcluster/fastcluster.cpp
)

add_library(cmt SHARED ${cmt_SRC})
target_link_libraries(cmt
    PUBLIC
    opencv_core
    opencv_imgproc
    opencv_features2d
    opencv_video
)

add_executable(cmt-bin main.cpp)
set_target_properties(cmt-bin
    PROPERTIES OUTPUT_NAME cmt)
target_link_libraries(cmt-bin cmt opencv_videoio)

install(TARGETS cmt DESTINATION native/x64)