package main.coursework3.controllers.modals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import main.coursework3.dao.ClientDAO;
import main.coursework3.io.Alerts;
import main.coursework3.model.Clients;

import java.sql.Date;
import java.time.LocalDate;

public class ClientDetailsController {

    @FXML
    private TextField pibField;
    @FXML
    private TextField passportSeriaField;
    @FXML
    private TextField passportNumberField;
    @FXML
    private TextField phoneField;
    @FXML
    private DatePicker birthdayPicker;

    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;

    private Clients currentClient;
    private final ClientDAO clientDAO = new ClientDAO();
    private final Alerts alerts = new Alerts();

    /**
     * Ініціалізація форми клієнта та налаштування обробників подій.
     */
    @FXML
    public void initialize() {
        btnCancel.setOnAction(event -> closeWindow());
        btnSave.setOnAction(event -> handleSave());

        phoneField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[\\d\\+\\-\\(\\)\\s]*")) {
                phoneField.setText(newValue.replaceAll("[^\\d\\+\\-\\(\\)\\s]", ""));
            }
        });

        passportNumberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                passportNumberField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    /**
     * Заповнення форми даними обраного клієнта.
     */
    public void setClientData(Clients client) {
        if (client == null) return;

        this.currentClient = client;
        pibField.setText(client.getPib());
        passportSeriaField.setText(client.getPasportSeria());
        passportNumberField.setText(client.getPasportNumber());
        phoneField.setText(client.getPhoneNumber());

        if (client.getDateOfBirth() != null) {
            birthdayPicker.setValue(client.getDateOfBirth().toLocalDate());
        }
    }

    /**
     * Перевірка та збереження даних клієнта у базі даних.
     */
    private void handleSave() {
        String pib = pibField.getText().trim();
        String phone = phoneField.getText().trim();
        String passportSeria = passportSeriaField.getText().trim();
        String passportNumber = passportNumberField.getText().trim();
        LocalDate birthday = birthdayPicker.getValue();

        if (pib.isEmpty() || phone.isEmpty()) {
            alerts.showError("Помилка заповнення", "Поля 'ПІБ' та 'Телефон' є обов'язковими для реєстрації клієнта!");
            return;
        }

        if (birthday != null && birthday.isAfter(LocalDate.now().minusYears(16))) {
            alerts.showError("Некоректна дата", "Клієнт повинен бути старше 16 років для укладання договору проживання.");
            return;
        }

        try {
            if (currentClient == null) {
                currentClient = new Clients();
            }

            currentClient.setPib(pib);
            currentClient.setPasportSeria(passportSeria.isEmpty() ? null : passportSeria);
            currentClient.setPasportNumber(passportNumber.isEmpty() ? null : passportNumber);
            currentClient.setPhoneNumber(phone);

            currentClient.setDateOfBirth(birthday != null ? Date.valueOf(birthday) : null);

            if (currentClient.getIdClient() == 0) {
                clientDAO.insertClient(currentClient);
                alerts.showMessage("Успіх", "Картку нового клієнта успішно створено!");
            } else {
                clientDAO.updateClient(currentClient);
                alerts.showMessage("Успіх", "Дані клієнта успішно оновлено.");
            }

            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            alerts.showError("Помилка бази даних",
                    "Не вдалося зберегти анкету клієнта. Перевірте унікальність номера паспорта або телефону.\nДеталі: " + e.getMessage());
        }
    }

    /**
     * Закриття поточного модального вікна.
     */
    private void closeWindow() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            Stage stage = (Stage) btnCancel.getScene().getWindow();
            stage.close();
        }
    }
}