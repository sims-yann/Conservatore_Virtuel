package I2new.group2.model;

public class Instrument {

    private int id;
    private String name;
    private String type;
    private boolean available;

    public Instrument(int id, String name, String type, boolean available) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Instrument name is required");
        }
        this.id = id;
        this.name = name.trim();
        this.type = type;
        this.available = available;
    }

    /** Copy constructor. */
    public Instrument(Instrument other) {
        this(other.id, other.name, other.type, other.available);
    }

    /** False when the instrument is out of service and cannot be used for a lesson. */
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) { this.available = available; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public String getType() { return type; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Instrument other && other.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
