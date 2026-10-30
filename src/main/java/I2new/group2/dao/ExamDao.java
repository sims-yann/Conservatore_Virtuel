package I2new.group2.dao;

import I2new.group2.model.ExamResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Student-Exam "registers for" association and the exam results.
 * Exams themselves are read and inserted through {@link ActivityDao}.
 */
public class ExamDao {

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public void insertRegistration(int examId, int studentId, LocalDateTime registeredAt) throws SQLException {
        ActivityDao.execute(connection(),
                "INSERT INTO exam_registration (exam_id, student_id, registered_at) VALUES (?, ?, ?)",
                examId, studentId, registeredAt);
    }

    /** Inserts the result, or updates it if one was already recorded. */
    public void saveResult(int examId, ExamResult result) throws SQLException {
        String sql = "INSERT INTO exam_result (exam_id, student_id, passed, score) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE passed = VALUES(passed), score = VALUES(score)";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.setInt(2, result.getStudent().getId());
            ps.setBoolean(3, result.isPassed());
            if (result.getScore() == null) {
                ps.setNull(4, Types.DECIMAL);
            } else {
                ps.setDouble(4, result.getScore());
            }
            ps.executeUpdate();
        }
    }
}
