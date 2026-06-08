package main.coursework3.services;

import main.coursework3.dao.BookingDAO;
import main.coursework3.dao.RoomDAO;
import main.coursework3.dao.SettlementDAO;
import main.coursework3.model.Bookings;
import main.coursework3.model.Rooms;
import main.coursework3.model.Settlements;

import java.time.LocalDate;
import java.util.List;

public class ChessService {

    private final RoomDAO roomDAO = new RoomDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final SettlementDAO settlementDAO = new SettlementDAO();

    /**
     * Повертає всі кімнати з бази даних.
     */
    public List<Rooms> getAllRooms() {
        return roomDAO.findAll();
    }

    /**
     * Повертає всі бронювання з бази даних.
     */
    public List<Bookings> getAllBookings() {
        return bookingDAO.findAll();
    }

    /**
     * Повертає всі заселення з бази даних.
     */
    public List<Settlements> getAllSettlements() {
        return settlementDAO.findAll();
    }

    /**
     * Визначає статус кімнати на конкретну дату на основі заселень і бронювань.
     */
    public String getRoomStatusOnDate(Rooms room, LocalDate date,
                                      List<Settlements> settlements,
                                      List<Bookings> bookings) {
        if ("Ремонт".equalsIgnoreCase(room.getStatus())) {
            return "Ремонт";
        }

        if (settlements != null) {
            for (Settlements s : settlements) {
                if (s.getIdRoom() == room.getIdRoom()) {
                    LocalDate arrival = s.getFactOfArrival().toLocalDate();
                    LocalDate leaving = (s.getFactOfLeaving() != null) ? s.getFactOfLeaving().toLocalDate() : null;

                    if (leaving != null && arrival.equals(leaving)) {
                        if (date.equals(arrival)) {
                            return "Зайнята";
                        }
                    }

                    if (leaving == null) {
                        leaving = LocalDate.MAX;
                    }
                    if (!date.isBefore(arrival) && date.isBefore(leaving)) {
                        return "Зайнята";
                    }
                }
            }
        }

        if (bookings != null) {
            for (Bookings b : bookings) {
                if (b.getIdRoom() == room.getIdRoom()) {
                    String statusArmor = b.getStatusArmor();

                    if ("Підтверджено".equalsIgnoreCase(statusArmor) ||
                            "Очікує оплати".equalsIgnoreCase(statusArmor) ||
                            "Активна".equalsIgnoreCase(statusArmor)) {

                        LocalDate arrival = b.getDateOfArrival().toLocalDate();
                        LocalDate departure = b.getDepartureDate().toLocalDate();

                        if (!date.isBefore(arrival) && date.isBefore(departure)) {
                            return "Заброньована";
                        }
                    }
                }
            }
        }

        return "Вільна";
    }

    /**
     * Повертає фінансові дані для звітів за вказаний місяць і рік.
     */
    public double[][] getFinancialReportData(int monthNum, int yearNum) {
        double[] sData = settlementDAO.getSettlementFinancials(monthNum, yearNum);
        double[] bData = bookingDAO.getBookingFinancials(monthNum, yearNum);
        return new double[][]{sData, bData};
    }

    /**
     * Повертає операційну статистику для звіту за вказаний місяць і рік.
     */
    public Object[] getOperationalReportData(int monthNum, int yearNum) {
        return settlementDAO.getOperationalStats(monthNum, yearNum);
    }
}