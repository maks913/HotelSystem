package main.coursework3.services;

import main.coursework3.dao.RoomDAO;
import main.coursework3.model.Rooms;

import java.time.LocalDate;
import java.util.List;

public class RoomSearchService {
    private final RoomDAO roomDAO = new RoomDAO();

    public List<Rooms> searchFreeRooms(LocalDate dateFrom, LocalDate dateTo, String roomClass, String maxPriceText) {
        double maxPrice = 0;
        if (maxPriceText != null && !maxPriceText.isEmpty()) {
            maxPrice = Double.parseDouble(maxPriceText);
        }

        return roomDAO.findFreeRooms(dateFrom, dateTo, roomClass, maxPrice);
    }

    /**
     * Результат валідації дат пошуку. Виключає бізнес-перевірки з контролера.
     */
    public static class DateValidationResult {
        public final boolean valid;
        public final String errorTitle;
        public final String errorMessage;

        private DateValidationResult(boolean valid, String errorTitle, String errorMessage) {
            this.valid = valid;
            this.errorTitle = errorTitle;
            this.errorMessage = errorMessage;
        }

        public static DateValidationResult ok() {
            return new DateValidationResult(true, null, null);
        }

        public static DateValidationResult error(String title, String message) {
            return new DateValidationResult(false, title, message);
        }
    }

    /**
     * Валідує діапазон дат для пошуку вільних номерів.
     */
    public DateValidationResult validateSearchDates(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null || dateTo == null) {
            return DateValidationResult.error("Помилка вводу", "Будь ласка, вкажіть обидві дати!");
        }
        if (dateFrom.isAfter(dateTo)) {
            return DateValidationResult.error("Помилка дат", "Дата виїзду не може бути раніше дати заїзду!");
        }
        if (dateFrom.isBefore(LocalDate.now())) {
            return DateValidationResult.error("Помилка дат", "Неможливо знайти номери на минулі дати!");
        }
        return DateValidationResult.ok();
    }

    /**
     * Валідує дати для прямого поселення (Check-In).
     */
    public DateValidationResult validateCheckInDate(LocalDate dateFrom) {
        if (dateFrom != null && dateFrom.isAfter(LocalDate.now())) {
            return DateValidationResult.error("Помилка", "Пряме поселення можливе лише на сьогодні!");
        }
        return DateValidationResult.ok();
    }
}