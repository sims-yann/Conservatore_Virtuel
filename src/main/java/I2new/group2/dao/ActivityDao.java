package I2new.group2.dao;

import I2new.group2.model.ActivityStatus;
import I2new.group2.model.Exam;
import I2new.group2.model.ExamResult;
import I2new.group2.model.Instrument;
import I2new.group2.model.Lesson;
import I2new.group2.model.LessonType;
import I2new.group2.model.Room;
import I2new.group2.model.RoomBooking;
import I2new.group2.model.ScheduledActivity;
import I2new.group2.model.Schedulable;
import I2new.group2.model.Student;
import I2new.group2.model.Teacher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Persists the ScheduledActivity hierarchy (joined inheritance: scheduled_activity +
 * lesson / room_booking / exam). Reading returns a polymorphic list.
 */
public class ActivityDao {

    private static final String SELECT = """
            SELECT a.id, a.date_time, a.duration, a.status, a.activity_type,
                   l.type AS lesson_type, l.teacher_id, l.room_id AS lesson_room_id, l.instrument_id AS lesson_instrument_id,
                   rb.room_id AS booking_room_id,
                   e.name AS exam_name, e.capacity, e.instrument_id AS exam_instrument_id
            FROM scheduled_activity a
            LEFT JOIN lesson l ON l.activity_id = a.id
            LEFT JOIN room_booking rb ON rb.activity_id = a.id
            LEFT JOIN exam e ON e.activity_id = a.id
            """;

    private final TeacherDao teacherDao;
    private final RoomDao roomDao;
    private final InstrumentDao instrumentDao;
    private final StudentDao studentDao;

    public ActivityDao(TeacherDao teacherDao, RoomDao roomDao, InstrumentDao instrumentDao, StudentDao studentDao) {
        this.teacherDao = teacherDao;
        this.roomDao = roomDao;
        this.instrumentDao = instrumentDao;
        this.studentDao = studentDao;
    }

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ------------------------------------------------------------------ reads

    /** The whole schedule: lessons, room bookings and exams in one polymorphic list. */
    public List<ScheduledActivity> findAll() throws SQLException {
        return query(SELECT + " ORDER BY a.date_time", List.of());
    }

    public List<ScheduledActivity> findBetween(LocalDateTime from, LocalDateTime to) throws SQLException {
        return query(SELECT + " WHERE a.date_time >= ? AND a.date_time < ? ORDER BY a.date_time", List.of(from, to));
    }

    public Optional<ScheduledActivity> findById(int id) throws SQLException {
        List<ScheduledActivity> found = query(SELECT + " WHERE a.id = ?", List.of(id));
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    public Optional<Lesson> findLessonById(int id) throws SQLException {
        return findById(id).filter(Lesson.class::isInstance).map(Lesson.class::cast);
    }

    public Optional<Exam> findExamById(int id) throws SQLException {
        return findById(id).filter(Exam.class::isInstance).map(Exam.class::cast);
    }

    public List<Exam> findExams() throws SQLException {
        List<Exam> exams = new ArrayList<>();
        for (ScheduledActivity activity : query(SELECT + " WHERE a.activity_type = 'EXAM' ORDER BY a.date_time", List.of())) {
            exams.add((Exam) activity);
        }
        return exams;
    }

    public List<ScheduledActivity> findByStudent(int studentId) throws SQLException {
        String sql = SELECT + """
                 WHERE a.id IN (SELECT lesson_id FROM lesson_participant WHERE student_id = ?)
                    OR a.id IN (SELECT exam_id FROM exam_registration WHERE student_id = ?)
                 ORDER BY a.date_time
                """;
        return query(sql, List.of(studentId, studentId));
    }

    public List<ScheduledActivity> findByTeacher(int teacherId) throws SQLException {
        return query(SELECT + " WHERE l.teacher_id = ? ORDER BY a.date_time", List.of(teacherId));
    }

    /**
     * Sequence diagram "hasConflict(timeSlot, resources)": returns the first non cancelled activity
     * overlapping the slot that uses the same teacher, room, or one of the students.
     */
    public Optional<ScheduledActivity> findConflict(LocalDateTime start, double duration, Integer teacherId,
                                                    Integer roomId, Collection<Integer> studentIds) throws SQLException {
        LocalDateTime end = Schedulable.endOf(start, duration);
        List<Object> params = new ArrayList<>(List.of(end, start));
        StringBuilder resources = new StringBuilder("FALSE");
        if (teacherId != null) {
            resources.append(" OR l.teacher_id = ?");
            params.add(teacherId);
        }
        if (roomId != null) {
            resources.append(" OR l.room_id = ? OR rb.room_id = ?");
            params.add(roomId);
            params.add(roomId);
        }
        if (studentIds != null && !studentIds.isEmpty()) {
            String marks = studentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
            resources.append(" OR a.id IN (SELECT lesson_id FROM lesson_participant WHERE student_id IN (")
                    .append(marks).append("))");
            params.addAll(studentIds);
            resources.append(" OR a.id IN (SELECT exam_id FROM exam_registration WHERE student_id IN (")
                    .append(marks).append("))");
            params.addAll(studentIds);
        }
        String sql = SELECT + " WHERE a.status <> 'CANCELLED'"
                + " AND a.date_time < ? AND DATE_ADD(a.date_time, INTERVAL ROUND(a.duration * 60) MINUTE) > ?"
                + " AND (" + resources + ") ORDER BY a.date_time LIMIT 1";
        List<ScheduledActivity> found = query(sql, params);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    // ----------------------------------------------------------------- writes

    /** Inserts the base row then the row of the concrete subclass (call inside a transaction). */
    public void insert(ScheduledActivity activity) throws SQLException {
        DatabaseConnection.getInstance().inTransaction(c -> {
            String type = switch (activity) {
                case Lesson ignored -> "LESSON";
                case RoomBooking ignored -> "ROOM_BOOKING";
                case Exam ignored -> "EXAM";
                default -> throw new IllegalArgumentException("Unknown activity " + activity.getClass().getSimpleName());
            };
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO scheduled_activity (date_time, duration, status, activity_type) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setObject(1, activity.getDateTime());
                ps.setDouble(2, activity.getDuration());
                ps.setString(3, activity.getStatus().name());
                ps.setString(4, type);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    activity.setId(keys.getInt(1));
                }
            }
            switch (activity) {
                case Lesson lesson -> insertLesson(c, lesson);
                case RoomBooking booking -> execute(c, "INSERT INTO room_booking (activity_id, room_id) VALUES (?, ?)",
                        booking.getId(), booking.getRoom().getId());
                case Exam exam -> execute(c, "INSERT INTO exam (activity_id, name, capacity, instrument_id) VALUES (?, ?, ?, ?)",
                        exam.getId(), exam.getName(), exam.getCapacity(), exam.getInstrument().getId());
                default -> { }
            }
            return null;
        });
    }

