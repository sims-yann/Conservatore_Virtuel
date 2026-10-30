package I2new.group2.model;

/**
 * Anything the school can charge for: services (packages, rentals) and lessons.
 * An {@link Invoice} only knows its items as {@code Billable}s (polymorphic collection).
 */
public interface Billable {

    double calculatePrice();

    /** Text printed on an invoice line. */
    String getBillingDescription();
}
