package I2new.group2.dao;

import I2new.group2.model.CoursePackage;
import I2new.group2.model.Instrument;
import I2new.group2.model.PackageType;
import I2new.group2.model.Rental;
import I2new.group2.model.RentalType;
import I2new.group2.model.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Persists the Service hierarchy (joined inheritance: service + course_package / rental).
 * Reading returns the right concrete subclass for each row.
 */
public class ServiceDao {

    private static final String SELECT = """
            SELECT s.id, s.name, s.price, s.service_type,
                   cp.total_hours, cp.start_date, cp.end_date, cp.type,
                   r.rental_type, r.instrument_id
            FROM service s
            LEFT JOIN course_package cp ON cp.service_id = s.id
            LEFT JOIN rental r ON r.service_id = s.id
            """;

    private final InstrumentDao instrumentDao;

    public ServiceDao(InstrumentDao instrumentDao) {
        this.instrumentDao = instrumentDao;
    }

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /** Every service, as a polymorphic list (CoursePackage and Rental objects). */
    public List<Service> findAll() throws SQLException {
        return query(SELECT + " ORDER BY s.service_type, s.id", null);
    }

    public List<CoursePackage> findPackages() throws SQLException {
        List<CoursePackage> packages = new ArrayList<>();
        for (Service service : query(SELECT + " WHERE s.service_type = 'COURSE_PACKAGE' ORDER BY s.id", null)) {
            packages.add((CoursePackage) service);
        }
        return packages;
    }

    public Optional<Service> findById(int id) throws SQLException {
        List<Service> found = query(SELECT + " WHERE s.id = ?", id);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.getFirst());
    }

    public Optional<CoursePackage> findPackageById(int id) throws SQLException {
        return findById(id).filter(CoursePackage.class::isInstance).map(CoursePackage.class::cast);
    }

    /** Inserts the base row then the subclass row (one transaction). */
    public void insert(Service service) throws SQLException {
        DatabaseConnection.getInstance().inTransaction(c -> {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO service (name, price, service_type) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, service.getName());
                ps.setDouble(2, service.getPrice());
                ps.setString(3, service instanceof CoursePackage ? "COURSE_PACKAGE" : "RENTAL");
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    service.setId(keys.getInt(1));
                }
            }
            switch (service) {
                case CoursePackage p -> {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO course_package (service_id, total_hours, start_date, end_date, type) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setInt(1, p.getId());
                        ps.setInt(2, p.getTotalHours());
                        ps.setObject(3, p.getStartDate());
                        ps.setObject(4, p.getEndDate());
                        ps.setString(5, p.getType().name());
                        ps.executeUpdate();
                    }
                }
                case Rental r -> {
                    try (PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO rental (service_id, rental_type, instrument_id) VALUES (?, ?, ?)")) {
                        ps.setInt(1, r.getId());
                        ps.setString(2, r.getRentalType().name());
                        if (r.getInstrument() == null) {
                            ps.setNull(3, Types.INTEGER);
                        } else {
                            ps.setInt(3, r.getInstrument().getId());
                        }
                        ps.executeUpdate();
                    }
                }
                default -> throw new IllegalArgumentException("Unknown service type: " + service.getClass().getSimpleName());
            }
            return null;
        });
    }

    private List<Service> query(String sql, Integer id) throws SQLException {
        List<Service> services = new ArrayList<>();
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            if (id != null) {
                ps.setInt(1, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    services.add(map(rs));
                }
            }
        }
        return services;
    }

    private Service map(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        double price = rs.getDouble("price");
        if ("COURSE_PACKAGE".equals(rs.getString("service_type"))) {
            return new CoursePackage(id, name, price, rs.getInt("total_hours"),
                    rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
                    PackageType.valueOf(rs.getString("type")));
        }
        int instrumentId = rs.getInt("instrument_id");
        Instrument instrument = rs.wasNull() ? null : instrumentDao.findById(instrumentId).orElse(null);
        return new Rental(id, name, price, RentalType.valueOf(rs.getString("rental_type")), instrument);
    }
}
