package I2new.group2.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Exam extends ScheduledActivity {

    private String name;
    private int capacity;
    private Instrument instrument;
    private final List<Student> registeredStudents = new ArrayList<>();
    private final List<ExamResult> results = new ArrayList<>();

    public Exam(int id, LocalDateTime dateTime, double duration, ActivityStatus status,
                String name, int capacity, Instrument instrument) {
        super(id, dateTime, duration, status);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Exam name is required");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("Exam capacity must be positive");
        }
        if (instrument == null) {
            throw new IllegalArgumentException("An exam concerns an instrument");
        }
        this.name = name.trim();
        this.capacity = capacity;
        this.instrument = instrument;
    }

    /** Copy constructor. */
    public Exam(Exam other) {
        super(other);
        this.name = other.name;
        this.capacity = other.capacity;
        this.instrument = new Instrument(other.instrument);
        for (Student student : other.registeredStudents) {
            this.registeredStudents.add(new Student(student));
        }
        for (ExamResult result : other.results) {
            this.results.add(new ExamResult(result));
        }
    }

    /** Business rule 3: registration closes when maximum capacity is reached. */
    public boolean isFull() {
        return registeredStudents.size() >= capacity;
    }

    public boolean isRegistered(Student student) {
        return registeredStudents.stream().anyMatch(s -> s.getId() == student.getId());
    }

    /**
     * Follows UML/ExamRegistration.puml: capacity check first, then duplicate check.
     * @return true when the student has been registered
     */
    public boolean registerStudent(Student student) {
        if (getStatus() != ActivityStatus.SCHEDULED) {
            throw new IllegalStateException("Exam registration is closed: the exam is " + getStatus());
        }
        if (isFull()) {
            throw new IllegalStateException("Exam registration is closed");
        }
        if (isRegistered(student)) {
            throw new IllegalStateException("Student is already registered");
        }
        return registeredStudents.add(student);
    }

    /** Adds or replaces the result of a registered student. */
    public void addResult(ExamResult result) {
        if (!isRegistered(result.getStudent())) {
            throw new IllegalStateException(result.getStudent().getFullName() + " is not registered for this exam");
        }
        results.removeIf(r -> r.getStudent().getId() == result.getStudent().getId());
        results.add(result);
    }

    public Optional<ExamResult> getResultFor(Student student) {
        return results.stream().filter(r -> r.getStudent().getId() == student.getId()).findFirst();
    }

    public int getRemainingSeats() {
        return Math.max(0, capacity - registeredStudents.size());
    }

    @Override
    public String getActivityType() {
        return "Exam";
    }

    @Override
    public String describe() {
        return String.format("%s (%s) - %d/%d registered", name, instrument.getName(), registeredStudents.size(), capacity);
    }

    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public Instrument getInstrument() { return instrument; }
    public List<Student> getRegisteredStudents() { return Collections.unmodifiableList(registeredStudents); }
    public List<ExamResult> getResults() { return Collections.unmodifiableList(results); }
}
