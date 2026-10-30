package I2new.group2.dao;

import I2new.group2.model.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomDao {

    private Connection connection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Room> findAll() throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM room ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Room> rooms = new ArrayList<>();
            while (rs.next()) {
                rooms.add(map(rs));
            }
            return rooms;
        }
    }

    public Optional<Room> findById(int id) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement("SELECT * FROM room WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void insert(Room room) throws SQLException {
        try (PreparedStatement ps = connection().prepareStatement(
                "INSERT INTO room (name, capacity) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, room.getName());
            ps.setInt(2, room.getCapacity());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                room.setId(keys.getInt(1));
            }
        }
    }

    private Room map(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("id"), rs.getString("name"), rs.getInt("capacity"));
    }
}
