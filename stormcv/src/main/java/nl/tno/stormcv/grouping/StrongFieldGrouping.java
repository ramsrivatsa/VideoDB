package nl.tno.stormcv.grouping;

import backtype.storm.generated.GlobalStreamId;
import backtype.storm.grouping.CustomStreamGrouping;
import backtype.storm.task.WorkerTopologyContext;
import backtype.storm.tuple.Fields;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A custom grouping that functions as a FieldGrouping. {@link CVParticle} with the specified field will be passed
 * through the same bolt, and different group are sent to different bolt when possible.
 *
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-26.
 */
public class StrongFieldGrouping<SerializerT extends CVParticleSerializer<?>> implements CustomStreamGrouping {
    private static final long serialVersionUID = -2816700364074254046L;
    private List<Integer> targetTasks;
    private List<Integer> indexes = new ArrayList<>();

    /**
     * Constructs a featureGrouping for the specified feature name. Features with this name will be routed
     * according the the fields specified.
     * @param grouping
     * @throws InstantiationException
     */
    public StrongFieldGrouping(Fields grouping, SerializerT serializer) throws InstantiationException{
        Fields fields = serializer.getFields();
        for(String groupBy : grouping){
            for(int i=0; i<fields.size(); i++){
                if(groupBy.equals(fields.get(i))){
                    indexes.add(i);
                }
            }
        }
        if(indexes.size() == 0) throw new InstantiationException("No field indexes found for provided grouping: "+grouping);
    }

    @Override
    public List<Integer> chooseTasks(int taskId, List<Object> values) {
        List<Integer> targets = new ArrayList<>();

        if(indexes.size() > 0){
            int hash = 0;
            for(int i : indexes){
                hash += values.get(i).hashCode();
            }
            targets.add(assignTask(hash));
        }else{
            int randI = (int)Math.floor(Math.random() * targetTasks.size());
            targets.add(targetTasks.get(randI));
        }
        return targets;
    }

    @Override
    public void prepare(WorkerTopologyContext context, GlobalStreamId stream, List<Integer> targetTasks) {
        this.targetTasks = targetTasks;
    }

    private Map<Integer, Integer> cachedAssignment = new HashMap<>();
    private int assignTask(int hashcode) {
        if (!cachedAssignment.containsKey(hashcode)) {
            int task = nextAvailableTasks();
            incTaskLoad(task);
            cachedAssignment.put(hashcode, task);
            return task;
        } else {
            return cachedAssignment.get(hashcode);
        }
    }

    private Map<Integer, Integer> taskLoad = new HashMap<>();
    private int getTaskLoad(int task) {
        if (!taskLoad.containsKey(task)) {
            taskLoad.put(task, 0);
            return 0;
        }
        return taskLoad.get(task);
    }
    private void incTaskLoad(int task) {
        int load = 0;
        if (taskLoad.containsKey(task)) {
            load = taskLoad.get(task);
        }
        taskLoad.put(task, load + 1);
    }

    private int occupyLevel = 0;
    private int nextAvailableTasks() {
        for (int t : targetTasks) {
            if (getTaskLoad(t) <= occupyLevel) {
                return t;
            }
        }
        while (true) {
            ++occupyLevel;
            for (int t : targetTasks) {
                if (getTaskLoad(t) <= occupyLevel) {
                    return t;
                }
            }
        }
    }
}
