package I2new.group2.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Student extends Person {

    private String address;
    private LocalDate birthDate;
    private StudentLevel level;
    private final List<Instrument> preferredInstruments = new ArrayList<>();
    private final List<Enrollment> enrollments = new ArrayList<>();

    public Student(int id, String firstName, String lastName, String email, String phone,
                   String address, LocalDate birthDate, StudentLevel level) {
        super(id, firstName, lastName, email, phone);
        this.address = address;
        setBirthDate(birthDate);
        this.level = level == null ? StudentLevel.BEGINNER : level;
    }

    /** Copy constructor: deep-copies the enrollments so the copy cannot alter the original's hours. */
    public Student(Student other) {
        super(other);
        this.address = other.address;
        this.birthDate = other.birthDate;
        this.level = other.level;
        for (Instrument instrument : other.preferredInstruments) {
            this.preferredInstruments.add(new Instrument(instrument));
        }
        for (Enrollment enrollment : other.enrollments) {
            this.enrollments.add(new Enrollment(enrollment));
        }
    }

    /** Adds a purchased package to the student. */
    public void enroll(Enrollment enrollment) {
        if (enrollment.getStudentId() != getId()) {
            throw new IllegalArgumentException("Enrollment belongs to another student");
        }
        enrollments.add(enrollment);
    }

    /** Hours left today on the student's valid, non-unlimited packages. */
    public double getRemainingHours() {
        return getRemainingHours(LocalDate.now());
    }

    public double getRemainingHours(LocalDate date) {
        return enrollments.stream()
                .filter(e -> e.isValid(date) && !e.isUnlimited())
                .mapToDouble(Enrollment::getRemainingHours)
                .sum();
    }

    public boolean hasUnlimitedPackage(LocalDate date) {
        return enrollments.stream().anyMatch(e -> e.isValid(date) && e.isUnlimited());
    }

    /**
     * Package that should pay for a lesson of {@code hours} on {@code date}
     * (sequence diagram: getValidPackage). Unlimited packages first,
     * then the standard package that expires first.
     */
    public Optional<Enrollment> getValidPackage(LocalDate date, double hours) {
        return enrollments.stream()
                .filter(e -> e.isValid(date) && e.hasEnoughHours(hours))
                .min(Comparator.comparing((Enrollment e) -> !e.isUnlimited())
                        .thenComparing(e -> e.getCoursePackage().getEndDate()));
    }

    public void addPreferredInstrument(Instrument instrument) {
        if (!preferredInstruments.contains(instrument)) {
            preferredInstruments.add(instrument);
        }
    }

    public boolean prefers(Instrument instrument) {
        return preferredInstruments.contains(instrument);
    }

    public Integer getAge() {
        return birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
    }

    @Override
    public String getRole() {
        return "Student";
    }

    @Override
    public String describe() {
        String instruments = preferredInstruments.isEmpty() ? "-"
                : preferredInstruments.stream().map(Instrument::getName).collect(Collectors.joining(", "));
        String hours = hasUnlimitedPackage(LocalDate.now()) ? "unlimited"
                : String.format("%.1fh", getRemainingHours());
        return String.format("%-4s %-22s %-13s age %-3s instruments: %-20s remaining: %s",
                "#" + getId(), getFullName(), level, getAge() == null ? "?" : getAge(), instruments, hours);
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future");
        }
        this.birthDate = birthDate;
    }

    public StudentLevel getLevel() { return level; }
    public void setLevel(StudentLevel level) {
        if (level == null) {
            throw new IllegalArgumentException("Level is required");
        }
        this.level = level;
    }

    public List<Instrument> getPreferredInstruments() {
        return Collections.unmodifiableList(preferredInstruments);
    }

    public List<Enrollment> getEnrollments() {
        return Collections.unmodifiableList(enrollments);
    }
}
