package com.eduze.platform.notification;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "eduze.wechat")
public class NotificationProperties {
    public static class Template {
        private String templateId;
        private Map<String, String> fields = new HashMap<>();

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String value) {
            templateId = value;
        }

        public Map<String, String> getFields() {
            return fields;
        }

        public void setFields(Map<String, String> value) {
            fields = value;
        }
    }

    private String appId;
    private String appSecret;
    private Map<String, Template> templates = new HashMap<>();

    public String getAppId() {
        return appId;
    }

    public void setAppId(String value) {
        appId = value;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String value) {
        appSecret = value;
    }

    public Map<String, Template> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, Template> value) {
        templates = value;
    }
}
