package ru.netology.cardtocardservice.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class TransferAmount {
    @PositiveOrZero(message = "Сумма перевода не может быть отрицательной")
    private Integer value;


    @NotBlank
    @Pattern(
            regexp = "RUB",
            message = "Значение вида валюты для перевода может быть только RUB"
    )
    private String currency;
}
