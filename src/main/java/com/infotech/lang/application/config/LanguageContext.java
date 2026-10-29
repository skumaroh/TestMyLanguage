package com.infotech.lang.application.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Component that provides language context for the current deployment.
 * 
 * Language is determined by the APP_LANGUAGE environment variable when available.
 * If APP_LANGUAGE is not provided, the language is derived from the datasource URL.
 *
 * Example datasource URL: jdbc:postgresql://.../tamil?user=...&password=...
 */
@Component
public class LanguageContext {
    
    @Value("${app.language:}")
    private String language;
    
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;
    
    /**
     * Get the current deployment's language.
     * @return Language code (e.g., "tamil", "hindi")
     */
    public String getLanguage() {
        if (language != null && !language.trim().isEmpty()) {
            return language.trim().toLowerCase();
        }
        return deriveLanguageFromDatasourceUrl();
    }
    
    private String deriveLanguageFromDatasourceUrl() {
        if (datasourceUrl == null || datasourceUrl.trim().isEmpty()) {
            return "tamil";
        }
        String trimmed = datasourceUrl.trim();
        int slashIndex = trimmed.indexOf("/", trimmed.indexOf("//") + 2);
        if (slashIndex == -1) {
            return "tamil";
        }
        int questionIndex = trimmed.indexOf("?", slashIndex + 1);
        String dbName = questionIndex == -1
                ? trimmed.substring(slashIndex + 1)
                : trimmed.substring(slashIndex + 1, questionIndex);
        if (dbName == null || dbName.trim().isEmpty()) {
            return "tamil";
        }
        return dbName.trim().toLowerCase();
    }
}
