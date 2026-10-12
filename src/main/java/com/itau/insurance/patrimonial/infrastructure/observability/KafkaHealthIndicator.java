package com.itau.insurance.patrimonial.infrastructure.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("kafka")
public class KafkaHealthIndicator implements HealthIndicator {

    @Value("${spring.kafka.bootstrap-servers:}")
    private String bootstrapServers;

    @Override
    public Health health() {
        if (bootstrapServers == null || bootstrapServers.isBlank()) {
            return Health.down()
                    .withDetail("configured", false)
                    .build();
        }

        return Health.up()
                .withDetail("configured", true)
                .withDetail("bootstrapServers", bootstrapServers)
                .build();
    }
}
