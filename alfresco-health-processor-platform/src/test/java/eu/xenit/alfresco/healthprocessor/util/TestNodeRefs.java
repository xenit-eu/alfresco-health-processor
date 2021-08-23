package eu.xenit.alfresco.healthprocessor.util;

import java.util.UUID;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;

public class TestNodeRefs {

    private static final int NUMBER_OF_TEST_REFS = 1000;
    public static final NodeRef[] REFS = new NodeRef[NUMBER_OF_TEST_REFS];
    public static final NodeRef REF;

    static {
        for (int i = 0; i < NUMBER_OF_TEST_REFS; i++) {
            REFS[i] = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, UUID.randomUUID().toString());
        }
        REF = REFS[0];
    }

}
