package I2new.group2.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Invoice {

    /** Rounding tolerance for money comparisons. */
    private static final double CENT = 0.005;

    private int id;
    private int studentId;
    private LocalDate date;
    private double totalAmount;
    private final List<Billable> items = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();

    public Invoice(int id, int studentId, LocalDate date) {
        this.id = id;
        this.studentId = studentId;
        this.date = date == null ? LocalDate.now() : date;
    }

    /** Copy constructor: payments are copied, items (immutable for billing purposes) are shared. */
    public Invoice(Invoice other) {
        this(other.id, other.studentId, other.date);
        this.items.addAll(other.items);
        for (Payment payment : other.payments) {
            this.payments.add(new Payment(payment));
        }
        this.totalAmount = other.totalAmount;
    }

    public void addItem(Billable item) {
        items.add(item);
        calculateTotal();
    }

    /** Sum of the items' prices - each item computes its own price (dynamic dispatch). */
    public double calculateTotal() {
        totalAmount = items.stream().mapToDouble(Billable::calculatePrice).sum();
        return totalAmount;
    }

    public double getAmountPaid() {
        return payments.stream().mapToDouble(Payment::getAmount).sum();
    }

    public double getOutstandingBalance() {
        return calculateTotal() - getAmountPaid();
    }

    public boolean isPaid() {
        return getOutstandingBalance() < CENT;
    }

    public void addPayment(Payment payment) {
        if (payment.getAmount() > getOutstandingBalance() + CENT) {
            throw new IllegalArgumentException(String.format(
                    "Payment of %.2f EUR exceeds the outstanding balance of %.2f EUR",
                    payment.getAmount(), getOutstandingBalance()));
        }
        payments.add(payment);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public LocalDate getDate() { return date; }
    public double getTotalAmount() { return totalAmount; }
    public List<Billable> getItems() { return Collections.unmodifiableList(items); }
    public List<Payment> getPayments() { return Collections.unmodifiableList(payments); }

    @Override
    public String toString() {
        return String.format("Invoice #%-3d %s  total %8.2f  paid %8.2f  due %8.2f  %s", id, date,
                calculateTotal(), getAmountPaid(), getOutstandingBalance(), isPaid() ? "PAID" : "OPEN");
    }
}
