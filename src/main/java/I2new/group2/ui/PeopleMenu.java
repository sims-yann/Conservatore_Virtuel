package I2new.group2.ui;

import I2new.group2.model.Instrument;
import I2new.group2.model.Person;
import I2new.group2.model.Student;
import I2new.group2.model.StudentLevel;
import I2new.group2.model.Teacher;
import I2new.group2.service.EnrollmentService;
import I2new.group2.service.PeopleService;
import I2new.group2.service.SchedulingService;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** a. Manage students and teachers. */
public class PeopleMenu extends Menu {

    private final PeopleService people;
    private final SchedulingService scheduling;
    private final EnrollmentService enrollments;

    public PeopleMenu(ConsoleInput in, PeopleService people, SchedulingService scheduling, EnrollmentService enrollments) {
        super(in);
        this.people = people;
        this.scheduling = scheduling;
        this.enrollments = enrollments;
    }

    @Override
    public String getTitle() {
        return "Students and teachers";
    }

    @Override
    protected List<String> getOptions() {
        return List.of("List students", "Student details", "Add student", "Update student",
                "Delete student", "List teachers", "Teacher details", "Add teacher",
                "Update teacher hourly rate", "Delete teacher", "School directory (everyone)");
    }

    @Override
    protected void handle(int choice) throws SQLException {
        switch (choice) {
            case 1 -> people.listStudents().forEach(s -> System.out.println(s.describe()));
            case 2 -> studentDetails();
            case 3 -> addStudent();
            case 4 -> updateStudent();
            case 5 -> {
                people.deleteStudent(in.readInt("Student id: "));
                System.out.println("Student deleted.");
            }
            case 6 -> people.listTeachers().forEach(t -> System.out.println(t.describe()));
            case 7 -> teacherDetails();
            case 8 -> addTeacher();
            case 9 -> {
                Teacher teacher = people.getTeacher(in.readInt("Teacher id: "));
                teacher.setHourlyRate(in.readDouble("New hourly rate (EUR): "));
                people.updateTeacher(teacher);
                System.out.println("Updated: " + teacher.describe());
            }
            case 10 -> {
                people.deleteTeacher(in.readInt("Teacher id: "));
                System.out.println("Teacher deleted.");
            }
            case 11 -> {
                // polymorphic collection: describe() / getRole() resolved at run time
                for (Person person : people.getDirectory()) {
                    System.out.printf("%-8s %s%n", person.getRole(), person.describe());
                }
            }
            default -> { }
        }
    }

    private void studentDetails() throws SQLException {
        Student student = people.getStudent(in.readInt("Student id: "));
        System.out.println(student.describe());
        System.out.println("  Contact : " + student.getEmail() + " / " + student.getPhone());
        System.out.println("  Address : " + student.getAddress());
        System.out.println("  Born    : " + student.getBirthDate());
        System.out.println("Packages:");
        printAll(student.getEnrollments());
        System.out.println("Usage history:");
        printAll(enrollments.getUsageHistory(student.getId()).stream()
                .map(u -> String.format("%s  lesson #%d %-7s %-28s -%.1fh  (%s)", u.lessonDate().format(ConsoleInput.DATE_TIME),
                        u.lessonId(), u.instrument(), u.packageName(), u.hours(), u.lessonStatus()))
                .toList());
        System.out.println("Schedule:");
        printAll(scheduling.getStudentSchedule(student.getId()));
    }

    private void addStudent() throws SQLException {
        Student student = new Student(0, in.readText("First name: "), in.readText("Last name: "),
                in.readText("Email: "), in.readOptional("Phone: ", null), in.readOptional("Address: ", null),
                readBirthDate(), in.readEnum("Level:", StudentLevel.class));
        addInstruments(student);
        people.addStudent(student);
        System.out.println("Created " + student);
    }

    private LocalDate readBirthDate() {
        String value = in.readOptional("Birth date (yyyy-MM-dd, optional): ", "");
        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    private void addInstruments(Student student) throws SQLException {
        List<Instrument> instruments = scheduling.listInstruments();
        instruments.forEach(i -> System.out.println("  " + i.getId() + ". " + i.getName()));
        for (int id : in.readIdList("Preferred instrument ids (comma separated, optional): ")) {
            instruments.stream().filter(i -> i.getId() == id).findFirst().ifPresent(student::addPreferredInstrument);
        }
    }

    private void updateStudent() throws SQLException {
        Student student = people.getStudent(in.readInt("Student id: "));
        System.out.println("Leave a field empty to keep the current value.");
        student.setEmail(in.readOptional("Email [" + student.getEmail() + "]: ", student.getEmail()));
        student.setPhone(in.readOptional("Phone [" + student.getPhone() + "]: ", student.getPhone()));
        student.setAddress(in.readOptional("Address [" + student.getAddress() + "]: ", student.getAddress()));
        if (in.readYesNo("Change level (currently " + student.getLevel() + ")?")) {
            student.setLevel(in.readEnum("Level:", StudentLevel.class));
        }
        people.updateStudent(student);
        System.out.println("Updated: " + student.describe());
    }

    private void teacherDetails() throws SQLException {
        Teacher teacher = people.getTeacher(in.readInt("Teacher id: "));
        System.out.println(teacher.describe());
        System.out.println("  Contact        : " + teacher.getEmail() + " / " + teacher.getPhone());
        System.out.println("  Qualifications : " + teacher.getQualifications());
        System.out.println("  Availability   :");
        printAll(teacher.getAvailability());
        System.out.println("Lessons:");
        printAll(scheduling.getTeacherSchedule(teacher.getId()));
    }

    private void addTeacher() throws SQLException {
        Teacher teacher = new Teacher(0, in.readText("First name: "), in.readText("Last name: "),
                in.readText("Email: "), in.readOptional("Phone: ", null), in.readDouble("Hourly rate (EUR): "));
        List<Instrument> instruments = scheduling.listInstruments();
        instruments.forEach(i -> System.out.println("  " + i.getId() + ". " + i.getName()));
        for (int id : in.readIdList("Specialization instrument ids (comma separated): ")) {
            instruments.stream().filter(i -> i.getId() == id).findFirst().ifPresent(teacher::addSpecialization);
        }
        String qualification;
        while (!(qualification = in.readOptional("Qualification (empty to finish): ", "")).isEmpty()) {
            teacher.addQualification(qualification);
        }
        while (in.readYesNo("Add a weekly availability slot?")) {
            DayOfWeek day = in.readEnum("Day:", DayOfWeek.class);
            LocalTime start = in.readTime("From ");
            LocalTime end = in.readTime("To ");
            teacher.addAvailability(new Teacher.Availability(day, start, end));
        }
        people.addTeacher(teacher);
        System.out.println("Created " + teacher);
    }
}
