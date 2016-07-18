package nl.tno.stormcv.batcher;

import nl.tno.stormcv.bolt.BatchInputBolt.History;
import nl.tno.stormcv.model.CVParticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.unlimitedcodeworks.utils.Timing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link IBatcher} implementation that partitions a set of {@link CVParticle} items into zero or more sliding windows of the specified size.
 * The windowSize and sequenceDelta are used to determine if the provided list with items contains any sliding windows. If the
 * Batcher was created with a windowSize of 3 and sequenceDelta of 10 the input <i>with sequenceNr's</i> [20, 30, 40, 50, 70, 80, 90] will
 * result in two 'windows': [20, 30, 40] and [30, 40, 50]. Note that no other windows are created because some item (60) is still missing.
 * The first item of any approved window will be removed from the history because they will not be needed any more. In the example above this
 * results in the removal of item '20' and item '30' from the History which will trigger an ACK on those items.
 * <p/>
 * If a specific element is missing this Batcher will never provide any results. To avoid this situation it is possible to set the maximum
 * number of elements in the queue. If this maximum is reached this Batcher will generate results even if they do not match the criteria specified.
 *
 * @author Corne Versloot
 */
public class SlidingWindowBatcher implements IBatcher {

    private static final long serialVersionUID = -4296426517808304248L;
    private Logger logger = LoggerFactory.getLogger(SlidingWindowBatcher.class);
    private int windowSize;
    private int sequenceDelta;
    private long lastSequence;
    private long lastSubmitTime;
    private boolean forceSingleFrameBatch = true;

    private int maxSize = Integer.MAX_VALUE;
    private long maxWait = Long.MAX_VALUE;

    public SlidingWindowBatcher(int windowSize, int sequenceDelta) {
        this(windowSize, sequenceDelta, 0);
    }

    public SlidingWindowBatcher(int windowSize, int sequenceDelta, long lastSequence) {
        this.windowSize = windowSize;
        this.sequenceDelta = sequenceDelta;
        this.lastSequence = lastSequence;
    }

    /**
     * Whether to force output batch size to be one
     * @param force
     * @return
     */
    public SlidingWindowBatcher forceSingleFrameBatch(boolean force) {
        this.forceSingleFrameBatch = force;
        return this;
    }

    /**
     * Maximum time to wait before skipping frames
     * @param waitMs
     * @return
     */
    public SlidingWindowBatcher maxWait(long waitMs) {
        this.maxWait = waitMs;
        return this;
    }

    /**
     * Maximum size for the cache before skipping frames
     * @param size
     * @return
     */
    public SlidingWindowBatcher maxSize(int size) {
        this.maxSize = size;
        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map conf) throws Exception {
    }

    @Override
    public List<List<CVParticle>> partition(History history, List<CVParticle> currentSet) {
        List<List<CVParticle>> result = new ArrayList<List<CVParticle>>();

        if (currentSet.size() == 0) return result;

        List<CVParticle> window = new ArrayList<>();
        for (int curr = 0; curr != currentSet.size(); ++curr) {
            CVParticle particle = currentSet.get(curr);
            if (particle.getSequenceNr() == lastSequence + sequenceDelta) {
                // get what we want
                window.add(particle);
                history.removeFromHistory(particle);
                lastSequence += sequenceDelta;
            } else if (particle.getSequenceNr() <= lastSequence) {
               // an old frame, drop it
                history.removeFromHistory(particle);
                logger.warn("Dropping old frame {}",particle.getSequenceNr());
            } else {
                // window ends, submit what we found
                if (window.size() != 0) {
                    if (forceSingleFrameBatch) {
                        for (CVParticle cvt : window) {
                            List<CVParticle> l = new ArrayList<>();
                            l.add(cvt);
                            result.add(l);
                        }
                    } else {
                        result.add(window);
                    }
                    lastSubmitTime = Timing.currentTimeMillis();
                    window = null;
                }

                // if we still want to wait
                if (currentSet.size() - curr > maxSize
                        || Timing.currentTimeMillis() - lastSubmitTime > maxWait) {
                    logger.warn("Skipping frame(s) between {} and {}",
                            lastSequence, particle.getSequenceNr());
                    if (window == null)
                        window = new ArrayList<>();

                    window.add(particle);
                    history.removeFromHistory(particle);
                    lastSequence = particle.getSequenceNr();
                } else {
                    break;
                }
            }
        }

        /*
        for (int startAt = 0; startAt != currentSet.size() - 1; ++startAt) {
            windowSize = 0;
            List<CVParticle> window = null;
            for (int endBefore = currentSet.size(); endBefore >= 0; --endBefore) {
                window = new ArrayList<>();
                window.addAll(currentSet.subList(startAt, endBefore));
                windowSize = window.size();
                if (assessWindow(window)) {
                    break;
                }
            }
            // leave the last matching one, which is used for next matching.
            if (windowSize > 1) {
                window.remove(window.size()-1);
                for (CVParticle cvt : window) {
                    history.removeFromHistory(cvt);
                }
                result.add(window);
                return result;
            }

            // suitable window not found
            if (currentSet.size() - startAt > maxSize) {
                logger.warn("Skipping frame(s) between {} and {}",
                            currentSet.get(startAt),
                            currentSet.get(startAt + 1));
                if (windowSize != 0) {
                    for (CVParticle cvt : window) {
                        history.removeFromHistory(cvt);
                    }
                    result.add(window);
                }
            } else {
                break;
            }
        }
        */

        /*
        long previous = currentSet.get(0).getSequenceNr();
        int i = 1;
        for (; i != currentSet.size(); ++i) {
            if (currentSet.get(i).getSequenceNr() - previous != sequenceDelta) {
                break;
            }
            previous = currentSet.get(i).getSequenceNr();
        }
        --i; // leave the last matching one, which is used for next matching.
        List<CVParticle> window = new ArrayList<>();
        window.addAll(currentSet.subList(0, i));
        for (CVParticle cvt : window) {
            history.removeFromHistory(cvt);
        }
        result.add(window);
        */


        /*
        for (int i = 0; i <= currentSet.size() - windowSize; i++) {
            List<CVParticle> window = new ArrayList<CVParticle>();
            window.addAll(currentSet.subList(i, i + windowSize)); // add all is used to avoid ConcurrentModificationException when the History cleans stuff up
            if (assessWindow(window) || currentSet.size() > maxSize) {
                result.add(window);
                //logger.info("Completed processing frame : " + "StreamID - "+ window.get(0).getStreamId() + " Sequence Nr - " + window.get(0).getSequenceNr() + " System Time - " + System.currentTimeMillis());
                history.removeFromHistory(window.get(0));
            } else break;
        }
        //logger.info("Frame Obtained" + System.nanoTime());
        */
        return result;
    }

    /**
     * Checks if the provided window fits the required windowSize and sequenceDelta criteria
     *
     * @param window
     * @return
     */
    private boolean assessWindow(List<CVParticle> window) {
        if (window.size() != windowSize) return false;
        long previous = window.get(0).getSequenceNr();
        for (int i = 1; i < window.size(); i++) {
            if (window.get(i).getSequenceNr() - previous != sequenceDelta) return false;
            previous = window.get(i).getSequenceNr();
        }
        return true;
    }

}
