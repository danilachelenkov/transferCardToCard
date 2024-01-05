package ru.netology.cardtocardservice.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import lombok.Data;

@Data
public class TransferInfo {
    @NotBlank
    @Pattern(
            regexp = "[0-9]{16}",
            message = "Номер карты клиента может быть только числовым и должен состоять из 16 символов"
    )
    private String cardFromNumber;

    @NotBlank
    @Pattern(
            regexp = "(0[1-9]|1[012])[0-9]{2}",
            message = "Значение срока действия карты клиента должен быть передан в формате MMYY"
    )
    private String cardFromValidTill;

    //todo можно написать аннотацию, которая бы проверяла валидность карты по текущей дате
    @NotBlank
    @Pattern(
            regexp = "[0-9]{3}",
            message = "CVV может быть только числовым и должен состоять из 3 симвлов"
    )
    private String cardFromCVV;

    @NotBlank
    @Pattern(
            regexp = "[0-9]{16}",
            message = "Номер карты клиента может быть только числовым и должен состоять из 16 символов"
    )
    private String cardToNumber;

    @Valid
    private TransferAmount amount;

    private String transactionRegistrationTime;
    private Integer commissionAmount;
}
