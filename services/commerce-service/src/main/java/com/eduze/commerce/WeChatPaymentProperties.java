package com.eduze.commerce;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "eduze.payment")
public class WeChatPaymentProperties {
    private String appId = "",
            mchId = "",
            merchantSerial = "",
            privateKeyPath = "",
            platformSerial = "",
            platformKeyPath = "",
            apiV3Key = "",
            notifyBaseUrl = "";

    public String getAppId() {
        return appId;
    }

    public void setAppId(String value) {
        appId = value;
    }

    public String getMchId() {
        return mchId;
    }

    public void setMchId(String value) {
        mchId = value;
    }

    public String getMerchantSerial() {
        return merchantSerial;
    }

    public void setMerchantSerial(String value) {
        merchantSerial = value;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String value) {
        privateKeyPath = value;
    }

    public String getPlatformSerial() {
        return platformSerial;
    }

    public void setPlatformSerial(String value) {
        platformSerial = value;
    }

    public String getPlatformKeyPath() {
        return platformKeyPath;
    }

    public void setPlatformKeyPath(String value) {
        platformKeyPath = value;
    }

    public String getApiV3Key() {
        return apiV3Key;
    }

    public void setApiV3Key(String value) {
        apiV3Key = value;
    }

    public String getNotifyBaseUrl() {
        return notifyBaseUrl;
    }

    public void setNotifyBaseUrl(String value) {
        notifyBaseUrl = value;
    }
}
