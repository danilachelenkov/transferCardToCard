package ru.netology.cardtocardservice.domain;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OperationInfo {
    @NotBlank
    private String operationId;

    @NotBlank
    private String code;
}
