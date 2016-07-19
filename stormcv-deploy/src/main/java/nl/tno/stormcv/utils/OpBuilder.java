package nl.tno.stormcv.utils;

import nl.tno.stormcv.fetcher.FileFrameFetcher;
import nl.tno.stormcv.fetcher.IFetcher;
import nl.tno.stormcv.fetcher.RefreshingImageFetcher;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.operation.CaptionerOp;
import nl.tno.stormcv.operation.DnnForwardOp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-19.
 */
public class OpBuilder {
    public OpBuilder(String[] args) {
        // process args
        final String switchKeyword = "--";
        for (String arg : args) {
            if (arg.startsWith(switchKeyword)) {
                String[] kv = arg.substring(switchKeyword.length()).split("=");
                if (kv.length != 2) continue;
                int value = 1;
                try {
                    value = Integer.parseInt(kv[1]);
                } catch (NumberFormatException ex) {
                    // nothing
                }
                switch (kv[0]) {
                    case "fat-priority":
                        fatPriority = value;
                        break;
                    case "use-caffe":
                        useCaffe = value != 0;
                        break;
                    case "use-gpu":
                        useGPU = value != 0;
                        maxGPUNum = value;
                        break;

                    case "frame-skip":
                        frameSkip = value;
                        break;
                    case "auto-sleep":
                        autoSleep = value != 0;
                        break;
                    case "start-delay":
                        startDelay = value;
                        break;
                    case "fps":
                        //sleepMs = 1000 / value;
                        sleepMs = 0;
                        sendingFps = value;
                        break;
                    case "fetcher":
                        fetcherType = kv[1];
                        break;
                }
            } else {
                // Multiple files will be spread over the available spouts
                files.add("file://" + arg);
            }
        }
    }

    public boolean useCaffe = false;
    public boolean useGPU = false;
    public int maxGPUNum = -1;
    public int fatPriority = 0;

    public DnnForwardOp buildGoogleNet(String name, String outputBlob) {
        DnnForwardOp dnnforward;
        if (useCaffe) {
            System.out.println("Using Caffe");
            dnnforward = new DnnForwardOp(name, "/data/bvlc_googlenet.prototxt",
                    "/data/bvlc_googlenet.caffemodel",
                    outputBlob,
                    "/data/imagenet_mean.binaryproto",
                    !useGPU); // caffeOnCPU == !useGPU
            dnnforward.maxGPUNum(maxGPUNum);
        } else {
            System.out.println("Using OpenCV::DNN");
            dnnforward = new DnnForwardOp(name, "/data/bvlc_googlenet.old.prototxt",
                    "/data/bvlc_googlenet.caffemodel", outputBlob);
        }
        dnnforward.outputFrame(true).threadPriority(fatPriority);
        return dnnforward;
    }

    public DnnForwardOp buildVggNet(String name, String outputBlob) {
        DnnForwardOp dnnforward;
        if (useCaffe) {
            System.out.println("Using Caffe");
            dnnforward = new DnnForwardOp(name,
                    "/data/bvlc_reference_caffenet.prototxt",
                    "/data/bvlc_reference_caffenet.caffemodel",
                    outputBlob,
                    "/data/imagenet_mean.binaryproto",
                    !useGPU); // caffeOnCPU == !useGPU
            dnnforward.maxGPUNum(maxGPUNum);
        } else {
            System.out.println("Using OpenCV::DNN");
            dnnforward = new DnnForwardOp(name,
                    "/data/bvlc_reference_caffenet.prototxt",
                    "/data/bvlc_reference_caffenet.caffemodel",
                    outputBlob);
        }
        dnnforward.outputFrame(true).threadPriority(fatPriority);
        return dnnforward;
    }

    public CaptionerOp buildCaptioner(String outputMetaName, String vggFeatureName) {
        return new CaptionerOp(outputMetaName,
                "/data/yt_coco_mvad_mpiimd_vocabulary.txt",
                "/data/s2vt.words_to_preds.prototxt",
                "/data/s2vt_vgg_rgb.caffemodel",
                vggFeatureName);
    }

    public List<String> files = new ArrayList<>();
    public int frameSkip = 1;
    public boolean autoSleep = false;
    public int sleepMs = 40;
    public String fetcherType = "video";
    public int sendingFps = 0;
    public int startDelay = 0;

    public IFetcher<? extends CVParticle> buildFetcher() {
        IFetcher<? extends  CVParticle> fetcher;
        switch(fetcherType) {
            case "video":
                fetcher = new FileFrameFetcher(files).frameSkip(frameSkip).autoSleep(autoSleep)
                        .sleep(sleepMs);
                break;
            default:
            case "image":
                fetcher = new RefreshingImageFetcher(files).sendingFps(sendingFps)
                        .autoSleep(autoSleep).startDelay(startDelay);
        }
        return fetcher;
    }
}
