# Video Analysis on Storm
Video Analysis using DNN based image classification

## Build

##### Get source code
[Git LFS](https://git-lfs.github.com/) is used to tracking binary files in the repository but keeping them separately stored. So you need Git LFS installed to get a full working copy of the whole repository.

Current active branch is `clarity-deploy`.

##### OpenCV
OpenCV java bindings must be present in the local maven repository so the build script can find it. The jar file itsself is bundled in the repository at `stormcv/thirdparty/opencv/opencv310.jar`. To install it to the local maven repository, run
```bash
mvn install:install-file -Dfile=opencv-310.jar -DgroupId=org.opencv -DartifactId=opencv -Dversion=3.1.0 -Dpackaging=jar
```

##### StormCV
```bash
$ cd stormcv
$ ./gradlew install
```

This will build the stormcv library and JNI native codes and install it to the local maven repository.

###### StormCV-Deploy
```bash
$ cd stormcv-deploy
$ mvn package
```

This will build the uber jar file for submit to storm. You can find it at `stormcv-deploy/target/stormcv-deploy-0.0.1-SNAPSHOT-jar-with-dependencies.jar`.