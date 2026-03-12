package com.chrionline.shared.models;

import com.chrionline.shared.enums.PaymentMode;
import com.chrionline.shared.enums.PaymentStatus;
import java.time.LocalDateTime;

public class Payment {

    private String paymentId;
    private String orderId;
    private PaymentMode method;
    private PaymentStatus status;
    private double amount;
    private String cardLastFour;
    private String transactionRef;
    private LocalDateTime paidAt;

    public Payment() {}

    public Payment(String paymentId, String orderId, PaymentMode method, PaymentStatus status,
                   double amount, String cardLastFour, String transactionRef, LocalDateTime paidAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.method = method;
        this.status = status;
        this.amount = amount;
        this.cardLastFour = cardLastFour;
        this.transactionRef = transactionRef;
        this.paidAt = paidAt;
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public PaymentMode getMethod() { return method; }
    public void setMethod(PaymentMode method) { this.method = method; }

    public PaymentStatus getStatus()                              { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCardLastFour() { return cardLastFour; }
    public void setCardLastFour(String cardLastFour) { this.cardLastFour = cardLastFour; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public boolean isSuccessful() { return this.status == PaymentStatus.ACCEPTE; }

    @Override
    public String toString() {
        return "Payment{id='" + paymentId + "', status=" + status + ", amount=" + amount + "}";
    }
}