package main.coursework3.controllers.modals;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import main.coursework3.io.Alerts;
import main.coursework3.model.Settlements;
import main.coursework3.services.SettlementService;

import java.time.LocalDate;

public class CheckOutController {

    @FXML
    private Label lblClientName;
    @FXML
    private Label lblRoomInfo;
    @FXML
    private Label lblArrivalDate;
    @FXML
    private DatePicker dpDepartureDate;
    @FXML
    private Label lblNights;
    @FXML
    private TextField txtTotalCost;
    @FXML
    private ComboBox<String> cmbPaymentStatus;

    @FXML
    private Label lblTotalCost;
    @FXML
    private Label lblDepositUsed;
    @FXML
    private Label lblFinalPayment;

    @FXML
    private Button btnConfirmOut;
    @FXML
    private Button btnCancel;

    private final SettlementService settlementService = new SettlementService();
    private final Alerts alerts = new Alerts();

    private Settlements currentSettlement;
    private double pricePerDay = 0.0;
    private double currentDeposit = 0.0;
    private SettlementService.CheckOutFinances lastFinances = null;

    /**
     * Ініціалізація форми виселення та налаштування елементів керування.
     */
    @FXML
    public void initialize() {
        if (cmbPaymentStatus != null) {
            cmbPaymentStatus.setItems(FXCollections.observableArrayList("Оплачено", "Не оплачено"));
            cmbPaymentStatus.setValue("Оплачено");
        }

        if (dpDepartureDate != null) {
            dpDepartureDate.setValue(LocalDate.now());

            dpDepartureDate.valueProperty().addListener((obs, oldDate, newDate) -> {
                if (currentSettlement != null) {
                    recalculateFinances();
                }
            });
        }

        if (btnCancel != null) btnCancel.setOnAction(e -> closeWindow(btnCancel));
        if (btnConfirmOut != null) btnConfirmOut.setOnAction(e -> handleConfirmCheckOut());

        if (currentSettlement != null) recalculateFinances();

        if (currentSettlement != null && currentSettlement.getFactOfLeaving() != null) {
            dpDepartureDate.setValue(currentSettlement.getFactOfLeaving().toLocalDate());
        } else {
            dpDepartureDate.setValue(LocalDate.now());
        }
    }

    /**
     * Заповнення форми даними поточного поселення та депозиту.
     */
    public void initData(Settlements settlement, double depositAmount) {
        this.currentSettlement = settlement;
        this.currentDeposit = depositAmount;
        this.pricePerDay = settlementService.resolvePricePerDay(settlement);

        if (lblClientName != null) lblClientName.setText(settlementService.getClientName(settlement.getIdClient()));
        if (lblRoomInfo != null)
            lblRoomInfo.setText("Кімн. " + settlementService.getRoomNumber(settlement.getIdRoom()));
        if (lblArrivalDate != null) lblArrivalDate.setText(settlement.getFactOfArrival().toString());

        if (dpDepartureDate != null) recalculateFinances();
    }

    /**
     * Перераховує фінанси через сервіс та оновлює UI-лейбли.
     */
    private void recalculateFinances() {
        if (currentSettlement == null || dpDepartureDate == null || dpDepartureDate.getValue() == null) return;

        LocalDate arrival = currentSettlement.getFactOfArrival().toLocalDate();
        LocalDate actualDeparture = dpDepartureDate.getValue();

        if (actualDeparture.isBefore(arrival)) {
            actualDeparture = arrival.plusDays(1);
            dpDepartureDate.setValue(actualDeparture);
        }

        lastFinances = settlementService.calculateCheckOutFinances(arrival, actualDeparture, pricePerDay, currentDeposit);

        if (lblNights != null) lblNights.setText(String.valueOf(lastFinances.nights));
        if (lblTotalCost != null) lblTotalCost.setText(String.format("%.2f грн", lastFinances.totalCost));
        if (lblDepositUsed != null) lblDepositUsed.setText(String.format("%.2f грн", lastFinances.depositUsed));
        if (lblFinalPayment != null) lblFinalPayment.setText(String.format("%.2f грн", lastFinances.finalPayment));
        if (txtTotalCost != null)
            txtTotalCost.setText(String.format("%.2f", lastFinances.finalPayment).replace(",", "."));
    }

    /**
     * Підтвердження процедури виселення клієнта.
     */
    private void handleConfirmCheckOut() {
        if (currentSettlement == null || lastFinances == null) return;

        String selectedStatus = cmbPaymentStatus != null ? cmbPaymentStatus.getValue() : "Оплачено";

        boolean confirm = alerts.showConfirmation(
                "Підтвердження виселення",
                "Оформити виселення клієнта?",
                String.format("Фактична сума до сплати з урахуванням депозиту: %.2f грн. Статус: %s",
                        lastFinances.finalPayment, selectedStatus)
        );

        if (!confirm) return;

        try {
            boolean success = settlementService.processCheckOutTransaction(
                    currentSettlement.getIdSettlement(),
                    currentSettlement.getIdRoom(),
                    lastFinances.totalCost,
                    selectedStatus
            );

            if (success) {
                alerts.showMessage("Успіх", "Клієнта успішно виселено! Кімнату звільнено для наступних заселень.");
                closeWindow(btnConfirmOut);
            } else {
                alerts.showError("Помилка бази даних", "Не вдалося зберегти фінансові зміни виселения в СУБД.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Системна помилка", "Помилка при обробці процедуры Check-out:\n" + e.getMessage());
        }
    }

    /**
     * Закриття поточного модального вікна.
     */
    private void closeWindow(Button sourceButton) {
        if (sourceButton != null && sourceButton.getScene() != null) {
            Stage stage = (Stage) sourceButton.getScene().getWindow();
            stage.close();
        }
    }
}