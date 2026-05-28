package main.coursework3.dao;

import main.coursework3.io.DatabaseConnection;
import main.coursework3.model.Clients;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    private static final String INSERT_CLIENT = "INSERT INTO clients (pib, pasport_seria, pasport_number, phone_number, date_of_birth) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_CLIENT = "UPDATE clients SET pib = ?, pasport_seria = ?, pasport_number = ?, phone_number = ?, date_of_birth = ? WHERE id_client = ?";
    private static final String DELETE_CLIENT = "DELETE FROM clients WHERE id_client = ?";
    private static final String SELECT_ALL = "SELECT * FROM clients";
    private static final String SELECT_BY_ID = "SELECT * FROM clients WHERE id_client = ?";
    private static final String SELECT_BY_PIB = "SELECT * FROM clients WHERE LOWER(pib) LIKE ? OR phone_number LIKE ?";
    /** Додає нового клієнта до бази даних. */
    public void insertClient(Clients client) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_CLIENT)) {

            bindClientFields(stmt, client);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при додаванні клієнта", e);
        }
    }
    /** Оновлює дані існуючого клієнта. */
    public void updateClient(Clients client) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_CLIENT)) {

            bindClientFields(stmt, client);
            stmt.setInt(6, client.getIdClient());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при оновленні клієнта", e);
        }
    }
    /** Видаляє клієнта з бази даних за ідентифікатором. */
    public void deleteClient(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_CLIENT)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при видаленні клієнта", e);
        }
    }
    /** Отримує список усіх клієнтів готелю. */
    public List<Clients> findAll() {
        List<Clients> clients = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clients.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку клієнтів", e);
        }
        return clients;
    }
    /** Виконує пошук клієнтів за ПІБ або номером телефону. */
    public List<Clients> findBySearchFilter(String filterText) {
        List<Clients> result = new ArrayList<>();
        String queryParam = "%" + filterText.trim().toLowerCase() + "%";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_PIB)) {

            stmt.setString(1, queryParam);
            stmt.setString(2, queryParam);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при фільтрації списку клієнтів", e);
        }
        return result;
    }
    /** Повертає клієнта за його ідентифікатором. */
    public Clients findById(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при пошуку клієнта за ID", e);
        }
        return null;
    }
    /** Заповнює PreparedStatement даними клієнта. */
    private void bindClientFields(PreparedStatement stmt, Clients client) throws SQLException {
        stmt.setString(1, client.getPib());
        stmt.setString(2, client.getPasportSeria());
        stmt.setString(3, client.getPasportNumber());
        stmt.setString(4, client.getPhoneNumber());

        if (client.getDateOfBirth() != null) {
            stmt.setDate(5, client.getDateOfBirth());
        } else {
            stmt.setNull(5, Types.DATE);
        }
    }
    /** Перетворює рядок ResultSet у об'єкт клієнта. */
    private Clients mapRow(ResultSet rs) throws SQLException {
        Clients client = new Clients();
        client.setIdClient(rs.getInt("id_client"));
        client.setPib(rs.getString("pib"));
        client.setPasportSeria(rs.getString("pasport_seria"));
        client.setPasportNumber(rs.getString("pasport_number"));
        client.setPhoneNumber(rs.getString("phone_number"));
        client.setDateOfBirth(rs.getDate("date_of_birth"));
        return client;
    }
}