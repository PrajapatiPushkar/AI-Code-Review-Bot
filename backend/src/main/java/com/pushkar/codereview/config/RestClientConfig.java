package com.pushkar.codereview.config;

import com.pushkar.codereview.config.resilience.ResilienceProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder(ResilienceProperties resilienceProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int connectTimeout = resilienceProperties.getTimeouts().getGeminiConnectTimeoutMs();
        int readTimeout = Math.max(resilienceProperties.getTimeouts().getGeminiReadTimeoutMs(), 60000);

        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory);
    }
}
