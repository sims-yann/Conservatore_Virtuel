package I2new.group2.exception;

/** Sequence diagram: a resource is unavailable or already used at that time. */
public class ScheduleConflictException extends RuntimeException {

    public ScheduleConflictException(String message) {
        super(message);
    }
}
