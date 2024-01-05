package ru.netology.cardtocardservice.domain;

import lombok.Data;

@Data
public class AccountTransaction extends TransferInfo {
    private ConfirmType commitCode;
    private String transactionProcessedTime;

    public AccountTransaction(TransferInfo transferInfo) {
        this.setCardFromNumber(transferInfo.getCardFromNumber());
        this.setCardFromCVV(transferInfo.getCardFromCVV());
        this.setCardFromValidTill(transferInfo.getCardFromValidTill());
        this.setCardToNumber(transferInfo.getCardToNumber());
        this.setTransactionRegistrationTime(transferInfo.getTransactionRegistrationTime());
        this.setAmount(transferInfo.getAmount());
    }
}


