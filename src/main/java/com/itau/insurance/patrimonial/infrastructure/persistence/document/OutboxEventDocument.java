package com.itau.insurance.patrimonial.infrastructure.persistence.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "outbox_events")
@CompoundIndexes({
    @CompoundIndex(name = "idx_status_created", def = "{'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_aggregate_status", def = "{'aggregateId': 1, 'status': 1}")
})
public class OutboxEventDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @JsonProperty("_id")
    private String id;

    @Field("aggregate_id")
    @Indexed
    private String aggregateId;

    @Field("aggregate_type")
    private String aggregateType;

    @Field("event_type")
    @Indexed
    private String eventType;

    @Field("event_data")
    private Map<String, Object> eventData;

    @Field("status")
    @Indexed
    private String status;

    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;

    @Field("published_at")
    private LocalDateTime publishedAt;

    @Field("failed_attempt_count")
    @Builder.Default
    private Integer failedAttemptCount = 0;

    @Field("last_error_message")
    private String lastErrorMessage;

    @Field("last_error_at")
    private LocalDateTime lastErrorAt;

    @Field("retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Field("next_retry_at")
    private LocalDateTime nextRetryAt;

    @Field("partition_key")
    private String partitionKey;

    @Field("correlation_id")
    @Indexed
    private String correlationId;

    @Field("causation_id")
    private String causationId;

    @Field("metadata")
    private Map<String, Object> metadata;

    @Field("source_system")
    private String sourceSystem;

    @Field("version")
    @Builder.Default
    private Long version = 0L;

    @Field("deleted")
    @Builder.Default
    private Boolean deleted = false;

    public enum EventStatus {
        PENDING("PENDING"),
        PUBLISHED("PUBLISHED"),
        FAILED("FAILED"),
        ARCHIVED("ARCHIVED");

        private final String value;

        EventStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
