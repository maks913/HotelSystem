package main.coursework3;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.coursework3.io.Alerts;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main/coursework3/fxml/main_view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setTitle("Hotel Management System");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            new Alerts().showError("Критична помилка запуску",
                    "Не вдалося завантажити головний FXML файл системи:\n" + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }
}