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
}