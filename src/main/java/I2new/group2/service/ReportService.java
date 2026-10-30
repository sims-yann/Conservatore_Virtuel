package I2new.group2.service;

import I2new.group2.dao.InvoiceDao;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Billing reports: total spent per student, summary per month or per student. */
public class ReportService {

    private final InvoiceDao invoiceDao;

    public ReportService(InvoiceDao invoiceDao) {
        this.invoiceDao = invoiceDao;
    }

    public double totalSpent(int studentId) throws SQLException {
        return invoiceDao.totalPaidByStudent(studentId);
    }

    public InvoiceDao.MonthSummary monthlySummary(YearMonth month) throws SQLException {
        return invoiceDao.summaryForMonth(month);
    }

    /** Summaries of the last {@code months} months, the current month last. */
    public List<InvoiceDao.MonthSummary> lastMonths(int months) throws SQLException {
        List<InvoiceDao.MonthSummary> summaries = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            summaries.add(invoiceDao.summaryForMonth(current.minusMonths(i)));
        }
        return summaries;
    }

    public List<InvoiceDao.StudentSummary> summaryPerStudent() throws SQLException {
        return invoiceDao.summaryPerStudent();
    }
}
