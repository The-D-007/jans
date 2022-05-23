package io.jans.agama.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.model.EngineConfig;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.service.cdi.event.ConfigurationUpdate;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ServicesFactory {

    @Inject
    private Logger logger;

    @Inject
    private FlowUtils futils;
    
    private ObjectMapper mapper;
    
    private EngineConfig econfig;

    @Produces
    public ObjectMapper mapperInstance() {
        return mapper;
    }
    
    @Produces
    @ApplicationScoped
    public EngineConfig engineConfigInstance() {
        return econfig;
    }
    
    public void updateConfiguration(@Observes @ConfigurationUpdate AppConfiguration appConfiguration) {
        
        try {
            logger.info("Refreshing Agama configuration...");
            BeanUtils.copyProperties(econfig, appConfiguration.getAgamaConfiguration());            
                
            int unauth = appConfiguration.getSessionIdUnauthenticatedUnusedLifetime();
            int effectiveInterruptionTime = econfig.getInterruptionTime();
            if (effectiveInterruptionTime == 0 || effectiveInterruptionTime > unauth) {
                //Ensure interruption time is lower than or equal to unauthenticated unused
                effectiveInterruptionTime = unauth;
                logger.warn("Agama flow interruption time modified to {}", unauth);
            }
            futils.setEffectiveInterruptionTime(effectiveInterruptionTime);
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        
    }

    @PostConstruct
    public void init() {
        mapper = new ObjectMapper();
        econfig = new EngineConfig();
    }
    
}
