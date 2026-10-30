package I2new.group2.service;

import I2new.group2.dao.ActivityDao;
import I2new.group2.dao.DatabaseConnection;
import I2new.group2.dao.EnrollmentDao;
import I2new.group2.dao.InstrumentDao;
import I2new.group2.dao.InvoiceDao;
import I2new.group2.dao.RoomDao;
import I2new.group2.dao.StudentDao;
import I2new.group2.dao.TeacherDao;
import I2new.group2.exception.InvalidSpecializationException;
import I2new.group2.exception.ScheduleConflictException;
import I2new.group2.model.ActivityStatus;
import I2new.group2.model.Enrollment;
import I2new.group2.model.Instrument;
import I2new.group2.model.Invoice;
import I2new.group2.model.Lesson;
import I2new.group2.model.LessonType;
import I2new.group2.model.Room;
import I2new.group2.model.RoomBooking;
import I2new.group2.model.ScheduledActivity;
import I2new.group2.model.Student;
import I2new.group2.model.Teacher;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Scheduling and booking (menu c). Implements UML/SeqDiagram.puml. */
public class SchedulingService {

    /** Result of scheduling a lesson: the saved lesson and what happened for each student. */
    public record SchedulingOutcome(Lesson lesson, List<String> notes) {
    }

    private final ActivityDao activityDao;
    private final EnrollmentDao enrollmentDao;
    private final InvoiceDao invoiceDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final RoomDao roomDao;
    private final InstrumentDao instrumentDao;

    public SchedulingService(ActivityDao activityDao, EnrollmentDao enrollmentDao, InvoiceDao invoiceDao,
                             StudentDao studentDao, TeacherDao teacherDao, RoomDao roomDao, InstrumentDao instrumentDao) {
        this.activityDao = activityDao;
        this.enrollmentDao = enrollmentDao;
        this.invoiceDao = invoiceDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
        this.roomDao = roomDao;
        this.instrumentDao = instrumentDao;
    }

    // ------------------------------------------------------------ schedule views

    /** Lessons, bookings and exams in one polymorphic list. */
    public List<ScheduledActivity> getSchedule() throws SQLException {
        return activityDao.findAll();
    }

