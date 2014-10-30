package alien4cloud.paas;

import alien4cloud.model.cloud.CloudResourceMatcherConfig;

/**
 * This interface defines contract for PaaS provider which do not discover resources automatically.
 * User will have to configure the mapping between alien resources and PaaS (IaaS) resources
 *
 * @author Minh Khang VU
 */
public interface IManualResourceMatcherPaaSProvider {

    /**
     * Call to initialize or notify the paaS provider about configuration change
     *
     * @param config the config to take into account
     */
    void updateMatcherConfig(CloudResourceMatcherConfig config);
}
