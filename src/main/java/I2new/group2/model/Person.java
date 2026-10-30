package I2new.group2.model;

/**
 * Common data and behaviour of every person known by the school.
 */
public abstract class Person {

    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    protected Person(int id, String firstName, String lastName, String email, String phone) {
        this.id = id;
        this.firstName = requireText(firstName, "First name");
        this.lastName = requireText(lastName, "Last name");
        this.email = requireEmail(email);
        this.phone = phone;
    }

    /** Copy constructor. */
    protected Person(Person other) {
        this(other.id, other.firstName, other.lastName, other.email, other.phone);
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** "Student" or "Teacher" - resolved by dynamic dispatch. */
    public abstract String getRole();

    /** One-line description specific to each kind of person. */
    public abstract String describe();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = requireText(firstName, "First name"); }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = requireText(lastName, "Last name"); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = requireEmail(email); }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    protected static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String requireEmail(String email) {
        String value = requireText(email, "Email");
        if (!value.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
        return value;
    }

    @Override
    public String toString() {
        return "#" + id + " " + getFullName() + " (" + getRole() + ")";
    }
}
