package main.coursework3.services;

import main.coursework3.dao.BookingDAO;
import main.coursework3.dao.ClientDAO;
import main.coursework3.dao.RoomDAO;
import main.coursework3.dao.SettlementDAO;
import main.coursework3.model.Bookings;
import main.coursework3.model.Clients;
import main.coursework3.model.Rooms;
import main.coursework3.model.Settlements;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettlementService {

    private final SettlementDAO settlementDAO = new SettlementDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final ClientDAO clientDAO = new ClientDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    public boolean insertSettlement(Settlements settlement) {
        return settlementDAO.insertSettlement(settlement);
    }

    public boolean updateSettlementFull(Settlements settlement) {
        return settlementDAO.updateSettlementFull(settlement);
    }
    public void runRoomAutoUpdate() {
        runFullHotelAutoUpdate();
    }

    public List<Clients> getAllClients() {
        return clientDAO.findAll();
    }

    public List<Rooms> findAvailableRoomsByDates(LocalDate arrival, LocalDate departure) {
        return roomDAO.findFreeRooms(arrival, departure, "Всі", 0);
    }

    public void updateRoomStatusById(int roomId, String status) {
        roomDAO.updateRoomStatusById(roomId, status);
    }

    public List<Settlements> getAllSettlements() {
        return settlementDAO.findAll();
    }

    public String getClientName(int clientId) {
        return settlementDAO.getClientNameById(clientId);
    }

    public String getRoomNumber(int roomId) {
        return settlementDAO.getRoomNumberById(roomId);
    }

    public Rooms findRoomById(int roomId) {
        return roomDAO.findById(roomId);
    }

    public int calculateNights(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 1;
        int nights = (int) ChronoUnit.DAYS.between(start, end);
        return Math.max(nights, 1);
    }

    public double findDepositForSettlement(Settlements settlement) {
        if (settlement == null) return 0.0;

        return bookingDAO.findAll().stream()
                .filter(b -> b.getIdClient() == settlement.getIdClient()
                        && b.getIdRoom() == settlement.getIdRoom()
                        && "Завершено".equalsIgnoreCase(b.getStatusArmor()))
                .mapToDouble(Bookings::getDepositAmount)
                .findFirst()
                .orElse(0.0);
    }

    public boolean processCheckOutTransaction(int settlementId, int roomId, double finalCost, String paymentStatus) {
        if (settlementDAO.updateCheckoutDetails(settlementId, finalCost, paymentStatus)) {
            roomDAO.updateRoomStatusById(roomId, RoomDAO.STATUS_FREE);
            runRoomAutoUpdate();
            return true;
        }
        return false;
    }

    public void runRoomAutoUpdateJava() {
        List<Rooms> allRooms = roomDAO.findAll();
        List<Settlements> allSettlements = settlementDAO.findAll();
        List<Bookings> allBookings = bookingDAO.findAll();
        LocalDate today = LocalDate.now();

        for (Rooms room : allRooms) {
            if ("Ремонт".equalsIgnoreCase(room.getStatus())) {
                continue;
            }

            String targetStatus = "Вільна";
            boolean isOccupied = false;

            for (Settlements s : allSettlements) {
                if (s.getIdRoom() != room.getIdRoom())
                    continue;

                LocalDate arrival = s.getFactOfArrival().toLocalDate();
                LocalDate departure = s.getFactOfLeaving().toLocalDate();

                if (!today.isBefore(arrival) &&
                        !today.isAfter(departure)) {

                    isOccupied = true;
                    break;
                }
            }

            if (isOccupied) {
                targetStatus = "Зайнята";
            } else {
                for (Bookings b : allBookings) {
                    if (b.getIdRoom() == room.getIdRoom()) {
                        String statusArmor = b.getStatusArmor();

                        if ("Підтверджено".equalsIgnoreCase(statusArmor) ||
                                "Очікує оплати".equalsIgnoreCase(statusArmor) ||
                                "Активна".equalsIgnoreCase(statusArmor)) {

                            LocalDate arrival = b.getDateOfArrival().toLocalDate();
                            LocalDate departure = b.getDepartureDate().toLocalDate();

                            if (!today.isBefore(arrival) && today.isBefore(departure)) {
                                targetStatus = "Заброньована";
                                break;
                            }
                        }
                    }
                }
            }
            if (!targetStatus.equalsIgnoreCase(room.getStatus())) {
                roomDAO.updateRoomStatusById(room.getIdRoom(), targetStatus);
                System.out.println("[Room Update] Кімната №" + room.getRoomNumber() +
                        " змінила статус на: " + targetStatus);
            }
        }
    }

    public void runSettlementAutoUpdateJava() {
        List<Settlements> allSettlements = settlementDAO.findAll();
        List<Bookings> allBookings = bookingDAO.findAll();

        for (Settlements s : allSettlements) {
            if ("Оплачено".equalsIgnoreCase(s.getPaymentStatus())) {
                continue;
            }

            double deposit = allBookings.stream()
                    .filter(b -> b.getIdClient() == s.getIdClient()
                            && b.getIdRoom() == s.getIdRoom()
                            && ("Завершено".equalsIgnoreCase(b.getStatusArmor())
                            || "Підтверджено".equalsIgnoreCase(b.getStatusArmor())
                            || "Активна".equalsIgnoreCase(b.getStatusArmor())))
                    .mapToDouble(Bookings::getDepositAmount)
                    .findFirst()
                    .orElse(0.0);

            double fullCost = s.getTotalCost();

            String targetStatus = "Не оплачено";
            if (deposit > 0.0 && deposit < fullCost) {
                targetStatus = "Частково";
            } else if (deposit >= fullCost && fullCost > 0.0) {
                targetStatus = "Оплачено";
            }

            if (!targetStatus.equalsIgnoreCase(s.getPaymentStatus())) {
                settlementDAO.updatePaymentStatusById(s.getIdSettlement(), targetStatus);
                s.setPaymentStatus(targetStatus);
                System.out.println("[AutoUpdate] Поселенню ID " + s.getIdSettlement() +
                        " виставлено статус: " + targetStatus + " (Депозит: " + deposit + ")");
            }
        }
    }

    public Map<String, Object> getRoomPerformanceStats(int month, int year) {

        List<Settlements> settlements = settlementDAO.findAll();
        List<Rooms> rooms = roomDAO.findAll();

        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();

        double totalRevenue = 0;
        int totalNights = 0;
        int totalSettlements = 0;

        Map<String, double[]> roomStats = new LinkedHashMap<>();
        Map<String, Double> classRevenue = new LinkedHashMap<>();

        classRevenue.put("Економ", 0.0);
        classRevenue.put("Стандарт", 0.0);
        classRevenue.put("Напів-Люкс", 0.0);
        classRevenue.put("Люкс", 0.0);

        for (Rooms room : rooms) {

            int soldNights = 0;
            double revenue = 0;

            for (Settlements s : settlements) {

                if (s.getIdRoom() != room.getIdRoom())
                    continue;

                LocalDate arrival = s.getFactOfArrival().toLocalDate();

                if (arrival.getMonthValue() != month ||
                        arrival.getYear() != year)
                    continue;

                LocalDate leaving = s.getFactOfLeaving().toLocalDate();

                int nights = Math.max(
                        calculateNights(arrival, leaving), 1
                );

                soldNights += nights;
                revenue += s.getTotalCost();

                totalNights += nights;
                totalRevenue += s.getTotalCost();
                totalSettlements++;
            }

            double occupancy =
                    (soldNights * 100.0) / daysInMonth;

            roomStats.put(
                    room.getRoomNumber() +
                            " (" + room.getRoomClass() + ")",
                    new double[]{occupancy, revenue}
            );

            classRevenue.put(
                    room.getRoomClass(),
                    classRevenue.getOrDefault(
                            room.getRoomClass(), 0.0
                    ) + revenue
            );
        }

        Map<String, Object> result = new HashMap<>();

        result.put("roomStats", roomStats);
        result.put("totalRevenue", totalRevenue);
        result.put("totalNights", totalNights);
        result.put("settlementsCount", totalSettlements);
        result.put("classRevenues", classRevenue);

        return result;
    }

    public void runFullHotelAutoUpdate() {
        runSettlementAutoUpdateJava();
        runRoomAutoUpdateJava();
    }
}