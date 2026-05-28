package main.coursework3.controllers.modals;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import main.coursework3.io.Alerts;
import main.coursework3.model.Bookings;
import main.coursework3.model.Clients;
import main.coursework3.model.Rooms;
import main.coursework3.dao.RoomDAO;
import main.coursework3.services.BookingService;
import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class BookingDetailsController implements Initializable {
    @FXML private ComboBox<Clients> cmbClient;
    @FXML private Button btnAddNewClient;
    @FXML private ComboBox<Rooms> cmbRoom;
    @FXML private DatePicker dpArrivalDate;
    @FXML private DatePicker dpDateLeaving;
    @FXML private TextField fieldDeposit;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final BookingService bookingService = new BookingService();
    private final Alerts alerts = new Alerts();

    private int currentBookingId = 0;
    private Rooms initialRoomRef = null;
    private LocalDate originalArrival = null;
    private LocalDate originalDeparture = null;
    /** Ініціалізація контролера та налаштування елементів форми. */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshClientList(false);
        setupConverters();

        dpArrivalDate.setValue(LocalDate.now());
        dpDateLeaving.setValue(LocalDate.now().plusDays(1));

        btnCancel.setOnAction(e -> closeWindow(btnCancel));
        btnSave.setOnAction(e -> handleSave());
        btnAddNewClient.setOnAction(e -> openNewClientModal());

        dpArrivalDate.valueProperty().addListener((obs, oldDate, newDate) -> updateAvailableRoomsList());
        dpDateLeaving.valueProperty().addListener((obs, oldDate, newDate) -> updateAvailableRoomsList());

        updateAvailableRoomsList();
    }
    /** Заповнення форми даними існуючого бронювання. */
    public void setInitialData(int idBooking, int idClient, Rooms initialRoom, LocalDate arrivalDate, LocalDate departureDate, double deposit) {
        this.currentBookingId = idBooking;
        this.initialRoomRef = initialRoom;
        this.originalArrival = arrivalDate;
        this.originalDeparture = departureDate;

        if (arrivalDate != null) dpArrivalDate.setValue(arrivalDate);
        if (departureDate != null) dpDateLeaving.setValue(departureDate);

        fieldDeposit.setText(String.valueOf(deposit));
        updateAvailableRoomsList();

        if (idClient > 0) {
            for (Clients client : cmbClient.getItems()) {
                if (client.getIdClient() == idClient) {
                    cmbClient.setValue(client);
                    break;
                }
            }
        }
    }
    /** Оновлення списку клієнтів у комбобоксі. */
    private void refreshClientList(boolean selectNewest) {
        try {
            List<Clients> clients = bookingService.getAllClients();
            cmbClient.setItems(FXCollections.observableArrayList(clients));

            if (selectNewest && !clients.isEmpty()) {
                Clients newestClient = clients.stream()
                        .max(Comparator.comparingInt(Clients::getIdClient))
                        .orElse(null);
                cmbClient.getSelectionModel().select(newestClient);
            }
        } catch (Exception e) {
            System.err.println("Не вдалося оновити список клієнтів: " + e.getMessage());
        }
    }
    /** Відкриття модального вікна для додавання нового клієнта. */
    private void openNewClientModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/client_details_modal.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Додавання нового клієнта");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(cmbClient.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            refreshClientList(true);

        } catch (IOException e) {
            e.printStackTrace();
            alerts.showError("Помилка завантаження", "Не вдалося відкрити вікно додавання клієнта.");
        }
    }
    /** Налаштування відображення клієнтів та номерів у комбобоксах. */
    private void setupConverters() {
        cmbClient.setConverter(new StringConverter<>() {
            @Override
            public String toString(Clients client) {
                return client == null ? "" : client.getPib() + " (Паспорт: " + client.getPasportNumber() + ")";
            }
            @Override
            public Clients fromString(String string) { return null; }
        });

        cmbRoom.setConverter(new StringConverter<>() {
            @Override
            public String toString(Rooms room) {
                return room == null ? "" : "№" + room.getRoomNumber() + " - " + room.getRoomClass();
            }
            @Override
            public Rooms fromString(String string) { return null; }
        });
    }
    /** Перевірка та збереження даних бронювання. */
    private void handleSave() {
        Clients selectedClient = cmbClient.getValue();
        Rooms selectedRoom = cmbRoom.getValue();
        LocalDate arrival = dpArrivalDate.getValue();
        LocalDate departure = dpDateLeaving.getValue();
        String depositText = fieldDeposit.getText().trim();

        if (selectedClient == null || selectedRoom == null || arrival == null || departure == null || depositText.isEmpty()) {
            alerts.showError("Помилка заповнення", "Будь ласка, заповніть усі поля форми!");
            return;
        }

        if (!departure.isAfter(arrival)) {
            alerts.showError("Помилка дат", "Дата виїзду має бути хоча б на 1 day пізніше дати заїзду!");
            return;
        }

        double depositAmount;
        try {
            depositAmount = Double.parseDouble(depositText.replace(",", "."));
            if (depositAmount < 0) {
                alerts.showError("Некоректна сума", "Сума депозиту не може бути від'ємною!");
                return;
            }
        } catch (NumberFormatException e) {
            alerts.showError("Помилка формату", "Некоректно введена сума авансового депозиту.");
            return;
        }

        try {
            List<Rooms> freeRoomsOnTheseDates = bookingService.findAvailableRoomsByDates(arrival, departure);

            boolean isRoomFree = freeRoomsOnTheseDates.stream()
                    .anyMatch(r -> r.getIdRoom() == selectedRoom.getIdRoom());
            if (currentBookingId > 0 && this.initialRoomRef != null) {
                boolean isSameRoom = (selectedRoom.getIdRoom() == initialRoomRef.getIdRoom());

                if (!isRoomFree && !isSameRoom) {
                    alerts.showError("Номер зайнято", "На вибрані дати цей номер уже закріплено за іншим бронюванням!");
                    return;
                }
            } else {
                if (!isRoomFree) {
                    alerts.showError("Номер зайнято", "Неможливо створити бронювання. Цей номер уже закритий на вибрані дати!");
                    return;
                }
            }

            Bookings booking = new Bookings();
            booking.setIdClient(selectedClient.getIdClient());
            booking.setIdRoom(selectedRoom.getIdRoom());
            booking.setDateOfArrival(Date.valueOf(arrival));
            booking.setDepartureDate(Date.valueOf(departure));
            booking.setDepositAmount(depositAmount);
            booking.setStatusArmor(depositAmount > 0 ? "Підтверджено" : "Очікує оплати");

            if (arrival.equals(LocalDate.now())) {
                bookingService.updateRoomStatusById(selectedRoom.getIdRoom(), RoomDAO.STATUS_BOOKED);
            }

            if (currentBookingId == 0) {
                bookingService.insertBooking(booking);
                alerts.showMessage("Успіх", "Бронювання успішно створено!");
            } else {
                booking.setIdBooking(currentBookingId);
                bookingService.updateBooking(booking);
                alerts.showMessage("Успіх", "Параметри броні №" + currentBookingId + " успішно змінено.");
            }

            bookingService.runAutoUpdate();
            closeWindow(btnSave);

        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка БД", "Не вдалося зберегти картку бронювання:\n" + e.getMessage());
        }
    }
    /** Оновлення списку доступних номерів за вибраними датами. */
    private void updateAvailableRoomsList() {
        LocalDate arrival = dpArrivalDate.getValue();
        LocalDate departure = dpDateLeaving.getValue();

        if (arrival == null || departure == null || departure.isBefore(arrival)) {
            cmbRoom.setItems(FXCollections.observableArrayList());
            return;
        }

        try {
            List<Rooms> freeRooms = bookingService.findAvailableRoomsByDates(arrival, departure);
            ObservableList<Rooms> roomOptions = FXCollections.observableArrayList(freeRooms);

            if (this.initialRoomRef != null) {
                boolean containsInitial = roomOptions.stream()
                        .anyMatch(r -> r.getIdRoom() == this.initialRoomRef.getIdRoom());
                if (!containsInitial) {
                    roomOptions.add(this.initialRoomRef);
                }
            }

            Rooms previouslySelected = cmbRoom.getValue();
            cmbRoom.setItems(roomOptions);

            if (previouslySelected != null) {
                roomOptions.stream()
                        .filter(r -> r.getIdRoom() == previouslySelected.getIdRoom())
                        .findFirst()
                        .ifPresent(cmbRoom::setValue);
            } else if (this.initialRoomRef != null) {
                roomOptions.stream()
                        .filter(r -> r.getIdRoom() == this.initialRoomRef.getIdRoom())
                        .findFirst()
                        .ifPresent(cmbRoom::setValue);
            }
        } catch (Exception e) {
            System.err.println("Помилка при розрахунку доступних номерів: " + e.getMessage());
        }
    }
    /** Закриття поточного модального вікна. */
    private void closeWindow(Button sourceButton) {
        if (sourceButton != null && sourceButton.getScene() != null) {
            Stage stage = (Stage) sourceButton.getScene().getWindow();
            stage.close();
        }
    }
}