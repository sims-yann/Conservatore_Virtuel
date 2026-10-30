package I2new.group2.dao;

import I2new.group2.model.Enrollment;
import I2new.group2.model.Instrument;
import I2new.group2.model.Student;
import I2new.group2.model.StudentLevel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentDao {

    private final InstrumentDao instrumentDao;
    private final EnrollmentDao enrollmentDao;

    public StudentDao(InstrumentDao instrumentDao, EnrollmentDao enrollmentDao) {
        this.instrumentDao = instrumentDao;
        this.enrollmentDao = enrollmentDao;
    }

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Student> findAll() throws SQLException {
        List<Student> students = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM student ORDER BY last_name, first_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                students.add(mapRow(rs));
            }
        }
        for (Student student : students) {
            loadDetails(student);
        }
        return students;
    }

    public Optional<Student> findById(int id) throws SQLException {
        Student student;
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM student WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                student = mapRow(rs);
            }
        }
        loadDetails(student);
        return Optional.of(student);
    }

    /** Students who listed {@code instrumentId} among their preferred instruments. */
    public List<Student> findByPreferredInstrument(int instrumentId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT student_id FROM student_instrument WHERE instrument_id = ? ORDER BY student_id")) {
            ps.setInt(1, instrumentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        }
        List<Student> students = new ArrayList<>();
        for (int id : ids) {
            findById(id).ifPresent(students::add);
        }
        return students;
    }

    public void insert(Student student) throws SQLException {
        DatabaseConnection.getInstance().inTransaction(c -> {
            String sql = "INSERT INTO student (first_name, last_name, email, phone, address, birth_date, level) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindColumns(ps, student);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    student.setId(keys.getInt(1));
                }
            }
            for (Instrument instrument : student.getPreferredInstruments()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO student_instrument (student_id, instrument_id) VALUES (?, ?)")) {
                    ps.setInt(1, student.getId());
                    ps.setInt(2, instrument.getId());
                    ps.executeUpdate();
                }
            }
            return null;
        });
    }

    public void update(Student student) throws SQLException {
        String sql = "UPDATE student SET first_name = ?, last_name = ?, email = ?, phone = ?, address = ?, "
                + "birth_date = ?, level = ? WHERE id = ?";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            bindColumns(ps, student);
            ps.setInt(8, student.getId());
            ps.executeUpdate();
        }
    }

    /** Fails (FK violation) if the student still has enrollments, invoices or lessons. */
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("DELETE FROM student WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void bindColumns(PreparedStatement ps, Student student) throws SQLException {
        ps.setString(1, student.getFirstName());
        ps.setString(2, student.getLastName());
        ps.setString(3, student.getEmail());
        ps.setString(4, student.getPhone());
        ps.setString(5, student.getAddress());
        ps.setObject(6, student.getBirthDate());
        ps.setString(7, student.getLevel().name());
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        return new Student(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"),
                rs.getString("email"), rs.getString("phone"), rs.getString("address"),
                rs.getObject("birth_date", LocalDate.class), StudentLevel.valueOf(rs.getString("level")));
    }

    private void loadDetails(Student student) throws SQLException {
        for (Instrument instrument : instrumentDao.findLinked("student_instrument", "student_id", student.getId())) {
            student.addPreferredInstrument(instrument);
        }
        for (Enrollment enrollment : enrollmentDao.findByStudent(student.getId())) {
            student.enroll(enrollment);
        }
    }
}
