package main.coursework3.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import main.coursework3.dao.BookingDAO;
import main.coursework3.dao.RoomDAO;
import main.coursework3.io.Alerts;
import main.coursework3.services.SettlementService;

import java.io.IOException;

public class MainController {

    @FXML
    private Button btnRooms;
    @FXML
    private Button btnClients;
    @FXML
    private Button btnBookings;
    @FXML
    private Button btnChess;
    @FXML
    private Button btnLogout;
    @FXML
    private Button btnSettlements;
    @FXML
    private Button btnFreeNumber;
    @FXML
    private AnchorPane contentArea;

    private final BookingDAO bookingDAO = new BookingDAO();
    private final SettlementService settlementService = new SettlementService();
    private final Alerts alerts = new Alerts();

    /**
     * Ініціалізує головне меню та налаштовує навігацію між модулями.
     */
    @FXML
    public void initialize() {
        try {
            bookingDAO.updateBookingStatusesSimple();
            settlementService.runRoomAutoUpdate();
        } catch (Exception e) {
            alerts.showError("Помилка синхронізації", "Не вдалося автоматично оновити статуси номерів при запуску:\n" + e.getMessage());
        }

        btnRooms.setOnAction(event -> loadView("/main/coursework3/fxml/room_fund_view.fxml"));
        btnClients.setOnAction(event -> loadView("/main/coursework3/fxml/client_view.fxml"));
        btnBookings.setOnAction(event -> loadView("/main/coursework3/fxml/bookings_view.fxml"));
        btnSettlements.setOnAction(event -> loadView("/main/coursework3/fxml/settlement_view.fxml"));
        btnChess.setOnAction(event -> loadView("/main/coursework3/fxml/chess_view.fxml"));
        btnFreeNumber.setOnAction(event -> loadView("/main/coursework3/fxml/room_search_view.fxml"));

        btnLogout.setOnAction(event -> Platform.exit());

    }

    /**
     * Завантажує та відображає вибране представлення у центральній області вікна.
     */
    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            AnchorPane view = loader.load();

            contentArea.getChildren().setAll(view);

            AnchorPane.setTopAnchor(view, 0.0);
            AnchorPane.setBottomAnchor(view, 0.0);
            AnchorPane.setLeftAnchor(view, 0.0);
            AnchorPane.setRightAnchor(view, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
            alerts.showError("Помилка навігації",
                    String.format("Не вдалося завантажити екран інтерфейсу.\nШлях: %s\nПричина: %s", fxmlPath, e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Критична помилка", "Помилка при ініціалізації компонентів екрану:\n" + e.getMessage());
        }
    }
}