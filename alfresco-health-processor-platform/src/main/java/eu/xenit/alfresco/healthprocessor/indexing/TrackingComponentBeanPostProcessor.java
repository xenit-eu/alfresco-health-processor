//package eu.xenit.alfresco.healthprocessor.indexing;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
//import org.springframework.beans.factory.config.ConstructorArgumentValues;
//import org.springframework.beans.factory.config.RuntimeBeanReference;
//import org.springframework.beans.factory.support.BeanDefinitionRegistry;
//import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
//import org.springframework.beans.factory.support.GenericBeanDefinition;
//
//@Slf4j
//public class TrackingComponentBeanPostProcessor implements BeanDefinitionRegistryPostProcessor {
//
//    static final String TRACKING_COMPONENT_BEAN_ID = "eu.xenit.alfresco.healthprocessor.indexing.TrackingComponent";
//
//    public static final String ALF_TRACKING_COMPONENT_BEAN_ID_BEFORE_7 = "solrTrackingComponent";
//    public static final String ALF_TRACKING_COMPONENT_BEAN_ID_AFTER_7 = "searchTrackingComponent";
//
//    /*
//     * Note: it is not possible to fetch the 'solr- or searchTrackingComponent' beans from this BeanDefinitionRegistry,
//     * because this BeanDefinitionRegistryPostProcessor runs in the SubSystem child context.
//     */
//    @Override
//    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
//        GenericBeanDefinition alfrescoTrackingComponentBean = new GenericBeanDefinition();
//        ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
//
//        if (classAvailable("org.alfresco.repo.solr.SOLRTrackingComponent")) {
//            alfrescoTrackingComponentBean.setBeanClass(AlfrescoTrackingComponent.class);
//            constructorArgumentValues
//                    .addGenericArgumentValue(new RuntimeBeanReference(ALF_TRACKING_COMPONENT_BEAN_ID_BEFORE_7));
//        } else if (classAvailable("org.alfresco.repo.search.SearchTrackingComponent")) {
//            alfrescoTrackingComponentBean.setBeanClass(
//                    getClazzForName("eu.xenit.alfresco.healthprocessor.indexing.Alfresco7TrackingComponent"));
//            constructorArgumentValues
//                    .addGenericArgumentValue(new RuntimeBeanReference(ALF_TRACKING_COMPONENT_BEAN_ID_AFTER_7));
//        } else {
//            throw new IllegalStateException("SOLR- nor SearchTrackingComponent class available on the classpath");
//        }
//
//        alfrescoTrackingComponentBean.setConstructorArgumentValues(constructorArgumentValues);
//        registry.registerBeanDefinition(TRACKING_COMPONENT_BEAN_ID, alfrescoTrackingComponentBean);
//
//    }
//
//    @Override
//    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//        // no need to modify the BeanFactory
//    }
//
//    private static Class<?> getClazzForName(final String className) {
//        try {
//            return Class.forName(className);
//        } catch (ClassNotFoundException e) {
//            String message = "Class '" + className + "' not available on the classpath";
//            throw new IllegalStateException(message, e);
//        }
//    }
//
//    private static boolean classAvailable(String className) {
//        try {
//            Class.forName(className);
//            return true;
//        } catch (ClassNotFoundException e) {
//            log.debug("Class {} not found", className, e);
//            return false;
//        }
//    }
//}
