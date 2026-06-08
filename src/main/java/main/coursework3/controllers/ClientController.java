package main.coursework3.controllers;

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
import main.coursework3.controllers.modals.ClientDetailsController;
import main.coursework3.dao.ClientDAO;
import main.coursework3.io.Alerts;
import main.coursework3.model.Clients;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class ClientController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private Button btnAddClient;
    @FXML
    private TableView<Clients> clientsTable;

    @FXML
    private TableColumn<Clients, Integer> colId;
    @FXML
    private TableColumn<Clients, String> colPib;
    @FXML
    private TableColumn<Clients, String> colPassportSeria;
    @FXML
    private TableColumn<Clients, String> colPassportNumber;
    @FXML
    private TableColumn<Clients, String> colPhone;
    @FXML
    private TableColumn<Clients, LocalDate> colBirthday;

    private final ClientDAO clientDAO = new ClientDAO();
    private final Alerts alerts = new Alerts();

    private final ObservableList<Clients> masterData = FXCollections.observableArrayList();

    /**
     * Ініціалізація контролера та налаштування таблиці, дій і пошуку.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupActions();
        setupContextMenu();
        setupSearchFilter();

        loadData();
    }

    /**
     * Налаштування колонок таблиці клієнтів.
     */
    private void setupTableColumns() {
        colId.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getIdClient()).asObject());
        colPib.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPib()));
        colPassportSeria.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPasportSeria()));
        colPassportNumber.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPasportNumber()));
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhoneNumber()));
        colBirthday.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDateOfBirth().toLocalDate()));
    }

    /**
     * Налаштування кнопок та подвійного кліку по рядку таблиці.
     */
    private void setupActions() {
        btnAddClient.setOnAction(e -> openClientDetailsModal(null));

        clientsTable.setRowFactory(tv -> {
            TableRow<Clients> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openClientDetailsModal(row.getItem());
                }
            });
            return row;
        });
    }

    /**
     * Створення контекстного меню для таблиці клієнтів.
     */
    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("❌ Видалити клієнта");
        deleteItem.setOnAction(e -> handleDeleteClient());
        contextMenu.getItems().add(deleteItem);

        clientsTable.setContextMenu(contextMenu);
    }

    /**
     * Завантаження списку клієнтів із бази даних.
     */
    private void loadData() {
        try {
            masterData.setAll(clientDAO.findAll());
        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка БД", "Не вдалося завантажити список клієнтів:\n" + e.getMessage());
        }
    }

    /**
     * Налаштування пошуку та фільтрації клієнтів.
     */
    private void setupSearchFilter() {
        try {
            List<Clients> initialList = clientDAO.findAll();
            masterData.setAll(initialList);
            SortedList<Clients> initialSorted = new SortedList<>(masterData);
            initialSorted.comparatorProperty().bind(clientsTable.comparatorProperty());
            clientsTable.setItems(initialSorted);
        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка бази даних", "Не вдалося завантажити початковий список клієнтів.");
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                List<Clients> searchResult;

                if (newValue == null || newValue.trim().isEmpty()) {
                    searchResult = clientDAO.findAll();
                } else {
                    searchResult = clientDAO.findBySearchFilter(newValue);
                }

                masterData.setAll(searchResult);

                SortedList<Clients> sortedData = new SortedList<>(masterData);
                sortedData.comparatorProperty().bind(clientsTable.comparatorProperty());
                clientsTable.setItems(sortedData);

            } catch (Exception e) {
                e.printStackTrace();
                alerts.showError("Помилка пошуку", "Не вдалося відфільтрувати дані через БД:\n" + e.getMessage());
            }
        });
    }

    /**
     * Відкриття модального вікна для додавання або редагування клієнта.
     */
    private void openClientDetailsModal(Clients client) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/client_details_modal.fxml"));
            Parent root = loader.load();
            ClientDetailsController controller = loader.getController();

            if (client != null) {
                controller.setClientData(client);
            }

            Stage stage = new Stage();
            stage.setTitle(client == null ? "Додавання нового клієнта" : "Редагування профілю клієнта: " + client.getPib());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(clientsTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            loadData();

        } catch (IOException e) {
            e.printStackTrace();
            alerts.showError("Помилка завантажения вікна", "Не вдалося відкрити форму деталей клієнта:\n" + e.getMessage());
        }
    }

    /**
     * Видалення обраного клієнта з бази даних.
     */
    private void handleDeleteClient() {
        Clients selectedClient = clientsTable.getSelectionModel().getSelectedItem();
        if (selectedClient == null) {
            alerts.showError("Вибір відсутній", "Будь ласка, оберіть клієнта з таблиці для видалення.");
            return;
        }

        boolean isConfirmed = alerts.showConfirmation(
                "Підтвердження видалення",
                "Ви впевнені, що хочете видалити клієнта " + selectedClient.getPib() + "?",
                "Ця дія незворотна."
        );

        if (isConfirmed) {
            try {
                clientDAO.deleteClient(selectedClient.getIdClient());
                alerts.showMessage("Успіх", "Картку клієнта успішно видалено.");
                loadData();
            } catch (Exception e) {
                e.printStackTrace();
                alerts.showError(
                        "Помилка видалення",
                        "Неможливо видалити цього клієнта з бази даних!\n" +
                                "Він пов'язаний з активними бронюваннями або історією проживання (Foreign Key Constraint)."
                );
            }
        }
    }
}