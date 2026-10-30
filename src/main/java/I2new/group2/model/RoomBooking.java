package I2new.group2.model;

import java.time.LocalDateTime;

public class RoomBooking extends ScheduledActivity {

    private Room room;

    public RoomBooking(int id, LocalDateTime dateTime, double duration, ActivityStatus status, Room room) {
        super(id, dateTime, duration, status);
        if (room == null) {
            throw new IllegalArgumentException("A booking needs a room");
        }
        this.room = room;
    }

    /** Copy constructor. */
    public RoomBooking(RoomBooking other) {
        super(other);
        this.room = new Room(other.room);
    }

    /** Checks the slot fits in the room's opening hours (overlaps are checked by the scheduling service). */
    public void bookRoom() {
        if (!room.isAvailable(getDateTime(), getDuration())) {
            throw new IllegalStateException(room.getName() + " is closed at that time (open "
                    + Room.OPENING + "-" + Room.CLOSING + ")");
        }
    }

    @Override
    public String getActivityType() {
        return "Room booking";
    }

    @Override
    public String describe() {
        return "Reserved: " + room.getName();
    }

    public Room getRoom() { return room; }
}
