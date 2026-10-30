package I2new.group2.dao;

import I2new.group2.model.CoursePackage;
import I2new.group2.model.Enrollment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enrollments and the Enrollment-Lesson "hours consumed" association (enrollment_lesson),
 * which is also the student's usage history.
 */
public class EnrollmentDao {

    /** One line of a student's usage history. */
    public record UsageRecord(LocalDateTime lessonDate, int lessonId, String instrument, String packageName,
                              double hours, String lessonStatus) {
    }

    private final ServiceDao serviceDao;

    public EnrollmentDao(ServiceDao serviceDao) {
        this.serviceDao = serviceDao;
    }

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Enrollment> findByStudent(int studentId) throws SQLException {
        List<Enrollment> enrollments = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT * FROM enrollment WHERE student_id = ? ORDER BY purchase_date, id")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    enrollments.add(map(rs));
                }
            }
        }
        return enrollments;
    }

    public Optional<Enrollment> findById(int id) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM enrollment WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void insert(Enrollment enrollment) throws SQLException {
        String sql = "INSERT INTO enrollment (student_id, package_id, purchase_date, remaining_hours) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, enrollment.getStudentId());
            ps.setInt(2, enrollment.getCoursePackage().getId());
            ps.setObject(3, enrollment.getPurchaseDate());
            ps.setDouble(4, enrollment.getRemainingHours());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                enrollment.setId(keys.getInt(1));
            }
        }
    }

    public void updateRemainingHours(Enrollment enrollment) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(
                "UPDATE enrollment SET remaining_hours = ? WHERE id = ?")) {
            ps.setDouble(1, enrollment.getRemainingHours());
            ps.setInt(2, enrollment.getId());
            ps.executeUpdate();
        }
    }

    /** Records that {@code hours} of the enrollment were used by a lesson. */
    public void recordConsumption(int enrollmentId, int lessonId, double hours) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(
                "INSERT INTO enrollment_lesson (enrollment_id, lesson_id, hours_consumed, consumed_at) VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, enrollmentId);
            ps.setInt(2, lessonId);
            ps.setDouble(3, hours);
            ps.setObject(4, LocalDateTime.now());
            ps.executeUpdate();
        }
    }

    public void deleteConsumption(int enrollmentId, int lessonId) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(
                "DELETE FROM enrollment_lesson WHERE enrollment_id = ? AND lesson_id = ?")) {
            ps.setInt(1, enrollmentId);
            ps.setInt(2, lessonId);
            ps.executeUpdate();
        }
    }

    /** enrollment id -> hours consumed, for every package that paid for the lesson. */
    public Map<Integer, Double> findConsumptionsForLesson(int lessonId) throws SQLException {
        Map<Integer, Double> consumptions = new LinkedHashMap<>();
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT enrollment_id, hours_consumed FROM enrollment_lesson WHERE lesson_id = ?")) {
            ps.setInt(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    consumptions.put(rs.getInt(1), rs.getDouble(2));
                }
            }
        }
        return consumptions;
    }

    public List<UsageRecord> findUsageHistory(int studentId) throws SQLException {
        String sql = """
                SELECT a.date_time, el.lesson_id, i.name AS instrument, s.name AS package_name,
                       el.hours_consumed, a.status
                FROM enrollment_lesson el
                JOIN enrollment e ON e.id = el.enrollment_id
                JOIN service s ON s.id = e.package_id
                JOIN scheduled_activity a ON a.id = el.lesson_id
                JOIN lesson l ON l.activity_id = el.lesson_id
                JOIN instrument i ON i.id = l.instrument_id
                WHERE e.student_id = ?
                ORDER BY a.date_time
                """;
        List<UsageRecord> history = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new UsageRecord(rs.getObject("date_time", LocalDateTime.class), rs.getInt("lesson_id"),
                            rs.getString("instrument"), rs.getString("package_name"),
                            rs.getDouble("hours_consumed"), rs.getString("status")));
                }
            }
        }
        return history;
    }

    private Enrollment map(ResultSet rs) throws SQLException {
        int packageId = rs.getInt("package_id");
        CoursePackage coursePackage = serviceDao.findPackageById(packageId)
                .orElseThrow(() -> new SQLException("Course package " + packageId + " not found"));
        return new Enrollment(rs.getInt("id"), rs.getInt("student_id"), coursePackage,
                rs.getObject("purchase_date", LocalDate.class), rs.getDouble("remaining_hours"));
    }
}
