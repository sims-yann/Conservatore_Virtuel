package I2new.group2.ui;

import I2new.group2.dao.InvoiceDao;
import I2new.group2.model.Billable;
import I2new.group2.model.Invoice;
import I2new.group2.model.Student;
import I2new.group2.service.BillingService;
import I2new.group2.service.PeopleService;
import I2new.group2.service.ReportService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** d. Manage payments and billing. */
public class BillingMenu extends Menu {

    private final BillingService billing;
    private final ReportService reports;
    private final PeopleService people;

    public BillingMenu(ConsoleInput in, BillingService billing, ReportService reports, PeopleService people) {
        super(in);
        this.billing = billing;
        this.reports = reports;
        this.people = people;
    }

    @Override
    public String getTitle() {
        return "Payments and billing";
    }

    @Override
    protected List<String> getOptions() {
        return List.of("All invoices", "Invoices of a student", "Invoice details", "Record a payment",
                "Outstanding balances", "Total spent by a student", "Monthly summary",
                "Summary of the last 6 months", "Summary per student");
    }

    @Override
    protected void handle(int choice) throws SQLException {
        switch (choice) {
            case 1 -> printInvoices(billing.listInvoices());
            case 2 -> printInvoices(billing.invoicesOf(in.readInt("Student id: ")));
            case 3 -> printDetails(billing.getInvoice(in.readInt("Invoice id: ")));
            case 4 -> {
                printInvoices(billing.openInvoices());
                int invoiceId = in.readInt("Invoice id: ");
                double amount = in.readPositiveDouble("Amount (EUR): ");
                LocalDate date = in.readOptionalDate("Payment date ", LocalDate.now());
                Invoice invoice = billing.recordPayment(invoiceId, amount, date);
                System.out.println("Payment recorded. " + invoice);
            }
            case 5 -> {
                List<Invoice> open = billing.openInvoices();
                printInvoices(open);
                System.out.printf("Total outstanding: %.2f EUR%n",
                        open.stream().mapToDouble(Invoice::getOutstandingBalance).sum());
            }
            case 6 -> {
                Student student = people.getStudent(in.readInt("Student id: "));
                System.out.printf("%s has spent %.2f EUR in total.%n", student.getFullName(), reports.totalSpent(student.getId()));
            }
            case 7 -> {
                InvoiceDao.MonthSummary summary = reports.monthlySummary(in.readYearMonth("Month "));
                printMonthHeader();
                printMonth(summary);
            }
            case 8 -> {
                printMonthHeader();
                reports.lastMonths(6).forEach(this::printMonth);
            }
            case 9 -> {
                System.out.printf("  %-22s %8s %12s %12s %12s%n", "Student", "Invoices", "Invoiced", "Paid", "Outstanding");
                for (InvoiceDao.StudentSummary s : reports.summaryPerStudent()) {
                    System.out.printf("  %-22s %8d %12.2f %12.2f %12.2f%n", s.studentName(), s.invoices(),
                            s.invoiced(), s.paid(), s.outstanding());
                }
            }
            default -> { }
        }
    }

    private void printInvoices(List<Invoice> invoices) throws SQLException {
        if (invoices.isEmpty()) {
            System.out.println("  (none)");
        }
        for (Invoice invoice : invoices) {
            System.out.println("  " + invoice + "  - " + people.getStudent(invoice.getStudentId()).getFullName());
        }
    }

    private void printDetails(Invoice invoice) throws SQLException {
        System.out.println(invoice);
        System.out.println("Student: " + people.getStudent(invoice.getStudentId()).getFullName());
        System.out.println("Items:");
        // polymorphic collection of Billable: services and lessons price themselves
        for (Billable item : invoice.getItems()) {
            System.out.printf("  %-55s %8.2f EUR%n", item.getBillingDescription(), item.calculatePrice());
        }
        System.out.println("Payments:");
        printAll(invoice.getPayments());
        System.out.printf("Outstanding balance: %.2f EUR%n", invoice.getOutstandingBalance());
    }

    private void printMonthHeader() {
        System.out.printf("  %-8s %9s %12s %9s %12s%n", "Month", "Invoices", "Invoiced", "Payments", "Received");
    }

    private void printMonth(InvoiceDao.MonthSummary m) {
        System.out.printf("  %-8s %9d %12.2f %9d %12.2f%n", m.month(), m.invoices(), m.invoiced(), m.payments(), m.paid());
    }
}
