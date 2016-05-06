package xyz.unlimitedcodeworks;

import nl.tno.stormcv.StormCVConfig;
import nl.tno.stormcv.fetcher.FileFrameFetcher;
import nl.tno.stormcv.fetcher.IFetcher;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.operation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-5-5.
 */
public class Standalone {
    public static List<CVParticle> bench(ISingleInputOperation stage, List<CVParticle> input) {
        List<CVParticle> output = new ArrayList<>();

        long start = 0, end = 0;
        try {
            start = System.currentTimeMillis();
            for (CVParticle frame : input) {
                output.addAll(stage.execute(frame));
            }
            end = System.currentTimeMillis();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        long total = end - start;
        System.out.println(String.format("Iteration %d, total time %d ms, average time %d ms",
                                         input.size(), total, total / input.size()));

        return output;
    }

    public static void main(String[] args) {
        // spout
        List<String> files = new ArrayList<>();
        files.add(args[0]);
        IFetcher<CVParticle> fetcher = new FileFrameFetcher(files).autoSleep(false);

        // scale
        ISingleInputOperation<Frame> scale = new ScaleImageOp(0.5f);

        // fat_feature
        List<ISingleInputOperation> operations = new ArrayList<>();
        operations.add(new HaarCascadeOp("face", "haarcascade_frontalface_default.xml"));
        operations.add(new DnnForwardOp("classprob", "/data/bvlc_googlenet.prototxt",
                                        "/data/bvlc_googlenet.caffemodel").outputFrame(true));
        operations.add(new DnnClassifyOp("classprob", "/data/synset_words.txt")
                       .addMetadata(true).outputFrame(true));
        ISingleInputOperation<CVParticle> fat_feature = new SequentialFrameOp(operations)
                                                        .outputFrame(true);

        // drawer
        ISingleInputOperation<Frame> drawer = new DrawFeaturesOp().drawMetadata(true);

        // prepare all components
        StormCVConfig stormConf = new StormCVConfig();
        stormConf.put(StormCVConfig.STORMCV_STANDALONE, true);
        stormConf.put(StormCVConfig.STORMCV_LOG_PROFILING, true);

        try {
            fetcher.prepare(stormConf, null);
            scale.prepare(stormConf, null);
            fat_feature.prepare(stormConf, null);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }


        // retrive all frames first
        List<CVParticle> frames = new ArrayList<>();
        fetcher.activate();
        while (frames.size() <= 1500) {
            frames.add(fetcher.fetchData());
        }
        fetcher.deactivate();

        // run through all stages
        List<CVParticle> results;

        results = bench(scale, frames);
        results = bench(fat_feature, results);
        results = bench(drawer, results);

        return;
    }
}
