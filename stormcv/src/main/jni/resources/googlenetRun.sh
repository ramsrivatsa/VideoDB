#!/bin/bash

base="./classification resources/deploy.prototxt resources/bvlc_googlenet.caffemodel resources/imagenet_mean.binaryproto resources/synset_words.txt "

for file in /home/lvnguyen/tools/caffe/examples/images/*
do
    base="$base $file"
done
$base #examples/images/space_shuttle.jpg
