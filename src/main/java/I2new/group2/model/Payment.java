package I2new.group2.model;

import java.time.LocalDate;

public class Payment {

    private int id;
    private double amount;
    private LocalDate paymentDate;

    public Payment(int id, double amount, LocalDate paymentDate) {
        this.id = id;
        this.amount = amount;
        this.paymentDate = paymentDate;
        recordPayment();
    }

    /** Copy constructor. */
    public Payment(Payment other) {
        this(other.id, other.amount, other.paymentDate);
    }

    /** Validates the payment; a payment without date is dated today. */
    public void recordPayment() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        if (paymentDate == null) {
            paymentDate = LocalDate.now();
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getAmount() { return amount; }
    public LocalDate getPaymentDate() { return paymentDate; }

    @Override
    public String toString() {
        return String.format("#%-3d %s  %8.2f EUR", id, paymentDate, amount);
    }
}
