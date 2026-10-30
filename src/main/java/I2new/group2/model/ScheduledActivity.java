package I2new.group2.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Anything placed in the school's schedule: lessons, room bookings, exams.
 */
public abstract class ScheduledActivity {

    protected static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy HH:mm");

    private int id;
    private LocalDateTime dateTime;
    private double duration;
    private ActivityStatus status;

    protected ScheduledActivity(int id, LocalDateTime dateTime, double duration, ActivityStatus status) {
        if (dateTime == null) {
            throw new IllegalArgumentException("Date and time are required");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        this.id = id;
        this.dateTime = dateTime;
        this.duration = duration;
        this.status = status == null ? ActivityStatus.SCHEDULED : status;
    }

    /** Copy constructor. */
    protected ScheduledActivity(ScheduledActivity other) {
        this(other.id, other.dateTime, other.duration, other.status);
    }

    public LocalDateTime getEndDateTime() {
        return Schedulable.endOf(dateTime, duration);
    }

    /** True if this (non cancelled) activity overlaps the given slot. */
    public boolean overlaps(LocalDateTime start, double otherDuration) {
        return status != ActivityStatus.CANCELLED
                && start.isBefore(getEndDateTime())
                && Schedulable.endOf(start, otherDuration).isAfter(dateTime);
    }

    public void cancel() {
        cancel(LocalDateTime.now());
    }

    /** Cancels the activity at time {@code when}. Subclasses may add rules (see {@link Lesson}). */
    public void cancel(LocalDateTime when) {
        if (status != ActivityStatus.SCHEDULED) {
            throw new IllegalStateException("Only a scheduled activity can be cancelled (status is " + status + ")");
        }
        status = ActivityStatus.CANCELLED;
    }

    public void complete() {
        if (status != ActivityStatus.SCHEDULED) {
            throw new IllegalStateException("Only a scheduled activity can be completed (status is " + status + ")");
        }
        status = ActivityStatus.COMPLETED;
    }

    /** "Lesson", "Room booking" or "Exam". */
    public abstract String getActivityType();

    /** Details specific to each kind of activity (resources, participants...). */
    public abstract String describe();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public LocalDateTime getDateTime() { return dateTime; }
    public double getDuration() { return duration; }
    public ActivityStatus getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("#%-3d %-12s %s  %4.1fh  %-9s  %s",
                id, getActivityType(), dateTime.format(DISPLAY), duration, status, describe());
    }
}
