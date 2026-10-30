package I2new.group2.service;

import I2new.group2.dao.DatabaseConnection;
import I2new.group2.dao.EnrollmentDao;
import I2new.group2.dao.InvoiceDao;
import I2new.group2.dao.ServiceDao;
import I2new.group2.dao.StudentDao;
import I2new.group2.model.CoursePackage;
import I2new.group2.model.Enrollment;
import I2new.group2.model.Invoice;
import I2new.group2.model.Rental;
import I2new.group2.model.Service;
import I2new.group2.model.Student;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Course packages, rentals and purchases (menu b). */
public class EnrollmentService {

    private final ServiceDao serviceDao;
    private final EnrollmentDao enrollmentDao;
    private final StudentDao studentDao;
    private final InvoiceDao invoiceDao;

    public EnrollmentService(ServiceDao serviceDao, EnrollmentDao enrollmentDao, StudentDao studentDao, InvoiceDao invoiceDao) {
        this.serviceDao = serviceDao;
        this.enrollmentDao = enrollmentDao;
        this.studentDao = studentDao;
        this.invoiceDao = invoiceDao;
    }

    /** Every service offered, as a polymorphic list. */
    public List<Service> listServices() throws SQLException {
        return serviceDao.findAll();
    }

    public void addService(Service service) throws SQLException {
        serviceDao.insert(service);
    }

    /** A student buys a course package: an enrollment and an invoice are created together. */
    public Invoice purchasePackage(int studentId, int packageId) throws SQLException {
        Student student = findStudent(studentId);
        CoursePackage coursePackage = serviceDao.findPackageById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("No course package #" + packageId));
        LocalDate today = LocalDate.now();
        if (coursePackage.isExpired(today)) {
            throw new IllegalStateException("Package '" + coursePackage.getName() + "' expired on " + coursePackage.getEndDate());
        }
        Enrollment enrollment = new Enrollment(student.getId(), coursePackage, today);
        Invoice invoice = new Invoice(0, student.getId(), today);
        invoice.addItem(coursePackage);
        DatabaseConnection.getInstance().inTransaction(c -> {
            enrollmentDao.insert(enrollment);
            invoiceDao.insert(invoice);
            return null;
        });
        return invoice;
    }

    /** A student rents an instrument or a room: billed on a new invoice. */
    public Invoice rent(int studentId, int rentalId) throws SQLException {
        Student student = findStudent(studentId);
        Service service = serviceDao.findById(rentalId)
                .orElseThrow(() -> new IllegalArgumentException("No service #" + rentalId));
        if (!(service instanceof Rental rental)) {
            throw new IllegalArgumentException("Service #" + rentalId + " is not a rental");
        }
        if (rental.getInstrument() != null && !rental.getInstrument().isAvailable()) {
            throw new IllegalStateException(rental.getInstrument().getName() + " is out of service");
        }
        Invoice invoice = new Invoice(0, student.getId(), LocalDate.now());
        invoice.addItem(rental);
        invoiceDao.insert(invoice);
        return invoice;
    }

    public List<Enrollment> getEnrollments(int studentId) throws SQLException {
        return enrollmentDao.findByStudent(findStudent(studentId).getId());
    }

    public List<EnrollmentDao.UsageRecord> getUsageHistory(int studentId) throws SQLException {
        return enrollmentDao.findUsageHistory(findStudent(studentId).getId());
    }

    private Student findStudent(int studentId) throws SQLException {
        return studentDao.findById(studentId).orElseThrow(() -> new IllegalArgumentException("No student #" + studentId));
    }
}
