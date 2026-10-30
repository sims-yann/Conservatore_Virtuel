package I2new.group2.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Lesson extends ScheduledActivity implements Billable {

    /** Business rule 2: cancelling less than this many hours before the start consumes the hours. */
    public static final long FREE_CANCELLATION_HOURS = 24;

    private LessonType type;
    private Teacher teacher;
    private Room room;
    private Instrument instrument;
    private final List<Student> participants = new ArrayList<>();
    private boolean lateCancellation;

    public Lesson(int id, LocalDateTime dateTime, double duration, ActivityStatus status,
                  LessonType type, Teacher teacher, Room room, Instrument instrument) {
        super(id, dateTime, duration, status);
        if (type == null || teacher == null || room == null || instrument == null) {
            throw new IllegalArgumentException("A lesson needs a type, a teacher, a room and an instrument");
        }
        this.type = type;
        this.teacher = teacher;
        this.room = room;
        this.instrument = instrument;
    }

    /** Copy constructor: participants and resources are copied, not shared. */
    public Lesson(Lesson other) {
        super(other);
        this.type = other.type;
        this.teacher = new Teacher(other.teacher);
        this.room = new Room(other.room);
        this.instrument = new Instrument(other.instrument);
        for (Student student : other.participants) {
            this.participants.add(new Student(student));
        }
        this.lateCancellation = other.lateCancellation;
    }

    public void addStudent(Student student) {
        if (participants.stream().anyMatch(p -> p.getId() == student.getId())) {
            throw new IllegalArgumentException(student.getFullName() + " is already in this lesson");
        }
        if (type == LessonType.INDIVIDUAL && !participants.isEmpty()) {
            throw new IllegalStateException("An individual lesson has only one student");
        }
        if (!room.canHost(participants.size() + 1)) {
            throw new IllegalStateException("Room " + room.getName() + " can host only " + room.getCapacity() + " students");
        }
        participants.add(student);
    }

    /**
     * Hours taken from EACH participant's package.
     * Business rule 4: a group lesson consumes the lesson duration from every participant.
     */
    public double calculateConsumedHours() {
        return getDuration();
    }

    /** Price of the lesson for one student paying without a package. */
    @Override
    public double calculatePrice() {
        return teacher.getHourlyRate() * getDuration();
    }

    @Override
    public String getBillingDescription() {
        return String.format("%s lesson #%d (%s, %s, %.1fh)", type == LessonType.GROUP ? "Group" : "Individual",
                getId(), instrument.getName(), getDateTime().toLocalDate(), getDuration());
    }

    /** True if cancelling at {@code when} is less than 24h before the start. */
    public boolean isLateCancellation(LocalDateTime when) {
        return when.isAfter(getDateTime().minusHours(FREE_CANCELLATION_HOURS));
    }

    @Override
    public void cancel(LocalDateTime when) {
        boolean late = isLateCancellation(when);
        super.cancel(when);
        this.lateCancellation = late;
    }

    /** After {@link #cancel(LocalDateTime)}: whether the participants' hours stay consumed. */
    public boolean wasLateCancellation() {
        return lateCancellation;
    }

    @Override
    public String getActivityType() {
        return "Lesson";
    }

    @Override
    public String describe() {
        String students = participants.stream().map(Student::getFullName).collect(Collectors.joining(", "));
        return String.format("%s %s with %s in %s - students: %s",
                type, instrument.getName(), teacher.getFullName(), room.getName(), students.isEmpty() ? "-" : students);
    }

    public LessonType getType() { return type; }
    public Teacher getTeacher() { return teacher; }
    public Room getRoom() { return room; }
    public Instrument getInstrument() { return instrument; }
    public List<Student> getParticipants() { return Collections.unmodifiableList(participants); }
}
