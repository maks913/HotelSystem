package main.coursework3.dao;

import main.coursework3.io.DatabaseConnection;
import main.coursework3.model.Appliances;
import main.coursework3.model.Rooms;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    private static final String FILTER_ALL = "Всі";
    public static final String STATUS_FREE = "Вільна";
    public static final String STATUS_BOOKED = "Заброньована";
    public static final String STATUS_OCCUPIED = "Зайнята";

    private static final String INSERT_ROOM = "INSERT INTO rooms (room_number, floor, room_class, cost_per_day, capacity, status) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_ROOM = "UPDATE rooms SET room_number=?, floor=?, room_class=?, cost_per_day=?, capacity=?, status=? WHERE id_room=?";
    private static final String DELETE_ROOM = "DELETE FROM rooms WHERE id_room = ?";
    private static final String SELECT_ALL = "SELECT * FROM rooms";
    private static final String SELECT_BY_ID = "SELECT * FROM rooms WHERE id_room = ?";
    private static final String UPDATE_STATUS_BY_ID = "UPDATE rooms SET status = ? WHERE id_room = ?";
    private static final String SELECT_ALL_APPLIANCES = "SELECT * FROM appliances";
    private static final String SELECT_APPLIANCES_FOR_ROOM = "SELECT o.id_appliance, o.name, o.technical_condition " +
            "FROM appliances o " +
            "JOIN room_appliances ra ON o.id_appliance = ra.id_appliance " +
            "WHERE ra.id_room = ?";
    private static final String INSERT_APPLIANCE_TO_ROOM = "INSERT INTO room_appliances (id_room, id_appliance) VALUES (?, ?)";
    private static final String DELETE_APPLIANCE_FROM_ROOM = "DELETE FROM room_appliances WHERE id_room = ? AND id_appliance = ?";
    private static final String SELECT_FREE_ROOMS = "SELECT * FROM rooms r " +
            "WHERE r.status != 'Ремонт' " +
            "AND (? = 'Всі' OR r.room_class = ?) " +
            "AND r.cost_per_day <= ? " +
            "AND r.id_room NOT IN ( " +
            "    SELECT b.id_room FROM bookings b " +
            "    WHERE b.status_armor IN ('Підтверджено', 'Очікує оплати', 'Активна') " +
            "    AND b.date_of_arrival < ? AND b.departure_date > ? " +
            ") " +
            "AND r.id_room NOT IN ( " +
            "    SELECT s.id_room FROM settlements s " +
            "    WHERE (s.fact_of_leaving IS NULL OR s.fact_of_leaving > ?) " +
            "    AND s.fact_of_arrival < ? " +
            ")";

    /** Додає нову кімнату до бази даних. */
    public void insertRoom(Rooms room) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_ROOM)) {
            bindRoomFields(stmt, room);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при додаванні кімнати", e);
        }
    }
    /** Оновлює параметри існуючої кімнати. */
    public void updateRoom(Rooms room) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_ROOM)) {
            bindRoomFields(stmt, room);
            stmt.setInt(7, room.getIdRoom());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні кімнати", e);
        }
    }
    /** Видаляє кімнату з бази даних за її ідентифікатором. */
    public void deleteRoom(int idRoom) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_ROOM)) {
            stmt.setInt(1, idRoom);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при видаленні кімнати", e);
        }
    }
    /** Отримує список усіх кімнат готелю. */
    public List<Rooms> findAll() {
        List<Rooms> rooms = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку кімнат", e);
        }
        return rooms;
    }
    /** Повертає кімнату за її ідентифікатором. */
    public Rooms findById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку кімнати за ID", e);
        }
        return null;
    }
    /** Виконує фільтрацію кімнат за класом та статусом. */
    public List<Rooms> findWithFilters(String roomClass, String status) {
        List<Rooms> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM rooms WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (roomClass != null && !roomClass.equals(FILTER_ALL)) {
            sql.append(" AND room_class = ?");
            params.add(roomClass);
        }
        if (status != null && !status.equals(FILTER_ALL)) {
            sql.append(" AND status = ?");
            params.add(status);
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при фільтрації кімнат", e);
        }
        return list;
    }
    /** Виконує пошук вільних номерів за заданими параметрами. */
    public List<Rooms> findFreeRooms(LocalDate checkIn, LocalDate checkOut, String roomClass, double maxPrice) {
        List<Rooms> list = new ArrayList<>();

        Date sqlCheckIn  = Date.valueOf(checkIn  != null ? checkIn  : LocalDate.now());
        Date sqlCheckOut = Date.valueOf(checkOut != null ? checkOut : LocalDate.now().plusDays(1));
        String cls       = (roomClass != null && !roomClass.trim().isEmpty()) ? roomClass : "Всі";
        double price     = maxPrice > 0 ? maxPrice : 999999.0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_FREE_ROOMS)) {

            stmt.setString(1, cls);
            stmt.setString(2, cls);
            stmt.setDouble(3, price);
            stmt.setDate(4, sqlCheckOut);
            stmt.setDate(5, sqlCheckIn);
            stmt.setDate(6, sqlCheckIn);
            stmt.setDate(7, sqlCheckOut);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Rooms room = mapRow(rs);

                    room.setStatus(RoomDAO.STATUS_FREE);

                    list.add(room);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при універсальному пошуку вільних номерів у БД", e);
        }
        return list;
    }
    /** Повертає список техніки, закріпленої за кімнатою. */
    public List<Appliances> getAllAppliance() {
        List<Appliances> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_APPLIANCES)) {
            while (rs.next()) list.add(mapAppliance(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні всієї техніки", e);
        }
        return list;
    }
    /** Повертає список техніки, закріпленої за кімнатою. */
    public List<Appliances> getAppliancesForRoom(int roomId) {
        List<Appliances> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_APPLIANCES_FOR_ROOM)) {
            stmt.setInt(1, roomId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapAppliance(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні техніки для кімнати", e);
        }
        return list;
    }
    /** Додає техніку до вибраної кімнати. */
    public void addApplianceToRoom(int roomId, int applianceId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_APPLIANCE_TO_ROOM)) {
            stmt.setInt(1, roomId);
            stmt.setInt(2, applianceId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при додаванні техніки до кімнати", e);
        }
    }
    /** Видаляє техніку з вибраної кімнати. */
    public void removeApplianceFromRoom(int roomId, int applianceId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_APPLIANCE_FROM_ROOM)) {
            stmt.setInt(1, roomId);
            stmt.setInt(2, applianceId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при видаленні техніки з кімнати", e);
        }
    }
    /** Оновлює статус кімнати за її ідентифікатором. */
    public void updateRoomStatusById(int roomId, String newStatus) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_STATUS_BY_ID)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка оновлення статусу кімнати", e);
        }
    }
    /** Заповнює PreparedStatement параметрами кімнати. */
    private void bindRoomFields(PreparedStatement stmt, Rooms room) throws SQLException {
        stmt.setString(1, room.getRoomNumber());
        stmt.setInt(2, room.getFloor());
        stmt.setString(3, room.getRoomClass());
        stmt.setDouble(4, room.getCostPerDay());
        stmt.setInt(5, room.getCapacity());
        stmt.setString(6, room.getStatus());
    }
    /** Перетворює рядок ResultSet у об'єкт кімнати. */
    private Rooms mapRow(ResultSet rs) throws SQLException {
        Rooms room = new Rooms();
        room.setIdRoom(rs.getInt("id_room"));
        room.setRoomNumber(rs.getString("room_number"));
        room.setFloor(rs.getInt("floor"));
        room.setRoomClass(rs.getString("room_class"));
        room.setCostPerDay(rs.getDouble("cost_per_day"));
        room.setCapacity(rs.getInt("capacity"));
        room.setStatus(rs.getString("status"));
        return room;
    }
    /** Перетворює рядок ResultSet у об'єкт техніки. */
    private Appliances mapAppliance(ResultSet rs) throws SQLException {
        return new Appliances(
                rs.getInt("id_appliance"),
                rs.getString("name"),
                rs.getString("technical_condition")
        );
    }
}