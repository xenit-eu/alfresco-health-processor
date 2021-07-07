package eu.xenit.alfresco.healthprocessor.indexing;

import org.alfresco.repo.solr.SOLRTrackingComponentImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class AlfrescoTrackingComponentFactoryBean extends AbstractFactoryBean<AlfrescoTrackingComponent>
        implements ApplicationContextAware {

    static final String SEARCH_TRACKING_COMPONENT_BEAN_ID_BEFORE_7 = "search.solrTrackingComponent";
    static final String SEARCH_TRACKING_COMPONENT_BEAN_ID_AFTER_7 = "search.trackingComponent";

    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @Override
    public Class<?> getObjectType() {
        return AlfrescoTrackingComponent.class;
    }

    @Override
    protected AlfrescoTrackingComponent createInstance() {
        return new AlfrescoTrackingComponent(getSOLRTrackingComponentBean());
    }

    private SOLRTrackingComponentImpl getSOLRTrackingComponentBean() {
        ApplicationContext contextToSearchForBean = ctx;
        while (contextToSearchForBean != null) {
            SOLRTrackingComponentImpl foundBean = getSOLRTrackingComponentBean(contextToSearchForBean);
            if (foundBean != null) {
                return foundBean;
            }
            contextToSearchForBean = contextToSearchForBean.getParent();
        }
        throw new IllegalStateException("SOLRTrackingComponent bean not found in the application context");

    }

    private static SOLRTrackingComponentImpl getSOLRTrackingComponentBean(ApplicationContext context) {
        if (context.containsBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_BEFORE_7)) {
            return (SOLRTrackingComponentImpl) context.getBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_BEFORE_7);
        }
        if (context.containsBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_AFTER_7)) {
            return (SOLRTrackingComponentImpl) context.getBean(SEARCH_TRACKING_COMPONENT_BEAN_ID_AFTER_7);
        }
        return null;
    }
}
