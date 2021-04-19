package eu.xenit.alfresco.healthprocessor.processing;

import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.alfresco.repo.cache.SimpleCache;

@RequiredArgsConstructor
public class StateCache {

    private static final String KEY_STATE = "state";

    private final SimpleCache<String, Serializable> cache;

    public void setState(ProcessorState state) {
        cache.put(KEY_STATE, state);
    }

    public ProcessorState getStateOrDefault() {
        return cache.contains(KEY_STATE) ? (ProcessorState) cache.get(KEY_STATE) : ProcessorState.IDLE;
    }

}
