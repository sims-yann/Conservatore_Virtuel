package I2new.group2.dao;

import I2new.group2.model.Instrument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstrumentDao {

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Instrument> findAll() throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM instrument ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<Instrument> instruments = new ArrayList<>();
            while (rs.next()) {
                instruments.add(map(rs));
            }
            return instruments;
        }
    }

    public Optional<Instrument> findById(int id) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM instrument WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    /** Instruments linked to {@code ownerId} through a join table (teacher_instrument, student_instrument). */
    List<Instrument> findLinked(String joinTable, String ownerColumn, int ownerId) throws SQLException {
        String sql = "SELECT i.* FROM instrument i JOIN " + joinTable + " j ON j.instrument_id = i.id WHERE j."
                + ownerColumn + " = ? ORDER BY i.name";
        try (PreparedStatement ps = connection().prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Instrument> instruments = new ArrayList<>();
                while (rs.next()) {
                    instruments.add(map(rs));
                }
                return instruments;
            }
        }
    }

    public void insert(Instrument instrument) throws SQLException {
        String sql = "INSERT INTO instrument (name, type, available) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, instrument.getName());
            ps.setString(2, instrument.getType());
            ps.setBoolean(3, instrument.isAvailable());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                instrument.setId(keys.getInt(1));
            }
        }
    }

    public void updateAvailability(Instrument instrument) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("UPDATE instrument SET available = ? WHERE id = ?")) {
            ps.setBoolean(1, instrument.isAvailable());
            ps.setInt(2, instrument.getId());
            ps.executeUpdate();
        }
    }

    private Instrument map(ResultSet rs) throws SQLException {
        return new Instrument(rs.getInt("id"), rs.getString("name"), rs.getString("type"), rs.getBoolean("available"));
    }
}
