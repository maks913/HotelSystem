package main.coursework3.dao;

import main.coursework3.io.DatabaseConnection;
import main.coursework3.model.Bookings;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    private static final String SELECT_ALL = "SELECT id_booking, id_client, id_room, date_of_arrival, departure_date, deposit_amount, status_armor FROM bookings";
    private static final String UPDATE_STATUS = "UPDATE bookings SET status_armor = ? WHERE id_booking = ?";
    private static final String INSERT_BOOKING = "INSERT INTO bookings(id_client, id_room, date_of_arrival, departure_date, deposit_amount, status_armor) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_BOOKING_FULL = "UPDATE bookings SET id_client = ?, id_room = ?, date_of_arrival = ?, departure_date = ?, deposit_amount = ?, status_armor = ? WHERE id_booking = ?";
    private static final String SELECT_CLIENT_NAME = "SELECT pib FROM clients WHERE id_client = ?";
    private static final String SELECT_ROOM_NUMBER = "SELECT room_number FROM rooms WHERE id_room = ?";
    private static final String UPDATE_BOOKINGS_AUTO =
            "UPDATE bookings SET status_armor = 'Завершено' WHERE departure_date <= CURDATE() AND status_armor IN ('Підтверджено', 'Очікує оплати', 'Активна')";

    private static final String SELECT_BOOKING_FINANCIAL =
            "SELECT COUNT(id_booking) AS total_bookings, " +
                    "SUM(deposit_amount) AS total_deposits, " +
                    "SUM(IF(status_armor = 'Очікує оплати', 1, 0)) AS awaiting_count " +
                    "FROM bookings WHERE MONTH(date_of_arrival) = ? AND YEAR(date_of_arrival) = ? " +
                    "AND status_armor IN ('Підтверджено', 'Очікує оплати', 'Активна')";

    /** Отримує список усіх бронювань із бази даних. */
    public List<Bookings> findAll() {
        List<Bookings> bookings = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                bookings.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при завантаженні бронювань", e);
        }
        return bookings;
    }

    /** Оновлює статус вибраного бронювання. */
    public void updateStatus(int bookingId, String newStatus) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_STATUS)) {

            stmt.setString(1, newStatus);
            stmt.setInt(2, bookingId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні статусу", e);
        }
    }

    /** Додає нове бронювання до бази даних. */
    public void insertBooking(Bookings booking) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_BOOKING)) {

            stmt.setInt(1, booking.getIdClient());
            stmt.setInt(2, booking.getIdRoom());
            stmt.setDate(3, booking.getDateOfArrival());
            stmt.setDate(4, booking.getDepartureDate());
            stmt.setDouble(5, booking.getDepositAmount());
            stmt.setString(6, booking.getStatusArmor());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при створенні бронювання", e);
        }
    }

    /** Оновлює параметри існуючого бронювання. */
    public void updateBooking(Bookings booking) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_BOOKING_FULL)) {

            stmt.setInt(1, booking.getIdClient());
            stmt.setInt(2, booking.getIdRoom());
            stmt.setDate(3, booking.getDateOfArrival());
            stmt.setDate(4, booking.getDepartureDate());
            stmt.setDouble(5, booking.getDepositAmount());
            stmt.setString(6, booking.getStatusArmor());
            stmt.setInt(7, booking.getIdBooking());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні параметрів бронювання", e);
        }
    }

    /** Повертає ПІБ клієнта за його ідентифікатором. */
    public String getClientNameById(int idClient) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_CLIENT_NAME)) {

            stmt.setInt(1, idClient);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("pib");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні імені клієнта", e);
        }
        return "Невідомий клієнт";
    }

    /** Повертає номер кімнати за її ідентифікатором. */
    public String getRoomNumberById(int idRoom) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ROOM_NUMBER)) {

            stmt.setInt(1, idRoom);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("room_number");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні номера кімнати", e);
        }
        return "Невідома кімната";
    }

    /** Автоматично закриває завершені за датою бронювання. */
    public void updateBookingStatusesSimple() {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_BOOKINGS_AUTO)) {

            int rows = stmt.executeUpdate();
            System.out.println("[AutoStatus] Бронювань закрито за календарем: " + rows);
        } catch (SQLException e) {
            throw new RuntimeException("Помилка автооновлення броней", e);
        }
    }

    /** Формує фінансову статистику бронювань за обраний період. */
    public double[] getBookingFinancials(int month, int year) {
        double[] result = new double[4];

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BOOKING_FINANCIAL)) {

            stmt.setInt(1, month);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result[0] = rs.getDouble("total_bookings");
                    result[1] = rs.getDouble("total_deposits");
                    result[2] = rs.getDouble("awaiting_count");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка підрахунку фінансів бронювань", e);
        }
        return result;
    }

    /** Перетворює рядок ResultSet у об'єкт бронювання. */
    private Bookings mapRow(ResultSet rs) throws SQLException {
        Bookings booking = new Bookings();
        booking.setIdBooking(rs.getInt("id_booking"));
        booking.setIdClient(rs.getInt("id_client"));
        booking.setIdRoom(rs.getInt("id_room"));
        booking.setDateOfArrival(rs.getDate("date_of_arrival"));
        booking.setDepartureDate(rs.getDate("departure_date"));
        booking.setDepositAmount(rs.getDouble("deposit_amount"));
        booking.setStatusArmor(rs.getString("status_armor"));
        return booking;
    }
}