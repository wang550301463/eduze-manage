package com.eduze.platform.runtime;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "eduze.runtime")
public class RuntimeProperties {
    private String serviceName;
    private String serviceToken;
    private Map<String, String> callers = new HashMap<>();
    private Map<String, String> services = new HashMap<>();

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String value) {
        serviceName = value;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(String value) {
        serviceToken = value;
    }

    public Map<String, String> getCallers() {
        return callers;
    }

    public void setCallers(Map<String, String> value) {
        callers = value;
    }

    public Map<String, String> getServices() {
        return services;
    }

    public void setServices(Map<String, String> value) {
        services = value;
    }
}
