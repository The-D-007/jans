package org.gluu.oxauth.service.logger;

import io.jans.as.model.configuration.AppConfiguration;
import org.gluu.oxauth.util.ServerUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Logger service
 *
 * @author Yuriy Movchan Date: 08/19/2018
 */
@ApplicationScoped
@Named
public class LoggerService extends io.jans.service.logger.LoggerService {

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public boolean isDisableJdkLogger() {
        return ServerUtil.isTrue(appConfiguration.getDisableJdkLogger());
    }

    @Override
    public String getLoggingLevel() {
        return appConfiguration.getLoggingLevel();
    }

    @Override
    public String getExternalLoggerConfiguration() {
        return appConfiguration.getExternalLoggerConfiguration();
    }

    @Override
    public String getLoggingLayout() {
        return appConfiguration.getLoggingLayout();
    }

}