    public List<ScheduledActivity> getUpcoming(int days) throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        return activityDao.findBetween(now.toLocalDate().atStartOfDay(), now.plusDays(days));
    }

    public List<ScheduledActivity> getStudentSchedule(int studentId) throws SQLException {
        return activityDao.findByStudent(studentId);
    }

    public List<ScheduledActivity> getTeacherSchedule(int teacherId) throws SQLException {
        return activityDao.findByTeacher(teacherId);
    }

    public List<Room> listRooms() throws SQLException {
        return roomDao.findAll();
    }

    public void addRoom(Room room) throws SQLException {
        roomDao.insert(room);
    }

    public List<Instrument> listInstruments() throws SQLException {
        return instrumentDao.findAll();
    }

    public void addInstrument(Instrument instrument) throws SQLException {
        instrumentDao.insert(instrument);
    }

    public void setInstrumentAvailable(int instrumentId, boolean available) throws SQLException {
        Instrument instrument = instrumentDao.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("No instrument #" + instrumentId));
        instrument.setAvailable(available);
        instrumentDao.updateAvailability(instrument);
    }

    // ------------------------------------------------------------ lessons

    /**
     * Sequence diagram "Schedule a Lesson":
     * specialization -> teacher / room availability -> conflicts -> package or invoice -> save.
     */
    public SchedulingOutcome scheduleLesson(int teacherId, int roomId, int instrumentId, List<Integer> studentIds,
                                            LessonType type, LocalDateTime start, double duration) throws SQLException {
        Teacher teacher = teacherDao.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("No teacher #" + teacherId));
        Room room = roomDao.findById(roomId).orElseThrow(() -> new IllegalArgumentException("No room #" + roomId));
        Instrument instrument = instrumentDao.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("No instrument #" + instrumentId));
        List<Integer> ids = new ArrayList<>(new LinkedHashSet<>(studentIds));
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("A lesson needs at least one student");
        }
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A lesson cannot be scheduled in the past");
        }

        // 1. specialization
        if (!teacher.teaches(instrument)) {
            throw new InvalidSpecializationException(teacher.getFullName() + " does not teach " + instrument.getName());
        }
        // 2. availability of the resources
        if (!teacher.isAvailable(start, duration)) {
            throw new ScheduleConflictException(teacher.getFullName() + " is not available at that time "
                    + "(availability: " + teacher.getAvailability() + ")");
        }
        if (!room.isAvailable(start, duration)) {
            throw new ScheduleConflictException(room.getName() + " is closed at that time (open "
                    + Room.OPENING + "-" + Room.CLOSING + ")");
        }
        if (!instrument.isAvailable()) {
            throw new ScheduleConflictException(instrument.getName() + " is out of service");
        }

        Lesson lesson = new Lesson(0, start, duration, ActivityStatus.SCHEDULED, type, teacher, room, instrument);
        for (int id : ids) {
            lesson.addStudent(studentDao.findById(id).orElseThrow(() -> new IllegalArgumentException("No student #" + id)));
        }

        // 3. conflicts with the existing schedule
        Optional<ScheduledActivity> conflict = activityDao.findConflict(start, duration, teacherId, roomId, ids);
        if (conflict.isPresent()) {
            throw new ScheduleConflictException("Conflicts with " + conflict.get());
        }

        // 4 + 5. pay with a package or invoice the lesson, then save - all or nothing
        List<String> notes = new ArrayList<>();
        LocalDate lessonDate = start.toLocalDate();
        DatabaseConnection.getInstance().inTransaction(c -> {
            activityDao.insert(lesson);
            for (Student student : lesson.getParticipants()) {
                double hours = lesson.calculateConsumedHours();
                Optional<Enrollment> enrollment = student.getValidPackage(lessonDate, hours);
                if (enrollment.isPresent()) {
                    Enrollment e = enrollment.get();
                    e.consumeHours(hours, lessonDate);
                    enrollmentDao.updateRemainingHours(e);
                    enrollmentDao.recordConsumption(e.getId(), lesson.getId(), hours);
                    notes.add(String.format("%s: %.1fh taken from '%s' (%s left)", student.getFullName(), hours,
                            e.getCoursePackage().getName(),
                            e.isUnlimited() ? "unlimited" : String.format("%.1fh", e.getRemainingHours())));
                } else {
                    Invoice invoice = new Invoice(0, student.getId(), LocalDate.now());
                    invoice.addItem(lesson);
                    invoiceDao.insert(invoice);
                    notes.add(String.format("%s: no valid package - billed %.2f EUR (invoice #%d)",
                            student.getFullName(), lesson.calculatePrice(), invoice.getId()));
                }
            }
            return null;
        });
        return new SchedulingOutcome(lesson, notes);
    }

    // ------------------------------------------------------------ room bookings

    public RoomBooking bookRoom(int roomId, LocalDateTime start, double duration) throws SQLException {
        Room room = roomDao.findById(roomId).orElseThrow(() -> new IllegalArgumentException("No room #" + roomId));
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("A room cannot be booked in the past");
        }
        RoomBooking booking = new RoomBooking(0, start, duration, ActivityStatus.SCHEDULED, room);
        booking.bookRoom();
        Optional<ScheduledActivity> conflict = activityDao.findConflict(start, duration, null, roomId, List.of());
        if (conflict.isPresent()) {
            throw new ScheduleConflictException("Conflicts with " + conflict.get());
        }
        activityDao.insert(booking);
        return booking;
    }

    // ------------------------------------------------------------ status changes

    /**
     * Cancels any activity. For a lesson (business rule 2): cancelled 24h or more before the start,
     * the hours go back to the packages and unpaid charges are removed. Less than 24h before, the hours
     * stay consumed.
     */
    public String cancelActivity(int activityId, LocalDateTime when) throws SQLException {
        ScheduledActivity activity = activityDao.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No activity #" + activityId));
        activity.cancel(when); // dynamic dispatch: Lesson.cancel records whether it is a late cancellation

        return DatabaseConnection.getInstance().inTransaction(c -> {
            activityDao.updateStatus(activity);
            if (!(activity instanceof Lesson lesson)) {
                return activity.getActivityType() + " #" + activityId + " cancelled.";
            }
            if (lesson.wasLateCancellation()) {
                return "Lesson #" + activityId + " cancelled less than " + Lesson.FREE_CANCELLATION_HOURS
                        + "h before it starts: the hours count as consumed.";
            }
            double refunded = 0;
            for (Map.Entry<Integer, Double> consumption : enrollmentDao.findConsumptionsForLesson(activityId).entrySet()) {
                Enrollment enrollment = enrollmentDao.findById(consumption.getKey()).orElseThrow();
                enrollment.refundHours(consumption.getValue());
                enrollmentDao.updateRemainingHours(enrollment);
                enrollmentDao.deleteConsumption(enrollment.getId(), activityId);
                refunded += consumption.getValue();
            }
            int invoices = invoiceDao.removeLessonCharges(activityId);
            return String.format("Lesson #%d cancelled in time: %.1fh refunded to packages, %d unpaid charge(s) removed.",
                    activityId, refunded, invoices);
        });
    }

    public void completeActivity(int activityId) throws SQLException {
        ScheduledActivity activity = activityDao.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("No activity #" + activityId));
        if (activity.getDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("An activity cannot be completed before it starts");
        }
        activity.complete();
        activityDao.updateStatus(activity);
    }
}
