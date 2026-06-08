package main.coursework3.controllers;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import main.coursework3.controllers.modals.BookingDetailsController;
import main.coursework3.io.Alerts;
import main.coursework3.model.Bookings;
import main.coursework3.model.Rooms;
import main.coursework3.services.BookingService;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.util.ResourceBundle;

public class BookingController implements Initializable {
    @FXML
    private CheckBox checkShowCancelled;
    @FXML
    private Button btnEditBooking;
    @FXML
    private Button btnConfirmBooking;
    @FXML
    private Button btnCancelBooking;

    @FXML
    private TableView<Bookings> bookingsTable;
    @FXML
    private TableColumn<Bookings, Integer> colBookingId;
    @FXML
    private TableColumn<Bookings, String> colClientName;
    @FXML
    private TableColumn<Bookings, String> colRoomNumber;
    @FXML
    private TableColumn<Bookings, Date> colArrivalDate;
    @FXML
    private TableColumn<Bookings, Date> colDepartureDate;
    @FXML
    private TableColumn<Bookings, Double> colDepositAmount;
    @FXML
    private TableColumn<Bookings, String> colStatusArmor;
    @FXML
    private TextField txtSearchClient;

    private final BookingService bookingService = new BookingService();
    private final Alerts alerts = new Alerts();

    private final ObservableList<Bookings> masterData = FXCollections.observableArrayList();
    private FilteredList<Bookings> filteredData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            bookingService.runAutoUpdate();
        } catch (Exception e) {
            System.err.println("Авто-оновлення статусів пропущено: " + e.getMessage());
        }

        setupTableColumns();
        setupFilters();
        setupActions();
        loadData();
    }

    /**
     * Прив'язує стовпці таблиці до властивостей моделі бронювання.
     */
    private void setupTableColumns() {
        colBookingId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getIdBooking()).asObject());
        colArrivalDate.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDateOfArrival()));
        colDepartureDate.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDepartureDate()));
        colDepositAmount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getDepositAmount()).asObject());
        colStatusArmor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusArmor()));

        colClientName.setCellValueFactory(cellData ->
                new SimpleStringProperty(bookingService.getClientName(cellData.getValue().getIdClient()))
        );

        colRoomNumber.setCellValueFactory(cellData ->
                new SimpleStringProperty(bookingService.getRoomNumber(cellData.getValue().getIdRoom()))
        );
    }

    /**
     * Ініціалізує пошук та фільтрацію списку бронювань.
     */
    private void setupFilters() {
        filteredData = new FilteredList<>(masterData, b -> true);
        SortedList<Bookings> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(bookingsTable.comparatorProperty());
        bookingsTable.setItems(sortedData);

        Runnable updatePredicate = () -> {
            String searchText = (txtSearchClient != null ? txtSearchClient.getText().trim().toLowerCase() : "");
            boolean showCancelled = (checkShowCancelled != null && checkShowCancelled.isSelected());

            filteredData.setPredicate(booking ->
                    bookingService.matchesFilter(booking, searchText, showCancelled)
            );
        };

        txtSearchClient.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate.run());
        if (checkShowCancelled != null) {
            checkShowCancelled.selectedProperty().addListener((obs, oldVal, newVal) -> updatePredicate.run());
            checkShowCancelled.setSelected(false);
        }
        updatePredicate.run();
    }

    /**
     * Реєструє обробники подій для кнопок керування бронюваннями.
     */
    private void setupActions() {
        btnCancelBooking.setOnAction(e -> handleCancelBooking());
        btnConfirmBooking.setOnAction(e -> handleCheckIn());
        btnEditBooking.setOnAction(e -> openEditBookingModal());
    }

    /**
     * Завантажує список бронювань із бази даних у таблицю.
     */
    private void loadData() {
        try {
            masterData.setAll(bookingService.getAllBookings());
        } catch (Exception e) {
            alerts.showError("Помилка БД", "Не вдалося завантажити бронювання:\n" + e.getMessage());
        }
    }

    /**
     * Відкриває модальне вікно редагування вибраного бронювання.
     */
    private void openEditBookingModal() {
        Bookings selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alerts.showError("Увага", "Виберіть бронювання для редагування!");
            return;
        }

        if (bookingService.isBookingClosed(selected)) {
            alerts.showError("Операція заблокована", "Неможливо редагувати закрите або скасоване бронювання.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/bookings_details_modal.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Редагування броні №" + selected.getIdBooking());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(bookingsTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);

            BookingDetailsController controller = loader.getController();
            controller.setInitialData(
                    selected.getIdBooking(),
                    selected.getIdClient(),
                    bookingService.findRoomById(selected.getIdRoom()),
                    selected.getDateOfArrival().toLocalDate(),
                    selected.getDepartureDate().toLocalDate(),
                    selected.getDepositAmount()
            );

            stage.showAndWait();
            loadData();

        } catch (IOException e) {
            alerts.showError("Помилка вікна", "Не вдалося відкрити вікно редагування:\n" + e.getMessage());
        }
    }

    /**
     * Скасовує вибране бронювання та оновлює статус кімнати.
     */
    private void handleCancelBooking() {
        Bookings selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alerts.showError("Увага", "Виберіть бронювання для скасування!");
            return;
        }

        if (bookingService.isBookingClosed(selected)) {
            alerts.showError("Увага", "Це бронювання вже закрите або скасоване!");
            return;
        }

        if (!alerts.showConfirmation("Скасування", "Скасувати бронювання №" + selected.getIdBooking() + "?", "Кімната звільниться."))
            return;

        try {
            bookingService.cancelBooking(selected.getIdBooking());
            alerts.showMessage("Успіх", "Бронювання успішно скасовано.");
            loadData();
        } catch (Exception e) {
            alerts.showError("Помилка скасування", "Не вдалося змінить статус броні в БД:\n" + e.getMessage());
        }
    }

    /**
     * Оформлює фактичне поселення гостя за вибраним бронюванням.
     */
    private void handleCheckIn() {
        Bookings selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alerts.showError("Увага", "Виберіть бронювання з таблиці для поселення!");
            return;
        }

        if (bookingService.isBookingCompleted(selected)) {
            alerts.showError("Увага", "Цього гостя вже було успішно поселено!");
            return;
        }

        if (!alerts.showConfirmation("Оформлення заїзду", "Оформити фактичний заїзд гостя №" + selected.getIdBooking() + "?", "Бронювання перейде в 'Завершено'."))
            return;

        try {
            BookingService.CheckInResult result = bookingService.performCheckIn(selected);
            if (result.success) {
                loadData();
                alerts.showMessage("Успіх", "Поселення успішно оформлено!\nЗагальна вартість: " + result.formatTotalCost());
            } else {
                alerts.showError("Помилка", result.errorMessage);
            }
        } catch (Exception e) {
            alerts.showError("Критична помилка", "Не вдалося завершити Check-In:\n" + e.getMessage());
        }
    }
}