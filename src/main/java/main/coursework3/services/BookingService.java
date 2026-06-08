package main.coursework3.services;

import main.coursework3.dao.BookingDAO;
import main.coursework3.dao.RoomDAO;
import main.coursework3.dao.SettlementDAO;
import main.coursework3.model.Bookings;
import main.coursework3.model.Rooms;
import main.coursework3.model.Settlements;

import java.sql.Date;
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

    public boolean isBookingClosed(Bookings booking) {
        String status = booking.getStatusArmor();
        return "Завершено".equalsIgnoreCase(status) || "Скасовано".equalsIgnoreCase(status);
    }

    public boolean isBookingCompleted(Bookings booking) {
        return "Завершено".equalsIgnoreCase(booking.getStatusArmor());
    }

    public void cancelBooking(int bookingId) {
        bookingDAO.updateStatus(bookingId, "Скасовано");
        runAutoUpdate();
    }

    public static class CheckInResult {
        public final boolean success;
        public final String errorMessage;
        public final double totalCost;

        private CheckInResult(boolean success, String errorMessage, double totalCost) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.totalCost = totalCost;
        }

        public static CheckInResult ok(double totalCost) {
            return new CheckInResult(true, null, totalCost);
        }

        public static CheckInResult error(String message) {
            return new CheckInResult(false, message, 0);
        }

        public String formatTotalCost() {
            return String.format("%.2f грн.", totalCost);
        }
    }

    public CheckInResult performCheckIn(Bookings booking) {
        Rooms room = findRoomById(booking.getIdRoom());
        if (room == null) {
            return CheckInResult.error("Кімнату не знайдено в базі даних.");
        }

        double totalCost = calculateTotalCost(
                booking.getDateOfArrival().toLocalDate(),
                booking.getDepartureDate().toLocalDate(),
                room.getCostPerDay()
        );

        if (!processCheckInTransaction(booking, totalCost)) {
            return CheckInResult.error("Не вдалося зберегти дані про поселення.");
        }

        return CheckInResult.ok(totalCost);
    }

    public boolean matchesFilter(Bookings booking, String searchText, boolean showCancelled) {
        String status = booking.getStatusArmor();
        if (!showCancelled && ("Скасовано".equalsIgnoreCase(status) || "Завершено".equalsIgnoreCase(status))) {
            return false;
        }
        if (!searchText.isEmpty()) {
            String clientName = getClientName(booking.getIdClient()).toLowerCase();
            if (!clientName.contains(searchText)) return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Логіка з BookingDetailsController
    // -------------------------------------------------------------------------

    /**
     * Результат валідації та збереження бронювання.
     */
    public static class BookingSaveResult {
        public final boolean success;
        public final String errorTitle;
        public final String errorMessage;

        private BookingSaveResult(boolean success, String errorTitle, String errorMessage) {
            this.success = success;
            this.errorTitle = errorTitle;
            this.errorMessage = errorMessage;
        }

        public static BookingSaveResult ok() {
            return new BookingSaveResult(true, null, null);
        }

        public static BookingSaveResult error(String title, String message) {
            return new BookingSaveResult(false, title, message);
        }
    }

    /**
     * Валідує та розбирає рядок депозиту.
     */
    public DepositParseResult parseDeposit(String depositText) {
        if (depositText == null || depositText.trim().isEmpty()) {
            return DepositParseResult.error("Помилка заповнення", "Будь ласка, заповніть усі поля форми!");
        }
        try {
            double amount = Double.parseDouble(depositText.trim().replace(",", "."));
            if (amount < 0) {
                return DepositParseResult.error("Некоректна сума", "Сума депозиту не може бути від'ємною!");
            }
            return DepositParseResult.ok(amount);
        } catch (NumberFormatException e) {
            return DepositParseResult.error("Помилка формату", "Некоректно введена сума авансового депозиту.");
        }
    }

    /**
     * Результат парсингу депозиту.
     */
    public static class DepositParseResult {
        public final boolean success;
        public final double amount;
        public final String errorTitle;
        public final String errorMessage;

        private DepositParseResult(boolean success, double amount, String errorTitle, String errorMessage) {
            this.success = success;
            this.amount = amount;
            this.errorTitle = errorTitle;
            this.errorMessage = errorMessage;
        }

        public static DepositParseResult ok(double amount) {
            return new DepositParseResult(true, amount, null, null);
        }

        public static DepositParseResult error(String title, String message) {
            return new DepositParseResult(false, 0, title, message);
        }
    }

    /**
     * Перевіряє, чи вибрана кімната доступна для бронювання.
     */
    public boolean isRoomAvailable(Rooms selectedRoom, LocalDate arrival, LocalDate departure,
                                   int currentBookingId, Rooms initialRoomRef) {
        List<Rooms> freeRooms = findAvailableRoomsByDates(arrival, departure);
        boolean isRoomFree = freeRooms.stream()
                .anyMatch(r -> r.getIdRoom() == selectedRoom.getIdRoom());

        if (currentBookingId > 0 && initialRoomRef != null) {
            boolean isSameRoom = (selectedRoom.getIdRoom() == initialRoomRef.getIdRoom());
            return isRoomFree || isSameRoom;
        }

        return isRoomFree;
    }

    /**
     * Визначає статус бронювання на основі суми депозиту.
     */
    public String resolveBookingStatus(double depositAmount) {
        return depositAmount > 0 ? "Підтверджено" : "Очікує оплати";
    }

    /**
     * Збирає об'єкт Bookings і зберігає його.
     */
    public BookingSaveResult saveBooking(int currentBookingId, int clientId, Rooms selectedRoom,
                                         LocalDate arrival, LocalDate departure, double depositAmount) {
        Bookings booking = new Bookings();
        booking.setIdClient(clientId);
        booking.setIdRoom(selectedRoom.getIdRoom());
        booking.setDateOfArrival(Date.valueOf(arrival));
        booking.setDepartureDate(Date.valueOf(departure));
        booking.setDepositAmount(depositAmount);
        booking.setStatusArmor(resolveBookingStatus(depositAmount));

        if (arrival.equals(LocalDate.now())) {
            updateRoomStatusById(selectedRoom.getIdRoom(), RoomDAO.STATUS_BOOKED);
        }

        if (currentBookingId == 0) {
            insertBooking(booking);
        } else {
            booking.setIdBooking(currentBookingId);
            updateBooking(booking);
        }

        runAutoUpdate();
        return BookingSaveResult.ok();
    }
}