    public void updateStatus(ScheduledActivity activity) throws SQLException {
        execute(connection(), "UPDATE scheduled_activity SET status = ? WHERE id = ?",
                activity.getStatus().name(), activity.getId());
    }

    private void insertLesson(Connection c, Lesson lesson) throws SQLException {
        execute(c, "INSERT INTO lesson (activity_id, type, teacher_id, room_id, instrument_id) VALUES (?, ?, ?, ?, ?)",
                lesson.getId(), lesson.getType().name(), lesson.getTeacher().getId(),
                lesson.getRoom().getId(), lesson.getInstrument().getId());
        for (Student student : lesson.getParticipants()) {
            execute(c, "INSERT INTO lesson_participant (lesson_id, student_id) VALUES (?, ?)",
                    lesson.getId(), student.getId());
        }
    }

    // ---------------------------------------------------------------- mapping

    private List<ScheduledActivity> query(String sql, List<?> params) throws SQLException {
        List<ScheduledActivity> activities = new ArrayList<>();
        // rows are read first, then related objects are loaded (the connection is shared)
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                            rs.getInt("id"), rs.getObject("date_time", LocalDateTime.class), rs.getDouble("duration"),
                            ActivityStatus.valueOf(rs.getString("status")), rs.getString("activity_type"),
                            rs.getString("lesson_type"), rs.getInt("teacher_id"), rs.getInt("lesson_room_id"),
                            rs.getInt("lesson_instrument_id"), rs.getInt("booking_room_id"),
                            rs.getString("exam_name"), rs.getInt("capacity"), rs.getInt("exam_instrument_id")});
                }
            }
        }
        for (Object[] row : rows) {
            activities.add(build(row));
        }
        return activities;
    }

    private ScheduledActivity build(Object[] row) throws SQLException {
        int id = (int) row[0];
        LocalDateTime dateTime = (LocalDateTime) row[1];
        double duration = (double) row[2];
        ActivityStatus status = (ActivityStatus) row[3];
        return switch ((String) row[4]) {
            case "LESSON" -> {
                Teacher teacher = teacherDao.findById((int) row[6]).orElseThrow(() -> missing("teacher", row[6]));
                Room room = roomDao.findById((int) row[7]).orElseThrow(() -> missing("room", row[7]));
                Instrument instrument = instrumentDao.findById((int) row[8]).orElseThrow(() -> missing("instrument", row[8]));
                Lesson lesson = new Lesson(id, dateTime, duration, status, LessonType.valueOf((String) row[5]),
                        teacher, room, instrument);
                for (int studentId : studentIds("SELECT student_id FROM lesson_participant WHERE lesson_id = ?", id)) {
                    studentDao.findById(studentId).ifPresent(lesson::addStudent);
                }
                yield lesson;
            }
            case "ROOM_BOOKING" -> new RoomBooking(id, dateTime, duration, status,
                    roomDao.findById((int) row[9]).orElseThrow(() -> missing("room", row[9])));
            case "EXAM" -> {
                Instrument instrument = instrumentDao.findById((int) row[12]).orElseThrow(() -> missing("instrument", row[12]));
                Exam exam = new Exam(id, dateTime, duration, ActivityStatus.SCHEDULED, (String) row[10], (int) row[11], instrument);
                for (int studentId : studentIds(
                        "SELECT student_id FROM exam_registration WHERE exam_id = ? ORDER BY registered_at", id)) {
                    studentDao.findById(studentId).ifPresent(exam::registerStudent);
                }
                loadResults(exam);
                if (status == ActivityStatus.COMPLETED) {
                    exam.complete();
                } else if (status == ActivityStatus.CANCELLED) {
                    exam.cancel();
                }
                yield exam;
            }
            default -> throw new SQLException("Unknown activity type " + row[4]);
        };
    }

    private void loadResults(Exam exam) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT student_id, passed, score FROM exam_result WHERE exam_id = ?")) {
            ps.setInt(1, exam.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double score = rs.getDouble("score");
                    rows.add(new Object[]{rs.getInt("student_id"), rs.getBoolean("passed"), rs.wasNull() ? null : score});
                }
            }
        }
        for (Object[] row : rows) {
            Student student = exam.getRegisteredStudents().stream()
                    .filter(s -> s.getId() == (int) row[0]).findFirst().orElse(null);
            if (student != null) {
                exam.addResult(new ExamResult(student, (boolean) row[1], (Double) row[2]));
            }
        }
    }

    private List<Integer> studentIds(String sql, int activityId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    static void execute(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    private static SQLException missing(String what, Object id) {
        return new SQLException("Missing " + what + " #" + id);
    }
}
