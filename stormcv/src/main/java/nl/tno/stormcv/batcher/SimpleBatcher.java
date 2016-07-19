package nl.tno.stormcv.batcher;

import nl.tno.stormcv.bolt.BatchInputBolt;
import nl.tno.stormcv.model.CVParticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-19.
 */
public class SimpleBatcher implements  IBatcher {
    private static final long serialVersionUID = 789734984649506398L;

    private int windowSize;

    public SimpleBatcher(int windowSize){
        this.windowSize = windowSize;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf) throws Exception { }

    @Override
    public List<List<CVParticle>> partition(BatchInputBolt.History history, List<CVParticle> currentSet) {
        List<List<CVParticle>> result = new ArrayList<>();

        while (currentSet.size() > windowSize) {
            List<CVParticle> window = new ArrayList<>();
            window.addAll(currentSet.subList(0, windowSize));
            result.add(window);
            for (CVParticle cvt : window) {
                history.removeFromHistory(cvt);
            }
        }
        return result;
    }
}
