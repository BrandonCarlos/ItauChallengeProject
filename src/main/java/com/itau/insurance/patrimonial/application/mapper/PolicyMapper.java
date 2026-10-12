package com.itau.insurance.patrimonial.application.mapper;

import com.itau.insurance.patrimonial.application.dto.PolicyRequestDTO;
import com.itau.insurance.patrimonial.application.dto.PolicyResponseDTO;
import com.itau.insurance.patrimonial.domain.model.Policy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;

/**
 * MapStruct mapper for converting between Policy domain entities and DTOs.
 * This mapper handles the transformation of data between the domain layer and application layer,
 * maintaining separation of concerns and providing a clear contract for data transformation.
 */
@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PolicyMapper {

    /**
     * Converts a PolicyRequestDTO to a Policy domain entity
     * 
     * @param requestDTO the request data
     * @return the domain entity
     */
    @Mapping(target = "policyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", expression = "java(com.itau.insurance.patrimonial.domain.model.Policy.PolicyStatus.ACTIVE)")
    Policy requestDTOToDomain(PolicyRequestDTO requestDTO);

    /**
     * Converts a Policy domain entity to a PolicyResponseDTO
     * 
     * @param policy the domain entity
     * @return the response data
     */
    @Mapping(target = "status", expression = "java(policy.getStatus() != null ? policy.getStatus().toString() : null)")
    PolicyResponseDTO domainToResponseDTO(Policy policy);

    /**
     * Updates a Policy domain entity with data from a PolicyRequestDTO
     * 
     * @param requestDTO the request data
     * @param policy the domain entity to update
     */
    @Mapping(target = "policyId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updatePolicyFromRequestDTO(PolicyRequestDTO requestDTO, @MappingTarget Policy policy);
}
