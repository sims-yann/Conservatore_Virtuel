package I2new.group2.model;

import java.time.LocalDateTime;

/**
 * A resource that can be booked for a time slot (teachers, rooms).
 * Used by the scheduling service to validate a lesson before saving it.
 */
public interface Schedulable {

    /** @param duration length of the slot in hours */
    boolean isAvailable(LocalDateTime start, double duration);

    /** End of a slot that starts at {@code start} and lasts {@code duration} hours. */
    static LocalDateTime endOf(LocalDateTime start, double duration) {
        return start.plusMinutes(Math.round(duration * 60));
    }
}
