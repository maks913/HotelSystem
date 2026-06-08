package main.coursework3.controllers;

import javafx.beans.property.SimpleIntegerProperty;
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
import main.coursework3.controllers.modals.CheckInController;
import main.coursework3.controllers.modals.CheckOutController;
import main.coursework3.io.Alerts;
import main.coursework3.model.Settlements;
import main.coursework3.services.SettlementService;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class SettlementController implements Initializable {

    @FXML
    private TextField txtSearchClient;
    @FXML
    private CheckBox checkShowCompleted;
    @FXML
    private Button btnCheckOut;
    @FXML
    private Button btnEditSettlement;

    @FXML
    private TableView<Settlements> settlementsTable;
    @FXML
    private TableColumn<Settlements, String> colClient;
    @FXML
    private TableColumn<Settlements, String> colRoom;
    @FXML
    private TableColumn<Settlements, String> colArrival;
    @FXML
    private TableColumn<Settlements, String> colLeaving;
    @FXML
    private TableColumn<Settlements, Integer> colNights;
    @FXML
    private TableColumn<Settlements, String> colPaymentStatus;

    private final Alerts alerts = new Alerts();
    private final SettlementService settlementService = new SettlementService();

    private final ObservableList<Settlements> masterData = FXCollections.observableArrayList();
    private FilteredList<Settlements> filteredData;

    /**
     * Ініціалізує таблицю заселень, фільтри та обробники подій.
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTableColumns();
        setupFilters();
        setupButtonActions();
        settlementService.runRoomAutoUpdate();

        loadData();
    }

    /**
     * Завантажує список заселень із бази даних у таблицю.
     */
    private void loadData() {
        try {
            masterData.setAll(settlementService.getAllSettlements());
        } catch (Exception e) {
            alerts.showError("Помилка завантаження", "Не вдалося отримати дані з бази даних:\n" + e.getMessage());
        }
    }

    /**
     * Прив'язує стовпці таблиці до властивостей моделі заселення.
     */
    private void setupTableColumns() {
        colClient.setCellValueFactory(cellData -> {
            String clientName = settlementService.getClientName(cellData.getValue().getIdClient());
            return new SimpleStringProperty(clientName);
        });

        colRoom.setCellValueFactory(cellData -> {
            String roomNumber = settlementService.getRoomNumber(cellData.getValue().getIdRoom());
            return new SimpleStringProperty(roomNumber);
        });

        colArrival.setCellValueFactory(cellData -> {
            Date arrivalDate = cellData.getValue().getFactOfArrival();
            return new SimpleStringProperty(arrivalDate != null ? arrivalDate.toString() : "Невідомо");
        });

        colLeaving.setCellValueFactory(cellData -> {
            Date leavingDate = cellData.getValue().getFactOfLeaving();
            return new SimpleStringProperty(leavingDate != null ? leavingDate.toString() : "Невідомо");
        });

        colNights.setCellValueFactory(cellData -> {
            Date arrivalDate = cellData.getValue().getFactOfArrival();
            Date leavingDate = cellData.getValue().getFactOfLeaving();

            LocalDate start = (arrivalDate != null) ? arrivalDate.toLocalDate() : LocalDate.now();
            LocalDate end = (leavingDate != null) ? leavingDate.toLocalDate() : LocalDate.now();

            int nights = settlementService.calculateNights(start, end);
            return new SimpleIntegerProperty(nights).asObject();
        });

        colPaymentStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPaymentStatus())
        );
    }

    /**
     * Ініціалізує пошук та фільтрацію списку заселень.
     */
    private void setupFilters() {
        filteredData = new FilteredList<>(masterData, s -> true);
        SortedList<Settlements> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(settlementsTable.comparatorProperty());
        settlementsTable.setItems(sortedData);

        Runnable updatePredicate = () -> {
            String searchText = (txtSearchClient != null ? txtSearchClient.getText().trim().toLowerCase() : "");
            boolean showCompleted = (checkShowCompleted != null && checkShowCompleted.isSelected());

            filteredData.setPredicate(settlement ->
                    settlementService.matchesFilter(settlement, searchText, showCompleted)
            );
        };

        txtSearchClient.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate.run());
        if (checkShowCompleted != null) {
            checkShowCompleted.selectedProperty().addListener((obs, oldVal, newVal) -> updatePredicate.run());
            checkShowCompleted.setSelected(false);
        }
        updatePredicate.run();
    }

    /**
     * Реєструє обробники подій для кнопок керування заселеннями.
     */
    private void setupButtonActions() {
        btnCheckOut.setOnAction(event -> handleCheckOut());
        btnEditSettlement.setOnAction(event -> handleEditSettlement());
    }

    /**
     * Відкриває форму редагування параметрів проживання.
     */
    private void handleEditSettlement() {
        Settlements selected = settlementsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alerts.showError("Увага", "Будь ласка, оберіть запис для редагування!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/check_in_modal.fxml"));
            Parent root = loader.load();

            CheckInController controller = loader.getController();
            controller.setInitialData(
                    selected.getIdSettlement(),
                    selected.getIdClient(),
                    settlementService.findRoomById(selected.getIdRoom()),
                    selected.getFactOfArrival().toLocalDate(),
                    selected.getFactOfLeaving().toLocalDate(),
                    selected.getPaymentStatus()
            );

            openModalWindow("Редагування параметрів проживання", root, btnEditSettlement);
            loadData();

        } catch (IOException e) {
            e.printStackTrace();
            alerts.showError("Помилка відкриття вікна", "Не вдалося завантажити форму редагування:\n" + e.getMessage());
        }
    }

    /**
     * Виконує оформлення виселення та фінального розрахунку.
     */
    private void handleCheckOut() {
        Settlements selectedSettlement = settlementsTable.getSelectionModel().getSelectedItem();
        if (selectedSettlement == null) {
            alerts.showError("Помилка виселення", "Будь ласка, оберіть запис для виселення гостя!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/modals/check_out_modal.fxml"));
            Parent root = loader.load();

            double depositAmount = settlementService.findDepositForSettlement(selectedSettlement);

            CheckOutController controller = loader.getController();
            controller.initData(selectedSettlement, depositAmount);

            openModalWindow("Виселення та розрахунок (Check-out)", root, btnCheckOut);

            loadData();
            settlementsTable.refresh();

        } catch (IOException e) {
            e.printStackTrace();
            alerts.showError("Помилка відкриття вікна", "Не вдалося завантажити форму розрахунку:\n" + e.getMessage());
        }
    }

    /**
     * Відкриває модальне вікно з переданим інтерфейсом.
     */
    private void openModalWindow(String title, Parent root, Button ownerButton) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.initModality(Modality.WINDOW_MODAL);

        if (ownerButton != null && ownerButton.getScene() != null) {
            Stage parentStage = (Stage) ownerButton.getScene().getWindow();
            stage.initOwner(parentStage);
        }

        stage.setResizable(false);
        stage.showAndWait();
    }
}