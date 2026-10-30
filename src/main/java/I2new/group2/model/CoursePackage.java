package I2new.group2.model;

import java.time.LocalDate;

public class CoursePackage extends Service {

    private int totalHours;
    private LocalDate startDate;
    private LocalDate endDate;
    private PackageType type;

    public CoursePackage(int id, String name, double price, int totalHours,
                         LocalDate startDate, LocalDate endDate, PackageType type) {
        super(id, name, price);
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Package end date must be on or after its start date");
        }
        if (type == PackageType.STANDARD && totalHours <= 0) {
            throw new IllegalArgumentException("A standard package needs a positive number of hours");
        }
        this.totalHours = type == PackageType.UNLIMITED ? 0 : totalHours;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = type;
    }

    /** Copy constructor. */
    public CoursePackage(CoursePackage other) {
        super(other);
        this.totalHours = other.totalHours;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.type = other.type;
    }

    /** Business rule 1: package hours expire at the package end date. */
    public boolean isExpired() {
        return isExpired(LocalDate.now());
    }

    public boolean isExpired(LocalDate date) {
        return date.isAfter(endDate);
    }

    public boolean isUnlimited() {
        return type == PackageType.UNLIMITED;
    }

    @Override
    public double calculatePrice() {
        return getPrice();
    }

    @Override
    public String describe() {
        String hours = isUnlimited() ? "unlimited lessons" : totalHours + "h";
        return "Package " + hours + ", valid " + startDate + " to " + endDate + (isExpired() ? " [EXPIRED]" : "");
    }

    public int getTotalHours() { return totalHours; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public PackageType getType() { return type; }
}
