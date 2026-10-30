package I2new.group2.service;

import I2new.group2.dao.InvoiceDao;
import I2new.group2.model.Invoice;
import I2new.group2.model.Payment;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Invoices and payments (menu d). */
public class BillingService {

    private final InvoiceDao invoiceDao;

    public BillingService(InvoiceDao invoiceDao) {
        this.invoiceDao = invoiceDao;
    }

    public List<Invoice> listInvoices() throws SQLException {
        return invoiceDao.findAll();
    }

    public List<Invoice> invoicesOf(int studentId) throws SQLException {
        return invoiceDao.findByStudent(studentId);
    }

    public List<Invoice> openInvoices() throws SQLException {
        return invoiceDao.findAll().stream().filter(i -> !i.isPaid()).toList();
    }

    public Invoice getInvoice(int invoiceId) throws SQLException {
        return invoiceDao.findById(invoiceId).orElseThrow(() -> new IllegalArgumentException("No invoice #" + invoiceId));
    }

    /** Records a (partial) payment; paying more than the outstanding balance is refused. */
    public Invoice recordPayment(int invoiceId, double amount, LocalDate date) throws SQLException {
        Invoice invoice = getInvoice(invoiceId);
        Payment payment = new Payment(0, amount, date);
        invoice.addPayment(payment);
        invoiceDao.insertPayment(invoiceId, payment);
        return invoice;
    }
}
