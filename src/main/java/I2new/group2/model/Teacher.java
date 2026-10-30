package I2new.group2.model;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Teacher extends Person implements Schedulable {

    /** One weekly availability window (row of the teacher_availability table). */
    public record Availability(DayOfWeek day, LocalTime start, LocalTime end) {
        public Availability {
            Objects.requireNonNull(day, "day");
            Objects.requireNonNull(start, "start");
            Objects.requireNonNull(end, "end");
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException("Availability must start before it ends");
            }
        }

        public boolean covers(LocalDateTime slotStart, LocalDateTime slotEnd) {
            return slotStart.getDayOfWeek() == day
                    && slotStart.toLocalDate().equals(slotEnd.toLocalDate())
                    && !slotStart.toLocalTime().isBefore(start)
                    && !slotEnd.toLocalTime().isAfter(end);
        }

        @Override
        public String toString() {
            return day + " " + start + "-" + end;
        }
    }

    private final List<Instrument> specializations = new ArrayList<>();
    private double hourlyRate;
    private final List<String> qualifications = new ArrayList<>();
    private final List<Availability> availability = new ArrayList<>();

    public Teacher(int id, String firstName, String lastName, String email, String phone, double hourlyRate) {
        super(id, firstName, lastName, email, phone);
        setHourlyRate(hourlyRate);
    }

    /** Copy constructor. */
    public Teacher(Teacher other) {
        super(other);
        this.hourlyRate = other.hourlyRate;
        for (Instrument instrument : other.specializations) {
            this.specializations.add(new Instrument(instrument));
        }
        this.qualifications.addAll(other.qualifications);
        this.availability.addAll(other.availability); // records are immutable
    }

    /** Sequence diagram: teaches(instrument) - specialization check. */
    public boolean teaches(Instrument instrument) {
        return specializations.contains(instrument);
    }

    public boolean checkAvailability(LocalDateTime start, double duration) {
        return isAvailable(start, duration);
    }

    @Override
    public boolean isAvailable(LocalDateTime start, double duration) {
        LocalDateTime end = Schedulable.endOf(start, duration);
        return availability.stream().anyMatch(a -> a.covers(start, end));
    }

    public void addSpecialization(Instrument instrument) {
        if (!specializations.contains(instrument)) {
            specializations.add(instrument);
        }
    }

    public void addQualification(String qualification) {
        qualifications.add(requireText(qualification, "Qualification"));
    }

    public void addAvailability(Availability slot) {
        availability.add(slot);
    }

    @Override
    public String getRole() {
        return "Teacher";
    }

    @Override
    public String describe() {
        String instruments = specializations.stream().map(Instrument::getName).collect(Collectors.joining(", "));
        return String.format("%-4s %-22s %6.2f EUR/h  teaches: %s",
                "#" + getId(), getFullName(), hourlyRate, instruments.isEmpty() ? "-" : instruments);
    }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) {
        if (hourlyRate < 0) {
            throw new IllegalArgumentException("Hourly rate cannot be negative");
        }
        this.hourlyRate = hourlyRate;
    }

    public List<Instrument> getSpecializations() { return Collections.unmodifiableList(specializations); }
    public List<String> getQualifications() { return Collections.unmodifiableList(qualifications); }
    public List<Availability> getAvailability() { return Collections.unmodifiableList(availability); }
}
