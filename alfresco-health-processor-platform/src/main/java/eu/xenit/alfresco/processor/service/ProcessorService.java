package eu.xenit.alfresco.processor.service;

public interface ProcessorService {
    enum Scope {
        ALL,
        NEW
    }
    void validateHealth();
}
