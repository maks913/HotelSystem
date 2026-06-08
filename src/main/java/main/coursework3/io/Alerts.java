package main.coursework3.io;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Alerts {

    public void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean showConfirmation(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    public void showReportWindow(String reportText, String periodName, String windowTitle) {
        Stage reportStage = new Stage();
        reportStage.setTitle(windowTitle + ": " + periodName);
        reportStage.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f4f6f9;");
        root.setAlignment(Pos.CENTER);

        Label lblTitle = new Label(windowTitle);
        lblTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        TextArea textArea = new TextArea(reportText);
        textArea.setEditable(false);
        textArea.setFont(Font.font("Consolas", 14));
        textArea.setStyle("-fx-control-inner-background: #ffffff; -fx-border-color: #bdc3c7; -fx-border-radius: 5;");
        textArea.setPrefRowCount(18);
        textArea.setPrefWidth(500);

        Button btnSave = new Button("💾 Зберегти звіт у .TXT");
        btnSave.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Зберегти звіт");
            fileChooser.setInitialFileName("Report_" + periodName.replace(" ", "_") + ".txt");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Текстовий документ (*.txt)", "*.txt"));

            File file = fileChooser.showSaveDialog(reportStage);
            if (file != null) {
                try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                    writer.println(reportText);
                    this.showMessage("Успіх", "Звіт успішно збережено за адресою:\n" + file.getAbsolutePath());
                } catch (Exception ex) {
                    this.showError("Помилка", "Не вдалося зберегти файл:\n" + ex.getMessage());
                }
            }
        });

        root.getChildren().addAll(lblTitle, textArea, btnSave);
        Scene scene = new Scene(root, 550, 600);
        reportStage.setScene(scene);
        reportStage.setResizable(false);
        reportStage.show();
    }
}