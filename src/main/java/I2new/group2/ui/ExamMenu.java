package I2new.group2.ui;

import I2new.group2.model.Exam;
import I2new.group2.model.ExamResult;
import I2new.group2.model.Student;
import I2new.group2.service.ExamService;
import I2new.group2.service.SchedulingService;

import java.sql.SQLException;
import java.util.List;

/** e. Manage exams and results. */
public class ExamMenu extends Menu {

    private final ExamService exams;
    private final SchedulingService scheduling;

    public ExamMenu(ConsoleInput in, ExamService exams, SchedulingService scheduling) {
        super(in);
        this.exams = exams;
        this.scheduling = scheduling;
    }

    @Override
    public String getTitle() {
        return "Exams and results";
    }

    @Override
    protected List<String> getOptions() {
        return List.of("List exams", "Exam details and results", "Create an exam",
                "Register a student", "Record a result");
    }

    @Override
    protected void handle(int choice) throws SQLException {
        switch (choice) {
            case 1 -> printAll(exams.listExams());
            case 2 -> details(exams.getExam(in.readInt("Exam id: ")));
            case 3 -> {
                scheduling.listInstruments().forEach(i -> System.out.println("  " + i.getId() + ". " + i.getName()));
                Exam exam = exams.createExam(in.readText("Name: "), in.readInt("Instrument id: "),
                        in.readDateTime("Date "), in.readPositiveDouble("Duration in hours: "),
                        in.readInt("Capacity: ", 1, 1000));
                System.out.println("Created " + exam);
            }
            case 4 -> register();
            case 5 -> recordResult();
            default -> { }
        }
    }

    /** UML/ExamRegistration.puml */
    private void register() throws SQLException {
        System.out.println("Scheduled exams:");
        printAll(exams.listExams().stream().filter(e -> e.getStatus().name().equals("SCHEDULED")).toList());
        Exam exam = exams.getExam(in.readInt("Exam id: "));
        details(exam);
        if (exam.isFull()) {
            System.out.println("Exam registration is closed");
            return;
        }
        List<Student> candidates = exams.candidatesFor(exam);
        System.out.println("Students learning " + exam.getInstrument().getName() + ":");
        printAll(candidates.stream().map(s -> "#" + s.getId() + " " + s.getFullName() + " (" + s.getLevel() + ")").toList());
        int studentId = in.readInt("Student id: ");
        exams.registerStudent(exam.getId(), studentId);
        System.out.println("Registration confirmed.");
    }

    private void recordResult() throws SQLException {
        Exam exam = exams.getExam(in.readInt("Exam id: "));
        printAll(exam.getRegisteredStudents().stream().map(s -> "#" + s.getId() + " " + s.getFullName()).toList());
        int studentId = in.readInt("Student id: ");
        boolean passed = in.readYesNo("Passed?");
        Double score = in.readOptionalDouble("Score /100 (optional): ");
        ExamResult result = exams.recordResult(exam.getId(), studentId, passed, score);
        System.out.println("Result recorded: " + result);
    }

    private void details(Exam exam) {
        System.out.println(exam);
        System.out.println("Remaining seats: " + exam.getRemainingSeats());
        System.out.println("Registered:");
        printAll(exam.getRegisteredStudents().stream()
                .map(s -> exam.getResultFor(s).map(ExamResult::toString).orElse(s.getFullName()))
                .toList());
    }
}
