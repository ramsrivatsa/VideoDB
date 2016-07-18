# CMAKE generated file: DO NOT EDIT!
# Generated by "Unix Makefiles" Generator, CMake Version 3.2

#=============================================================================
# Special targets provided by cmake.

# Disable implicit rules so canonical targets will work.
.SUFFIXES:

# Remove some rules from gmake that .SUFFIXES does not remove.
SUFFIXES =

.SUFFIXES: .hpux_make_needs_suffix_list

# Suppress display of executed commands.
$(VERBOSE).SILENT:

# A target that is always out of date.
cmake_force:
.PHONY : cmake_force

#=============================================================================
# Set environment variables for the build.

# The shell in which to execute make rules.
SHELL = /bin/sh

# The CMake executable.
CMAKE_COMMAND = /usr/bin/cmake

# The command to remove a file.
RM = /usr/bin/cmake -E remove -f

# Escaping for special characters.
EQUALS = =

# The top-level source directory on which CMake was run.
CMAKE_SOURCE_DIR = /home/lvnguyen/VideoDB/stormcv/src/main/jni

# The top-level build directory on which CMake was run.
CMAKE_BINARY_DIR = /home/lvnguyen/VideoDB/stormcv/src/main/jni/test

# Include any dependencies generated for this target.
include CMakeFiles/stormcv_thread.dir/depend.make

# Include the progress variables for this target.
include CMakeFiles/stormcv_thread.dir/progress.make

# Include the compile flags for this target's objects.
include CMakeFiles/stormcv_thread.dir/flags.make

CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o: CMakeFiles/stormcv_thread.dir/flags.make
CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o: ../ThreadUtils.cpp
	$(CMAKE_COMMAND) -E cmake_progress_report /home/lvnguyen/VideoDB/stormcv/src/main/jni/test/CMakeFiles $(CMAKE_PROGRESS_1)
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Building CXX object CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o"
	/usr/bin/c++   $(CXX_DEFINES) $(CXX_FLAGS) -o CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o -c /home/lvnguyen/VideoDB/stormcv/src/main/jni/ThreadUtils.cpp

CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.i"
	/usr/bin/c++  $(CXX_DEFINES) $(CXX_FLAGS) -E /home/lvnguyen/VideoDB/stormcv/src/main/jni/ThreadUtils.cpp > CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.i

CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.s"
	/usr/bin/c++  $(CXX_DEFINES) $(CXX_FLAGS) -S /home/lvnguyen/VideoDB/stormcv/src/main/jni/ThreadUtils.cpp -o CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.s

CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o.requires:
.PHONY : CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o.requires

CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o.provides: CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o.requires
	$(MAKE) -f CMakeFiles/stormcv_thread.dir/build.make CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o.provides.build
.PHONY : CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o.provides

CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o.provides.build: CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o

CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o: CMakeFiles/stormcv_thread.dir/flags.make
CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o: ../JniThreadUtils.cpp
	$(CMAKE_COMMAND) -E cmake_progress_report /home/lvnguyen/VideoDB/stormcv/src/main/jni/test/CMakeFiles $(CMAKE_PROGRESS_2)
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Building CXX object CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o"
	/usr/bin/c++   $(CXX_DEFINES) $(CXX_FLAGS) -o CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o -c /home/lvnguyen/VideoDB/stormcv/src/main/jni/JniThreadUtils.cpp

CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.i: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Preprocessing CXX source to CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.i"
	/usr/bin/c++  $(CXX_DEFINES) $(CXX_FLAGS) -E /home/lvnguyen/VideoDB/stormcv/src/main/jni/JniThreadUtils.cpp > CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.i

CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.s: cmake_force
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --green "Compiling CXX source to assembly CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.s"
	/usr/bin/c++  $(CXX_DEFINES) $(CXX_FLAGS) -S /home/lvnguyen/VideoDB/stormcv/src/main/jni/JniThreadUtils.cpp -o CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.s

CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o.requires:
.PHONY : CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o.requires

CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o.provides: CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o.requires
	$(MAKE) -f CMakeFiles/stormcv_thread.dir/build.make CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o.provides.build
.PHONY : CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o.provides

CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o.provides.build: CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o

# Object files for target stormcv_thread
stormcv_thread_OBJECTS = \
"CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o" \
"CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o"

# External object files for target stormcv_thread
stormcv_thread_EXTERNAL_OBJECTS =

libstormcv_thread.so: CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o
libstormcv_thread.so: CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o
libstormcv_thread.so: CMakeFiles/stormcv_thread.dir/build.make
libstormcv_thread.so: CMakeFiles/stormcv_thread.dir/link.txt
	@$(CMAKE_COMMAND) -E cmake_echo_color --switch=$(COLOR) --red --bold "Linking CXX shared library libstormcv_thread.so"
	$(CMAKE_COMMAND) -E cmake_link_script CMakeFiles/stormcv_thread.dir/link.txt --verbose=$(VERBOSE)

# Rule to build all files generated by this target.
CMakeFiles/stormcv_thread.dir/build: libstormcv_thread.so
.PHONY : CMakeFiles/stormcv_thread.dir/build

CMakeFiles/stormcv_thread.dir/requires: CMakeFiles/stormcv_thread.dir/ThreadUtils.cpp.o.requires
CMakeFiles/stormcv_thread.dir/requires: CMakeFiles/stormcv_thread.dir/JniThreadUtils.cpp.o.requires
.PHONY : CMakeFiles/stormcv_thread.dir/requires

CMakeFiles/stormcv_thread.dir/clean:
	$(CMAKE_COMMAND) -P CMakeFiles/stormcv_thread.dir/cmake_clean.cmake
.PHONY : CMakeFiles/stormcv_thread.dir/clean

CMakeFiles/stormcv_thread.dir/depend:
	cd /home/lvnguyen/VideoDB/stormcv/src/main/jni/test && $(CMAKE_COMMAND) -E cmake_depends "Unix Makefiles" /home/lvnguyen/VideoDB/stormcv/src/main/jni /home/lvnguyen/VideoDB/stormcv/src/main/jni /home/lvnguyen/VideoDB/stormcv/src/main/jni/test /home/lvnguyen/VideoDB/stormcv/src/main/jni/test /home/lvnguyen/VideoDB/stormcv/src/main/jni/test/CMakeFiles/stormcv_thread.dir/DependInfo.cmake --color=$(COLOR)
.PHONY : CMakeFiles/stormcv_thread.dir/depend
