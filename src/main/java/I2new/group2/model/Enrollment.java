package I2new.group2.model;

import java.time.LocalDate;

/**
 * A course package bought by a student, with the hours still available on it.
 */
public class Enrollment {

    private int id;
    private int studentId;
    private CoursePackage coursePackage;
    private LocalDate purchaseDate;
    private double remainingHours;

    public Enrollment(int id, int studentId, CoursePackage coursePackage, LocalDate purchaseDate, double remainingHours) {
        if (coursePackage == null) {
            throw new IllegalArgumentException("Course package is required");
        }
        if (remainingHours < 0) {
            throw new IllegalArgumentException("Remaining hours cannot be negative");
        }
        this.id = id;
        this.studentId = studentId;
        this.coursePackage = coursePackage;
        this.purchaseDate = purchaseDate;
        this.remainingHours = remainingHours;
    }

    /** New purchase: all the package hours are available. */
    public Enrollment(int studentId, CoursePackage coursePackage, LocalDate purchaseDate) {
        this(0, studentId, coursePackage, purchaseDate, coursePackage.getTotalHours());
    }

    /** Copy constructor. */
    public Enrollment(Enrollment other) {
        this(other.id, other.studentId, new CoursePackage(other.coursePackage), other.purchaseDate, other.remainingHours);
    }

    public boolean isValid() {
        return isValid(LocalDate.now());
    }

    /** The package can be used on {@code date}: started and not expired. */
    public boolean isValid(LocalDate date) {
        return !date.isBefore(coursePackage.getStartDate()) && !coursePackage.isExpired(date);
    }

    public boolean isUnlimited() {
        return coursePackage.isUnlimited();
    }

    public boolean hasEnoughHours(double hours) {
        return isUnlimited() || remainingHours + 1e-9 >= hours;
    }

    public void consumeHours(double hours) {
        consumeHours(hours, LocalDate.now());
    }

    /** Uses hours for a lesson taking place on {@code date}. Expired hours cannot be used. */
    public void consumeHours(double hours, LocalDate date) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be positive");
        }
        if (!isValid(date)) {
            throw new IllegalStateException("Package '" + coursePackage.getName() + "' is not valid on " + date);
        }
        if (!hasEnoughHours(hours)) {
            throw new IllegalStateException("Not enough hours left on '" + coursePackage.getName() + "'");
        }
        if (!isUnlimited()) {
            remainingHours -= hours;
        }
    }

    /** Gives hours back (lesson cancelled more than 24h before it starts). */
    public void refundHours(double hours) {
        if (!isUnlimited()) {
            remainingHours = Math.min(coursePackage.getTotalHours(), remainingHours + hours);
        }
    }

    public double getRemainingHours() {
        return remainingHours;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getStudentId() { return studentId; }
    public CoursePackage getCoursePackage() { return coursePackage; }
    public LocalDate getPurchaseDate() { return purchaseDate; }

    @Override
    public String toString() {
        String hours = isUnlimited() ? "unlimited" : String.format("%.1f/%dh left", remainingHours, coursePackage.getTotalHours());
        return String.format("#%-3d %-28s bought %s  %-14s %s", id, coursePackage.getName(), purchaseDate, hours,
                isValid() ? "valid until " + coursePackage.getEndDate() : "EXPIRED / not started");
    }
}
