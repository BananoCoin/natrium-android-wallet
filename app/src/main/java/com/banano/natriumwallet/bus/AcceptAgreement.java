package com.banano.natriumwallet.bus;

public class AcceptAgreement {
    private String agreementId;

    public AcceptAgreement(String agreementId) {
        this.agreementId = agreementId;
    }

    public String getAgreementId() {
        return agreementId;
    }

    public void setAgreementId(String agreementId) {
        this.agreementId = agreementId;
    }
}
