package I2new.group2.service;

import I2new.group2.dao.DatabaseConnection;
import I2new.group2.dao.EnrollmentDao;
import I2new.group2.dao.InvoiceDao;
import I2new.group2.dao.StudentDao;
import I2new.group2.dao.TeacherDao;
import I2new.group2.model.Enrollment;
import I2new.group2.model.Person;
import I2new.group2.model.Student;
import I2new.group2.model.Teacher;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Students and teachers management (menu a). */
public class PeopleService {

    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final EnrollmentDao enrollmentDao;
    private final InvoiceDao invoiceDao;

    public PeopleService(StudentDao studentDao, TeacherDao teacherDao, EnrollmentDao enrollmentDao, InvoiceDao invoiceDao) {
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
        this.enrollmentDao = enrollmentDao;
        this.invoiceDao = invoiceDao;
    }

    public List<Student> listStudents() throws SQLException {
        return studentDao.findAll();
    }

    public Student getStudent(int id) throws SQLException {
        return studentDao.findById(id).orElseThrow(() -> new IllegalArgumentException("No student #" + id));
    }

    public void addStudent(Student student) throws SQLException {
        studentDao.insert(student);
    }

    public void updateStudent(Student student) throws SQLException {
        studentDao.update(student);
    }

    /** What {@link #deleteStudent} also removes, e.g. "1 forfait(s), 2 facture(s) (1 paiement(s)), 3 cours". */
    public String describeStudentRecords(int id) throws SQLException {
        getStudent(id);
        StudentDao.LinkedRecords r = studentDao.countLinkedRecords(id);
        List<String> parts = new ArrayList<>();
        addPart(parts, r.enrollments(), "forfait(s) et les heures consommées");
        if (r.invoices() > 0) {
            parts.add(r.invoices() + " facture(s)" + (r.payments() > 0 ? " (" + r.payments() + " paiement(s))" : ""));
        }
        addPart(parts, r.lessons(), "participation(s) à des cours");
        addPart(parts, r.examRegistrations(), "inscription(s) à des examens et leurs résultats");
        return parts.isEmpty() ? "aucune donnée liée" : String.join(", ", parts);
    }

    /** Deletes the student and their whole history. */
    public void deleteStudent(int id) throws SQLException {
        getStudent(id);
        studentDao.delete(id);
    }

    public List<Teacher> listTeachers() throws SQLException {
        return teacherDao.findAll();
    }

    public Teacher getTeacher(int id) throws SQLException {
        return teacherDao.findById(id).orElseThrow(() -> new IllegalArgumentException("No teacher #" + id));
    }

    public void addTeacher(Teacher teacher) throws SQLException {
        teacherDao.insert(teacher);
    }

    public void updateTeacher(Teacher teacher) throws SQLException {
        teacherDao.update(teacher);
    }

    /** What {@link #deleteTeacher} also removes. */
    public String describeTeacherRecords(int id) throws SQLException {
        getTeacher(id);
        TeacherDao.LinkedRecords r = teacherDao.countLinkedRecords(id);
        List<String> parts = new ArrayList<>();
        if (r.lessons() > 0) {
            parts.add(r.lessons() + " cours" + (r.scheduledLessons() > 0
                    ? " (dont " + r.scheduledLessons() + " à venir : heures rendues aux élèves)" : ""));
        }
        addPart(parts, r.invoicedLessons(), "ligne(s) de facture de cours");
        return parts.isEmpty() ? "aucune donnée liée" : String.join(", ", parts);
    }

    /**
     * Deletes the teacher with their lessons: hours of lessons not given yet go back to the
     * students' packages, the lessons are removed from invoices, then lessons and teacher are deleted.
     */
    public void deleteTeacher(int id) throws SQLException {
        getTeacher(id);
        DatabaseConnection.getInstance().inTransaction(c -> {
            for (int lessonId : teacherDao.findLessonIds(id, true)) {
                for (Map.Entry<Integer, Double> consumption : enrollmentDao.findConsumptionsForLesson(lessonId).entrySet()) {
                    Enrollment enrollment = enrollmentDao.findById(consumption.getKey()).orElseThrow();
                    enrollment.refundHours(consumption.getValue());
                    enrollmentDao.updateRemainingHours(enrollment);
                }
            }
            for (int lessonId : teacherDao.findLessonIds(id, false)) {
                enrollmentDao.deleteConsumptionsForLesson(lessonId);
                invoiceDao.removeAllLessonCharges(lessonId);
            }
            teacherDao.delete(id);
            return null;
        });
    }

    /** Everyone in the school as a polymorphic list of Person. */
    public List<Person> getDirectory() throws SQLException {
        List<Person> people = new ArrayList<>(teacherDao.findAll());
        people.addAll(studentDao.findAll());
        return people;
    }

    private static void addPart(List<String> parts, int count, String label) {
        if (count > 0) {
            parts.add(count + " " + label);
        }
    }
}
