package I2new.group2.service;

import I2new.group2.dao.ActivityDao;
import I2new.group2.dao.ExamDao;
import I2new.group2.dao.InstrumentDao;
import I2new.group2.dao.StudentDao;
import I2new.group2.model.ActivityStatus;
import I2new.group2.model.Exam;
import I2new.group2.model.ExamResult;
import I2new.group2.model.Instrument;
import I2new.group2.model.Student;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** Exams and results (menu e). Registration follows UML/ExamRegistration.puml. */
public class ExamService {

    private final ActivityDao activityDao;
    private final ExamDao examDao;
    private final StudentDao studentDao;
    private final InstrumentDao instrumentDao;

    public ExamService(ActivityDao activityDao, ExamDao examDao, StudentDao studentDao, InstrumentDao instrumentDao) {
        this.activityDao = activityDao;
        this.examDao = examDao;
        this.studentDao = studentDao;
        this.instrumentDao = instrumentDao;
    }

    public List<Exam> listExams() throws SQLException {
        return activityDao.findExams();
    }

    public Exam getExam(int examId) throws SQLException {
        return activityDao.findExamById(examId).orElseThrow(() -> new IllegalArgumentException("No exam #" + examId));
    }

    public Exam createExam(String name, int instrumentId, LocalDateTime start, double duration, int capacity)
            throws SQLException {
        Instrument instrument = instrumentDao.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("No instrument #" + instrumentId));
        if (start.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("An exam cannot be scheduled in the past");
        }
        Exam exam = new Exam(0, start, duration, ActivityStatus.SCHEDULED, name, capacity, instrument);
        activityDao.insert(exam);
        return exam;
    }

    /** Activity diagram: "Choose among list of students for instrument" - those not registered yet. */
    public List<Student> candidatesFor(Exam exam) throws SQLException {
        return studentDao.findByPreferredInstrument(exam.getInstrument().getId()).stream()
                .filter(s -> !exam.isRegistered(s))
                .toList();
    }

    /**
     * Activity diagram: full capacity -> "Exam registration is closed";
     * already registered -> "Student is already registered"; otherwise save the registration.
     */
    public void registerStudent(int examId, int studentId) throws SQLException {
        Exam exam = getExam(examId);
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("No student #" + studentId));
        exam.registerStudent(student);
        examDao.insertRegistration(examId, studentId, LocalDateTime.now());
    }

    /** Results are recorded once the exam has taken place: pass/fail and an optional score. */
    public ExamResult recordResult(int examId, int studentId, boolean passed, Double score) throws SQLException {
        Exam exam = getExam(examId);
        if (exam.getStatus() == ActivityStatus.CANCELLED) {
            throw new IllegalStateException("The exam was cancelled");
        }
        if (exam.getDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Results can be recorded once the exam has taken place ("
                    + exam.getDateTime().toLocalDate() + ")");
        }
        Student student = exam.getRegisteredStudents().stream()
                .filter(s -> s.getId() == studentId).findFirst()
                .orElseThrow(() -> new IllegalStateException("Student #" + studentId + " is not registered for this exam"));
        ExamResult result = new ExamResult(student, passed, score);
        exam.addResult(result);
        examDao.saveResult(examId, result);
        return result;
    }
}
