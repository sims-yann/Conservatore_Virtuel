package I2new.group2.dao;

import I2new.group2.model.Billable;
import I2new.group2.model.Invoice;
import I2new.group2.model.Lesson;
import I2new.group2.model.Payment;
import I2new.group2.model.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Invoices, their Billable items (services or lessons) and payments, plus the billing reports.
 */
public class InvoiceDao {

    /** Totals of one student, for the per-student summary. */
    public record StudentSummary(int studentId, String studentName, int invoices, double invoiced, double paid) {
        public double outstanding() {
            return invoiced - paid;
        }
    }

    /** Totals of one month, for the monthly summary. */
    public record MonthSummary(YearMonth month, int invoices, double invoiced, int payments, double paid) {
    }

    private final ServiceDao serviceDao;
    private final ActivityDao activityDao;

    public InvoiceDao(ServiceDao serviceDao, ActivityDao activityDao) {
        this.serviceDao = serviceDao;
        this.activityDao = activityDao;
    }

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ------------------------------------------------------------------ reads

    public List<Invoice> findAll() throws SQLException {
        return query("SELECT * FROM invoice ORDER BY date, id", null);
    }

    public List<Invoice> findByStudent(int studentId) throws SQLException {
        return query("SELECT * FROM invoice WHERE student_id = ? ORDER BY date, id", studentId);
    }

