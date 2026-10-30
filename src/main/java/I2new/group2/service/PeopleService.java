package I2new.group2.service;

import I2new.group2.dao.StudentDao;
import I2new.group2.dao.TeacherDao;
import I2new.group2.model.Person;
import I2new.group2.model.Student;
import I2new.group2.model.Teacher;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Students and teachers management (menu a). */
public class PeopleService {

    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public PeopleService(StudentDao studentDao, TeacherDao teacherDao) {
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
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

    public void deleteTeacher(int id) throws SQLException {
        getTeacher(id);
        teacherDao.delete(id);
    }

    /** Everyone in the school as a polymorphic list of Person. */
    public List<Person> getDirectory() throws SQLException {
        List<Person> people = new ArrayList<>(teacherDao.findAll());
        people.addAll(studentDao.findAll());
        return people;
    }
}
