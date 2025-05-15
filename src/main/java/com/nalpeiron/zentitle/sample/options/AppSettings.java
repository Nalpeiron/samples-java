package com.nalpeiron.zentitle.sample.options;


import com.fasterxml.jackson.annotation.JsonProperty;

public class AppSettings {
    @JsonProperty("UseCoreLibrary")
    private boolean useCoreLibrary;

    @JsonProperty("CoreLibPath")
    private String coreLibPath;

    @JsonProperty("Licensing")
    private Licensing licensing;

    public boolean isUseCoreLibrary() {
        return useCoreLibrary;
    }

    public String getCoreLibPath() {
        return coreLibPath;
    }

    public Licensing getLicensing() {
        return licensing;
    }

    public static class Licensing {
        @JsonProperty("ApiUrl")
        private String apiUrl;

        @JsonProperty("TenantId")
        private String tenantId;

        @JsonProperty("TenantRsaKeyModulus")
        private String tenantRsaKeyModulus;

        @JsonProperty("ProductId")
        private String productId;

        public String getApiUrl() {
            return apiUrl;
        }

        public String getTenantId() {
            return tenantId;
        }

        public String getTenantRsaKeyModulus() {
            return tenantRsaKeyModulus;
        }

        public String getProductId() {
            return productId;
        }
    }
}