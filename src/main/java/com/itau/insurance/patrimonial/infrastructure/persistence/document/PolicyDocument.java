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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "policies")
@CompoundIndexes({
    @CompoundIndex(name = "idx_customer_status", def = "{'customerId': 1, 'status': 1}"),
    @CompoundIndex(name = "idx_status_created", def = "{'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "idx_external_id", def = "{'externalId': 1}")
})
public class PolicyDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @JsonProperty("_id")
    private String id;

    @Indexed(unique = true, sparse = true)
    @Field("policy_number")
    private String policyNumber;

    @Field("policy_code")
    private String policyCode;

    @Indexed
    @Field("customer_id")
    private String customerId;

    @Field("customer_name")
    private String customerName;

    @Field("customer_email")
    private String customerEmail;

    @Field("cpf")
    private String cpf;

    @Field("status")
    @Indexed
    private String status;

    @Field("policy_type")
    private String policyType;

    @Field("coverage_type")
    private String coverageType;

    @Field("property_description")
    private String propertyDescription;

    @Field("property_address")
    private String propertyAddress;

    @Field("idempotency_key")
    @Indexed(unique = true, sparse = true)
    private String idempotencyKey;

    @Field("coverage_limit")
    private BigDecimal coverageLimit;

    @Field("monthly_premium")
    private BigDecimal monthlyPremium;

    @Field("annual_premium")
    private BigDecimal annualPremium;

    @Field("effective_date")
    private LocalDateTime effectiveDate;

    @Field("expiration_date")
    private LocalDateTime expirationDate;

    @Field("start_date")
    @Indexed
    private LocalDateTime startDate;

    @Field("end_date")
    private LocalDateTime endDate;

    @Field("created_at")
    @Indexed
    private LocalDateTime createdAt;

    @Field("updated_at")
    private LocalDateTime updatedAt;

    @Field("created_by")
    private String createdBy;

    @Field("updated_by")
    private String updatedBy;

    @Field("external_id")
    @Indexed
    private String externalId;

    @Field("external_system")
    private String externalSystem;

    @Field("sync_status")
    private String syncStatus;

    @Field("last_sync_at")
    private LocalDateTime lastSyncAt;

    @Field("metadata")
    private Map<String, Object> metadata;

    @Field("beneficiaries")
    private List<BeneficiaryData> beneficiaries;

    @Field("documents")
    private List<DocumentData> documents;

    @Field("active")
    private Boolean active;

    @Field("deleted")
    private Boolean deleted;

    @Field("version")
    @Builder.Default
    private Long version = 0L;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BeneficiaryData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Field("name")
        private String name;

        @Field("cpf")
        private String cpf;

        @Field("relationship")
        private String relationship;

        @Field("percentage")
        private BigDecimal percentage;

        @Field("created_at")
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentData implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        @Field("id")
        private String id;

        @Field("type")
        private String type;

        @Field("url")
        private String url;

        @Field("file_name")
        private String fileName;

        @Field("size")
        private Long size;

        @Field("uploaded_at")
        private LocalDateTime uploadedAt;

        @Field("stored_in")
        private String storedIn;
    }
}
