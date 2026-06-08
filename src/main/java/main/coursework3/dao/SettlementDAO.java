package main.coursework3.dao;

import main.coursework3.io.DatabaseConnection;
import main.coursework3.model.Settlements;

import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class SettlementDAO {

    private static final String SELECT_ALL = "SELECT * FROM settlements";
    private static final String INSERT_SETTLEMENT = "INSERT INTO settlements (id_client, id_room, fact_of_arrival, fact_of_leaving, total_cost, payment_status) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_CHECKOUT = "UPDATE settlements SET total_cost = ?, payment_status = ?, fact_of_leaving = CURDATE() WHERE id_settlement = ?";
    private static final String UPDATE_SETTLEMENT_FULL = "UPDATE settlements SET id_client = ?, id_room = ?, fact_of_arrival = ?, fact_of_leaving = ?, total_cost = ?, payment_status = ? WHERE id_settlement = ?";
    private static final String SELECT_CLIENT_NAME = "SELECT pib FROM clients WHERE id_client = ?";
    private static final String SELECT_ROOM_NUMBER = "SELECT room_number FROM rooms WHERE id_room = ?";
    private static final String SELECT_SETTLEMENTS_FINANCIAL = "SELECT " +
            " COUNT(*) AS total_settlements," +
            " SUM(IF(payment_status = 'Оплачено', total_cost, 0)) AS net_revenue," +
            " SUM(IF(payment_status = 'Не оплачено', total_cost, 0)) AS unpaid_debts " +
            "FROM settlements " +
            "WHERE MONTH(fact_of_arrival) = ? AND YEAR(fact_of_arrival) = ?; ";

    private static final String SELECT_SETTLEMENTS_NIGHTS = "SELECT fact_of_arrival, fact_of_leaving FROM settlements WHERE fact_of_arrival < ? AND fact_of_leaving > ?";

    private static final String SELECT_POPULAR_CLASS = "SELECT r.room_class, COUNT(s.id_room) AS rents " +
            "FROM settlements s JOIN rooms r ON s.id_room = r.id_room " +
            "WHERE s.fact_of_arrival < ? AND s.fact_of_leaving >= ? " +
            "GROUP BY r.room_class " +
            "ORDER BY rents DESC LIMIT 1";

    private static final String UPDATE_AUTO_PAYMENT_STATUS = "UPDATE settlements SET payment_status = ? WHERE id_settlement = ?";
    ;

    /**
     * Отримує список усіх заселень із бази даних.
     */
    public List<Settlements> findAll() {
        List<Settlements> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при отриманні списку заселень", e);
        }
        return list;
    }

    /**
     * Додає новий запис про заселення до бази даних.
     */
    public boolean insertSettlement(Settlements settlement) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SETTLEMENT)) {

            stmt.setInt(1, settlement.getIdClient());
            stmt.setInt(2, settlement.getIdRoom());
            stmt.setDate(3, settlement.getFactOfArrival());
            stmt.setDate(4, settlement.getFactOfLeaving());
            stmt.setDouble(5, settlement.getTotalCost());
            stmt.setString(6, settlement.getPaymentStatus());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при створенні заселення", e);
        }
    }

    /**
     * Оновлює статус оплати для вибраного заселення.
     */
    public void updatePaymentStatusById(int settlementId, String status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_AUTO_PAYMENT_STATUS)) {

            stmt.setString(1, status);
            stmt.setInt(2, settlementId);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Фіксує виселення гостя та оновлює фінальні дані оплати.
     */
    public boolean updateCheckoutDetails(int idSettlement, double finalCost, String paymentStatus) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_CHECKOUT)) {

            stmt.setDouble(1, finalCost);
            stmt.setString(2, paymentStatus);
            stmt.setInt(3, idSettlement);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при фіксації виселення та оплати в БД", e);
        }
    }

    /**
     * Повертає ПІБ клієнта за його ідентифікатором.
     */
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

    /**
     * Повертає номер кімнати за її ідентифікатором.
     */
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

    /**
     * Оновлює всі параметри вибраного заселення.
     */
    public boolean updateSettlementFull(Settlements settlement) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SETTLEMENT_FULL)) {

            stmt.setInt(1, settlement.getIdClient());
            stmt.setInt(2, settlement.getIdRoom());
            stmt.setDate(3, settlement.getFactOfArrival());
            stmt.setDate(4, settlement.getFactOfLeaving());
            stmt.setDouble(5, settlement.getTotalCost());
            stmt.setString(6, settlement.getPaymentStatus());
            stmt.setInt(7, settlement.getIdSettlement());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Помилка при повній зміні параметрів заселення", e);
        }
    }

    /**
     * Формує фінансову статистику заселень за обраний період.
     */
    public double[] getSettlementFinancials(int month, int year) {
        double[] result = new double[3];

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_SETTLEMENTS_FINANCIAL)) {

            stmt.setInt(1, month);
            stmt.setInt(2, year);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result[0] = rs.getDouble("total_settlements");
                    result[1] = rs.getDouble("net_revenue");
                    result[2] = rs.getDouble("unpaid_debts");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Помилка підрахунку фінансів поселень", e);
        }
        return result;
    }

    /**
     * Формує операційну статистику роботи готелю за місяць.
     */
    public Object[] getOperationalStats(int month, int year) {
        Object[] stats = new Object[]{0, 0, "Немає даних"};

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate startOfNextMonth = startOfMonth.plusMonths(1);

        Date sqlStart = Date.valueOf(startOfMonth);
        Date sqlNextMonth = Date.valueOf(startOfNextMonth);

        int uniqueGuests = 0;
        int totalNights = 0;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt1 = conn.prepareStatement(SELECT_SETTLEMENTS_NIGHTS)) {
                stmt1.setDate(1, sqlNextMonth);
                stmt1.setDate(2, sqlStart);

                try (ResultSet rs1 = stmt1.executeQuery()) {
                    while (rs1.next()) {
                        LocalDate arrival = rs1.getDate("fact_of_arrival").toLocalDate();
                        LocalDate leaving = rs1.getDate("fact_of_leaving").toLocalDate();
                        totalNights += calculateNights(
                                arrival,
                                leaving,
                                startOfMonth,
                                startOfNextMonth
                        );
                        uniqueGuests++;
                    }
                }
            }
            stats[0] = uniqueGuests;
            stats[1] = totalNights;
            try (PreparedStatement stmt2 = conn.prepareStatement(SELECT_POPULAR_CLASS)) {
                stmt2.setDate(1, sqlNextMonth);
                stmt2.setDate(2, sqlStart);

                try (ResultSet rs2 = stmt2.executeQuery()) {
                    if (rs2.next()) {
                        stats[2] = rs2.getString("room_class")
                                + " (" +
                                rs2.getInt("rents") +
                                " поселень)";
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Помилка формування операційного звіту", e);
        }
        return stats;
    }

    /**
     * Обчислює кількість ночей проживання в межах заданого періоду.
     */
    private int calculateNights(LocalDate arrival, LocalDate leaving, LocalDate periodStart, LocalDate periodEnd) {
        LocalDate start = arrival.isAfter(periodStart) ? arrival : periodStart;
        LocalDate end = leaving.isBefore(periodEnd) ? leaving : periodEnd;
        int nights = (int) ChronoUnit.DAYS.between(start, end);
        return Math.max(nights, 1);
    }

    /**
     * Перетворює рядок ResultSet у об'єкт заселення.
     */
    private Settlements mapRow(ResultSet rs) throws SQLException {
        return new Settlements(
                rs.getInt("id_settlement"),
                rs.getInt("id_client"),
                rs.getInt("id_room"),
                rs.getDate("fact_of_arrival"),
                rs.getDate("fact_of_leaving"),
                rs.getDouble("total_cost"),
                rs.getString("payment_status")
        );
    }
}