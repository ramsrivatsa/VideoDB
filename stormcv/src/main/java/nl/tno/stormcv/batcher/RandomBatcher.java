package nl.tno.stormcv.batcher;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-19.
 */
public class RandomBatcher extends SimpleBatcher {
    private int minWindowSize;
    private int maxWindowSize;

    public RandomBatcher(int min, int max) {
        this.minWindowSize = min;
        this.maxWindowSize = max;

        updateWindowSize();
    }

    @Override
    protected void updateWindowSize() {
        currWindowSize = ThreadLocalRandom.current().nextInt(minWindowSize, maxWindowSize);
    }
}
