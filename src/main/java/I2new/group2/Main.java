package I2new.group2;

import I2new.group2.dao.ActivityDao;
import I2new.group2.dao.DatabaseConnection;
import I2new.group2.dao.DatabaseInitializer;
import I2new.group2.dao.EnrollmentDao;
import I2new.group2.dao.ExamDao;
import I2new.group2.dao.InstrumentDao;
import I2new.group2.dao.InvoiceDao;
import I2new.group2.dao.RoomDao;
import I2new.group2.dao.ServiceDao;
import I2new.group2.dao.StudentDao;
import I2new.group2.dao.TeacherDao;
import I2new.group2.service.BillingService;
import I2new.group2.service.EnrollmentService;
import I2new.group2.service.ExamService;
import I2new.group2.service.PeopleService;
import I2new.group2.service.ReportService;
import I2new.group2.service.SchedulingService;
import I2new.group2.ui.BillingMenu;
import I2new.group2.ui.ConsoleInput;
import I2new.group2.ui.ExamMenu;
import I2new.group2.ui.MainMenu;
import I2new.group2.ui.PeopleMenu;
import I2new.group2.ui.SchedulingMenu;
import I2new.group2.ui.ServicesMenu;

import java.sql.SQLException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            if (DatabaseInitializer.initializeIfNeeded()) {
                System.out.println("Database created and demo data loaded.");
            }
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Cannot open the database: " + e.getMessage());
            System.err.println("Check that MySQL is running and that src/main/resources/db.properties is correct.");
            return;
        }

        // DAOs
        InstrumentDao instrumentDao = new InstrumentDao();
        RoomDao roomDao = new RoomDao();
        TeacherDao teacherDao = new TeacherDao(instrumentDao);
        ServiceDao serviceDao = new ServiceDao(instrumentDao);
        EnrollmentDao enrollmentDao = new EnrollmentDao(serviceDao);
        StudentDao studentDao = new StudentDao(instrumentDao, enrollmentDao);
        ActivityDao activityDao = new ActivityDao(teacherDao, roomDao, instrumentDao, studentDao);
        ExamDao examDao = new ExamDao();
        InvoiceDao invoiceDao = new InvoiceDao(serviceDao, activityDao);

        // services (reusable by the future GUI)
        PeopleService people = new PeopleService(studentDao, teacherDao);
        EnrollmentService enrollments = new EnrollmentService(serviceDao, enrollmentDao, studentDao, invoiceDao);
        SchedulingService scheduling = new SchedulingService(activityDao, enrollmentDao, invoiceDao,
                studentDao, teacherDao, roomDao, instrumentDao);
        ExamService exams = new ExamService(activityDao, examDao, studentDao, instrumentDao);
        BillingService billing = new BillingService(invoiceDao);
        ReportService reports = new ReportService(invoiceDao);

        // console UI
        ConsoleInput in = new ConsoleInput();
        new MainMenu(in, List.of(
                new PeopleMenu(in, people, scheduling, enrollments),
                new ServicesMenu(in, enrollments, scheduling),
                new SchedulingMenu(in, scheduling, people),
                new BillingMenu(in, billing, reports, people),
                new ExamMenu(in, exams, scheduling)
        )).run();

        DatabaseConnection.getInstance().close();
        System.out.println("Goodbye.");
    }
}
