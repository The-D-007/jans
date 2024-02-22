
package io.jans.configapi.service.auth;

import static io.jans.as.model.util.Util.escapeLog;
import io.jans.as.common.model.common.SimpleUser;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.model.configuration.ApiAppConfiguration;
import io.jans.configapi.util.ApiConstants;
import io.jans.model.JansAttribute;
import io.jans.model.SearchRequest;
import io.jans.orm.model.AttributeType;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
public class AttributeService extends io.jans.as.common.service.AttributeService {

    private static final long serialVersionUID = -820393743995746612L;
    
    @Inject
    private ApiAppConfiguration appConfiguration;
    
    @Inject
    transient ConfigurationService configurationService;
    
    @Override
    protected boolean isUseLocalCache() {
        return false;
    }

    public PagedResult<JansAttribute> searchJansAttributes(SearchRequest searchRequest, String status) {
        if (log.isInfoEnabled()) {
            log.info("Search JansAttributes with searchRequest:{}, status:{}", escapeLog(searchRequest), escapeLog(status));
        }

        Filter activeFilter = null;
        if (ApiConstants.ACTIVE.equalsIgnoreCase(status)) {
            activeFilter = Filter.createEqualityFilter(AttributeConstants.JANS_STATUS, "active");
        } else if (ApiConstants.INACTIVE.equalsIgnoreCase(status)) {
            activeFilter = Filter.createEqualityFilter(AttributeConstants.JANS_STATUS, "inactive");
        }

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                        targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null,
                        targetArray, null);
                Filter nameFilter = Filter.createSubstringFilter(AttributeConstants.JANS_ATTR_NAME, null, targetArray,
                        null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, nameFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }
        
        log.trace("Attributes pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if(searchRequest.getFieldValueMap()!=null && !searchRequest.getFieldValueMap().isEmpty())
        {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                log.trace("dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }  
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters), Filter.createANDFilter(fieldValueFilters));
        }        

        log.trace("Attributes pattern and field searchFilter:{}", searchFilter);
       
        if (activeFilter != null) {
            searchFilter = Filter.createANDFilter(searchFilter, activeFilter);
        }

        log.info("JansAttributes final searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForAttribute(null), JansAttribute.class, searchFilter,
                null, searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }

    public JansAttribute getAttributeUsingDn(String dn) {
        JansAttribute result = null;
        try {
            result = persistenceEntryManager.find(JansAttribute.class, dn);
        } catch (Exception ex) {
            log.error("Failed to load attribute with dn:{}, ex:{}", dn, ex);
        }
        return result;
    }

    public JansAttribute getAttributeUsingName(String claimName) {
        JansAttribute jansAttribute = null;
        try {
            jansAttribute = getByClaimName(claimName);
        } catch (Exception ex) {
            log.error("Failed to load attribute with name:{}, ex:{}", claimName, ex);
        }
        return jansAttribute;
    }
    
    public boolean validateAttributeDefinition(String attributeName) {
        log.info(" Validate attributeName:{}, getPersistenceType():{}, appConfiguration:{}", attributeName, getPersistenceType(), appConfiguration);
        boolean isValidAttribute = false;
        try {
            
            //return if isCustomAttributeValidationEnabled not enabled
            if(appConfiguration!=null && !appConfiguration.isCustomAttributeValidationEnabled()) {
                return true;                
            }

            log.info(" attributeName:{}, persistenceEntryManager.getAttributeType(ou=people,o=jans, SimpleUser.class,attributeName)():{}", attributeName, persistenceEntryManager.getAttributeType("ou=people,o=jans", SimpleUser.class,
                    attributeName));
            AttributeType attributeType = persistenceEntryManager.getAttributeType("ou=people,o=jans", SimpleUser.class,
                    attributeName);
            log.info(" attributeName:{}, attributeType():{}", attributeName, attributeType);

            if (attributeType != null) {
                isValidAttribute = true;
            }
        } catch (Exception ex) {
            log.error("Exception by ORM while validating attribute is:{}", ex);
            isValidAttribute = true;
        }
        return isValidAttribute;
    }

    private String getPersistenceType() {
        return configurationService.getPersistenceType();
    }

}