    public Optional<Invoice> findById(int id) throws SQLException {
        List<Invoice> found = query("SELECT * FROM invoice WHERE id = ?", id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    // ----------------------------------------------------------------- writes

    /** Inserts the invoice header and all its items. */
    public void insert(Invoice invoice) throws SQLException {
        DatabaseConnection.getInstance().inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO invoice (student_id, date, total_amount) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, invoice.getStudentId());
                ps.setObject(2, invoice.getDate());
                ps.setDouble(3, invoice.calculateTotal());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    invoice.setId(keys.getInt(1));
                }
            }
            for (Billable item : invoice.getItems()) {
                insertItem(c, invoice.getId(), item);
            }
            return null;
        });
    }

    public void insertPayment(int invoiceId, Payment payment) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(
                "INSERT INTO payment (invoice_id, amount, payment_date) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, invoiceId);
            ps.setDouble(2, payment.getAmount());
            ps.setObject(3, payment.getPaymentDate());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                payment.setId(keys.getInt(1));
            }
        }
    }

    /**
     * Removes a cancelled lesson from the unpaid invoices that charge for it.
     * An invoice left without items is deleted, otherwise its total is updated.
     * @return number of invoices changed
     */
    public int removeLessonCharges(int lessonId) throws SQLException {
        List<Integer> invoiceIds = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement("""
                SELECT DISTINCT ii.invoice_id FROM invoice_item ii
                WHERE ii.lesson_id = ? AND NOT EXISTS (SELECT 1 FROM payment p WHERE p.invoice_id = ii.invoice_id)
                """)) {
            ps.setInt(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    invoiceIds.add(rs.getInt(1));
                }
            }
        }
        for (int invoiceId : invoiceIds) {
            ActivityDao.execute(connection(), "DELETE FROM invoice_item WHERE invoice_id = ? AND lesson_id = ?",
                    invoiceId, lessonId);
            Invoice invoice = findById(invoiceId).orElseThrow();
            if (invoice.getItems().isEmpty()) {
                ActivityDao.execute(connection(), "DELETE FROM invoice WHERE id = ?", invoiceId);
            } else {
                ActivityDao.execute(connection(), "UPDATE invoice SET total_amount = ? WHERE id = ?",
                        invoice.calculateTotal(), invoiceId);
            }
        }
        return invoiceIds.size();
    }

    private void insertItem(Connection c, int invoiceId, Billable item) throws SQLException {
        switch (item) {
            case Service service -> ActivityDao.execute(c,
                    "INSERT INTO invoice_item (invoice_id, service_id) VALUES (?, ?)", invoiceId, service.getId());
            case Lesson lesson -> ActivityDao.execute(c,
                    "INSERT INTO invoice_item (invoice_id, lesson_id) VALUES (?, ?)", invoiceId, lesson.getId());
            default -> throw new IllegalArgumentException("Cannot invoice " + item.getClass().getSimpleName());
        }
    }

    // ---------------------------------------------------------------- reports

    /** Total actually paid by a student (spec: "total amount spent by a student"). */
    public double totalPaidByStudent(int studentId) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT COALESCE(SUM(p.amount), 0) FROM payment p JOIN invoice i ON i.id = p.invoice_id WHERE i.student_id = ?")) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        }
    }

    public List<StudentSummary> summaryPerStudent() throws SQLException {
        String sql = """
                SELECT s.id, CONCAT(s.first_name, ' ', s.last_name) AS name,
                       COUNT(i.id) AS invoices,
                       COALESCE(SUM(i.total_amount), 0) AS invoiced,
                       COALESCE(SUM(p.paid), 0) AS paid
                FROM student s
                LEFT JOIN invoice i ON i.student_id = s.id
                LEFT JOIN (SELECT invoice_id, SUM(amount) AS paid FROM payment GROUP BY invoice_id) p
                       ON p.invoice_id = i.id
                GROUP BY s.id, s.first_name, s.last_name
                ORDER BY s.last_name, s.first_name
                """;
        List<StudentSummary> summaries = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                summaries.add(new StudentSummary(rs.getInt("id"), rs.getString("name"), rs.getInt("invoices"),
                        rs.getDouble("invoiced"), rs.getDouble("paid")));
            }
        }
        return summaries;
    }

    public MonthSummary summaryForMonth(YearMonth month) throws SQLException {
        LocalDate from = month.atDay(1);
        LocalDate to = month.plusMonths(1).atDay(1);
        int invoices;
        double invoiced;
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT COUNT(*), COALESCE(SUM(total_amount), 0) FROM invoice WHERE date >= ? AND date < ?")) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                invoices = rs.getInt(1);
                invoiced = rs.getDouble(2);
            }
        }
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT COUNT(*), COALESCE(SUM(amount), 0) FROM payment WHERE payment_date >= ? AND payment_date < ?")) {
            ps.setObject(1, from);
            ps.setObject(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new MonthSummary(month, invoices, invoiced, rs.getInt(1), rs.getDouble(2));
            }
        }
    }

    // ---------------------------------------------------------------- mapping

    private List<Invoice> query(String sql, Integer param) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            if (param != null) {
                ps.setInt(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    invoices.add(new Invoice(rs.getInt("id"), rs.getInt("student_id"), rs.getObject("date", LocalDate.class)));
                }
            }
        }
        for (Invoice invoice : invoices) {
            loadItems(invoice);
            loadPayments(invoice);
        }
        return invoices;
    }

    private void loadItems(Invoice invoice) throws SQLException {
        List<int[]> rows = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT service_id, lesson_id FROM invoice_item WHERE invoice_id = ? ORDER BY id")) {
            ps.setInt(1, invoice.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int serviceId = rs.getInt(1);
                    boolean isService = !rs.wasNull();
                    rows.add(new int[]{isService ? serviceId : 0, isService ? 0 : rs.getInt(2)});
                }
            }
        }
        for (int[] row : rows) {
            if (row[0] != 0) {
                serviceDao.findById(row[0]).ifPresent(invoice::addItem);
            } else {
                activityDao.findLessonById(row[1]).ifPresent(invoice::addItem);
            }
        }
    }

    private void loadPayments(Invoice invoice) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(
                "SELECT * FROM payment WHERE invoice_id = ? ORDER BY payment_date, id")) {
            ps.setInt(1, invoice.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    invoice.addPayment(new Payment(rs.getInt("id"), rs.getDouble("amount"),
                            rs.getObject("payment_date", LocalDate.class)));
                }
            }
        }
    }
}
