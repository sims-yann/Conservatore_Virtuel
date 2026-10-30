package I2new.group2.model;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class Room implements Schedulable {

    /** Opening hours of the building. */
    public static final LocalTime OPENING = LocalTime.of(8, 0);
    public static final LocalTime CLOSING = LocalTime.of(22, 0);

    private int id;
    private String name;
    private int capacity;

    public Room(int id, String name, int capacity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Room name is required");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Room capacity must be positive");
        }
        this.id = id;
        this.name = name.trim();
        this.capacity = capacity;
    }

    /** Copy constructor. */
    public Room(Room other) {
        this(other.id, other.name, other.capacity);
    }

    /**
     * The room can be used if the slot fits in the opening hours of a single day.
     * Overlaps with other activities are checked against the database by the scheduling service.
     */
    @Override
    public boolean isAvailable(LocalDateTime start, double duration) {
        LocalDateTime end = Schedulable.endOf(start, duration);
        return start.toLocalDate().equals(end.toLocalDate())
                && !start.toLocalTime().isBefore(OPENING)
                && !end.toLocalTime().isAfter(CLOSING);
    }

    public boolean canHost(int people) {
        return people <= capacity;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }

    @Override
    public String toString() {
        return name + " (capacity " + capacity + ")";
    }
}
