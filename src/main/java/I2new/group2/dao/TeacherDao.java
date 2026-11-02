package I2new.group2.dao;

import I2new.group2.model.Instrument;
import I2new.group2.model.Teacher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeacherDao {

    private final InstrumentDao instrumentDao;

    public TeacherDao(InstrumentDao instrumentDao) {
        this.instrumentDao = instrumentDao;
    }

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Teacher> findAll() throws SQLException {
        List<Teacher> teachers = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM teacher ORDER BY last_name, first_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                teachers.add(mapRow(rs));
            }
        }
        for (Teacher teacher : teachers) {
            loadDetails(teacher);
        }
        return teachers;
    }

    public Optional<Teacher> findById(int id) throws SQLException {
        Teacher teacher;
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM teacher WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                teacher = mapRow(rs);
            }
        }
        loadDetails(teacher);
        return Optional.of(teacher);
    }

    /** Inserts the teacher with specializations, qualifications and availability (one transaction). */
    public void insert(Teacher teacher) throws SQLException {
        DatabaseConnection.getInstance().inTransaction(c -> {
            String sql = "INSERT INTO teacher (first_name, last_name, email, phone, hourly_rate) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, teacher.getFirstName());
                ps.setString(2, teacher.getLastName());
                ps.setString(3, teacher.getEmail());
                ps.setString(4, teacher.getPhone());
                ps.setDouble(5, teacher.getHourlyRate());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    teacher.setId(keys.getInt(1));
                }
            }
            for (Instrument instrument : teacher.getSpecializations()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO teacher_instrument (teacher_id, instrument_id) VALUES (?, ?)")) {
                    ps.setInt(1, teacher.getId());
                    ps.setInt(2, instrument.getId());
                    ps.executeUpdate();
                }
            }
            for (String qualification : teacher.getQualifications()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO teacher_qualification (teacher_id, qualification) VALUES (?, ?)")) {
                    ps.setInt(1, teacher.getId());
                    ps.setString(2, qualification);
                    ps.executeUpdate();
                }
            }
            for (Teacher.Availability slot : teacher.getAvailability()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO teacher_availability (teacher_id, day_of_week, start_time, end_time) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, teacher.getId());
                    ps.setString(2, slot.day().name());
                    ps.setObject(3, slot.start());
                    ps.setObject(4, slot.end());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    public void update(Teacher teacher) throws SQLException {
        String sql = "UPDATE teacher SET first_name = ?, last_name = ?, email = ?, phone = ?, hourly_rate = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setString(1, teacher.getFirstName());
            ps.setString(2, teacher.getLastName());
            ps.setString(3, teacher.getEmail());
            ps.setString(4, teacher.getPhone());
            ps.setDouble(5, teacher.getHourlyRate());
            ps.setInt(6, teacher.getId());
            ps.executeUpdate();
        }
    }

    /** What deleting a teacher also removes. */
    public record LinkedRecords(int lessons, int scheduledLessons, int invoicedLessons) {
    }

    public LinkedRecords countLinkedRecords(int id) throws SQLException {
        return new LinkedRecords(
                findLessonIds(id, false).size(),
                findLessonIds(id, true).size(),
                count("SELECT COUNT(DISTINCT ii.lesson_id) FROM invoice_item ii JOIN lesson l ON l.activity_id = ii.lesson_id "
                        + "WHERE l.teacher_id = ?", id));
    }

    /** Ids of the teacher's lessons; only the ones still SCHEDULED when {@code scheduledOnly}. */
    public List<Integer> findLessonIds(int teacherId, boolean scheduledOnly) throws SQLException {
        String sql = "SELECT a.id FROM scheduled_activity a JOIN lesson l ON l.activity_id = a.id WHERE l.teacher_id = ?"
                + (scheduledOnly ? " AND a.status = 'SCHEDULED'" : "");
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        return ids;
    }

    /**
     * Deletes the teacher's lessons, then the teacher (qualifications, availability and specializations cascade).
     * Hours consumed by those lessons and their invoice lines must be removed first
     * (see {@code PeopleService.deleteTeacher}).
     */
    public void delete(int id) throws SQLException {
        DatabaseConnection.getInstance().inTransaction(c -> {
            ActivityDao.execute(c, "DELETE a FROM scheduled_activity a JOIN lesson l ON l.activity_id = a.id "
                    + "WHERE l.teacher_id = ?", id);
            ActivityDao.execute(c, "DELETE FROM teacher WHERE id = ?", id);
            return null;
        });
    }

    private int count(String sql, int id) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Teacher mapRow(ResultSet rs) throws SQLException {
        return new Teacher(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"),
                rs.getString("email"), rs.getString("phone"), rs.getDouble("hourly_rate"));
    }

    private void loadDetails(Teacher teacher) throws SQLException {
        for (Instrument instrument : instrumentDao.findLinked("teacher_instrument", "teacher_id", teacher.getId())) {
            teacher.addSpecialization(instrument);
        }
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT qualification FROM teacher_qualification WHERE teacher_id = ? ORDER BY qualification")) {
            ps.setInt(1, teacher.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    teacher.addQualification(rs.getString(1));
                }
            }
        }
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT * FROM teacher_availability WHERE teacher_id = ? ORDER BY FIELD(day_of_week,"
                        + "'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'), start_time")) {
            ps.setInt(1, teacher.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    teacher.addAvailability(new Teacher.Availability(
                            DayOfWeek.valueOf(rs.getString("day_of_week")),
                            rs.getObject("start_time", LocalTime.class),
                            rs.getObject("end_time", LocalTime.class)));
                }
            }
        }
    }
}
