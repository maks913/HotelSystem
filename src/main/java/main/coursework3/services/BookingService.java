package main.coursework3.services;

import main.coursework3.dao.BookingDAO;
import main.coursework3.dao.RoomDAO;
import main.coursework3.dao.SettlementDAO;
import main.coursework3.model.Bookings;
import main.coursework3.model.Rooms;
import main.coursework3.model.Settlements;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BookingService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final SettlementDAO settlementDAO = new SettlementDAO();
    private final SettlementService settlementService = new SettlementService();

    public List<main.coursework3.model.Clients> getAllClients() {
        return new main.coursework3.dao.ClientDAO().findAll();
    }

    public List<Rooms> findAvailableRoomsByDates(LocalDate arrival, LocalDate departure) {
        return roomDAO.findFreeRooms(arrival, departure, "Всі", 0);
    }

    public void insertBooking(Bookings booking) {
        bookingDAO.insertBooking(booking);
    }

    public void updateBooking(Bookings booking) {
        bookingDAO.updateBooking(booking);
    }

    public void updateRoomStatusById(int roomId, String status) {
        roomDAO.updateRoomStatusById(roomId, status);
    }

    public List<Bookings> getAllBookings() {
        return bookingDAO.findAll();
    }

    public String getClientName(int clientId) {
        return bookingDAO.getClientNameById(clientId);
    }

    public String getRoomNumber(int roomId) {
        return bookingDAO.getRoomNumberById(roomId);
    }

    public Rooms findRoomById(int roomId) {
        return roomDAO.findById(roomId);
    }

    public void updateStatus(int bookingId, String status) {
        bookingDAO.updateStatus(bookingId, status);
    }

    public void runAutoUpdate() {
        bookingDAO.updateBookingStatusesSimple();
        settlementService.runRoomAutoUpdate();
    }

    public double calculateTotalCost(LocalDate arrival, LocalDate departure, double costPerDay) {
        long days = ChronoUnit.DAYS.between(arrival, departure);
        if (days <= 0) days = 1;
        return days * costPerDay;
    }

    public boolean processCheckInTransaction(Bookings booking, double totalCost) {
        String initialPaymentStatus = (booking.getDepositAmount() > 0) ? "Частково" : "Не оплачено";

        Settlements settlement = new Settlements(
                0,
                booking.getIdClient(),
                booking.getIdRoom(),
                booking.getDateOfArrival(),
                booking.getDepartureDate(),
                totalCost,
                initialPaymentStatus
        );

        if (settlementDAO.insertSettlement(settlement)) {
            bookingDAO.updateStatus(booking.getIdBooking(), "Завершено");
            roomDAO.updateRoomStatusById(booking.getIdRoom(), RoomDAO.STATUS_OCCUPIED);
            settlementService.runRoomAutoUpdate();
            return true;
        }
        return false;
    }
}