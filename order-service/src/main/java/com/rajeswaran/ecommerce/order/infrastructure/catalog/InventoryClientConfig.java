package com.rajeswaran.ecommerce.order.infrastructure.catalog;

import com.rajeswaran.ecommerce.platform.messaging.MessageHeaders;
import com.rajeswaran.ecommerce.platform.security.CurrentUser;
import org.slf4j.MDC;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * {@code lb://}-style client: "http://inventory-service" is resolved through Eureka by Spring Cloud LoadBalancer.
 * The caller's JWT and correlation id are forwarded so Inventory can authenticate and trace the call.
 */
@Configuration
class InventoryClientConfig {

    /**
     * defaultCandidate = false: only injection points qualified with @LoadBalanced get this builder. Otherwise it
     * would replace Boot's default RestClient.Builder and Eureka's own HTTP client would try to load-balance
     * "localhost:9761" as if it were a service id.
     */
    @Bean(defaultCandidate = false)
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().requestFactory(factory);
    }

    @Bean
    InventoryApi inventoryApi(@LoadBalanced RestClient.Builder builder) {
        RestClient client = builder
                .baseUrl("http://inventory-service")
                .requestInterceptor((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(CurrentUser.bearerToken());
                    String correlationId = MDC.get(MessageHeaders.CORRELATION_MDC_KEY);
                    if (correlationId != null) {
                        request.getHeaders().set(MessageHeaders.CORRELATION_HTTP_HEADER, correlationId);
                    }
                    request.getHeaders().set(HttpHeaders.ACCEPT, "application/json");
                    return execution.execute(request, body);
                })
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(client)).build().createClient(InventoryApi.class);
    }
}
