package I2new.group2.model;

/**
 * Something the school sells (course packages, rentals).
 * Concrete subclasses decide how the price is computed and described.
 */
public abstract class Service implements Billable {

    private int id;
    private String name;
    private double price;

    protected Service(int id, String name, double price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service name is required");
        }
        if (price < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        this.id = id;
        this.name = name.trim();
        this.price = price;
    }

    /** Copy constructor. */
    protected Service(Service other) {
        this(other.id, other.name, other.price);
    }

    /** Human readable details specific to the kind of service. */
    public abstract String describe();

    @Override
    public String getBillingDescription() {
        return name;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public double getPrice() { return price; }

    @Override
    public String toString() {
        return String.format("#%-3d %-28s %8.2f EUR  %s", id, name, calculatePrice(), describe());
    }
}
