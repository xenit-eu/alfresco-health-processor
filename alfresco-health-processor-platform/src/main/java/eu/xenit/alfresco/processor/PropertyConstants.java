package eu.xenit.alfresco.processor;

import lombok.Getter;

public interface PropertyConstants {

    String PREFIX_HP = "eu.xenit.alfresco.processor";

    String PREFIX_INDEXING = PREFIX_HP + ".indexing";
    String PROP_INDEXING_STRATEGY = PREFIX_INDEXING + ".strategy";

    String PREFIX_INDEXING_TXNID = PREFIX_INDEXING + ".txn-id";
    String PROP_INDEXING_TXNID_START = PREFIX_INDEXING_TXNID + ".start";
    String PROP_INDEXING_TXNID_STOP = PREFIX_INDEXING_TXNID + ".stop";
    String PROP_INDEXING_TXNID_TXNBATCHSIZE = PREFIX_INDEXING_TXNID + ".txn-batch-size";

    enum IndexingStrategyKey {
        TXNID("txn-id");

        @Getter
        private final String key;

        IndexingStrategyKey(String key) {
            this.key = key;
        }

        public static IndexingStrategyKey fromKey(String key) {
            for (IndexingStrategyKey s : IndexingStrategyKey.values()) {
                if (s.getKey().equals(key)) {
                    return s;
                }
            }
            return null;
        }


    }
}
