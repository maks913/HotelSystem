package main.coursework3.io;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static Connection connection = null;
    private static final Alerts alerts = new Alerts();

    /**
     * Повертає активне з'єднання з MySQL БД.
     * Якщо з'єднання вже відкрите – повертає його повторно.
     * Параметри читаються з /main/coursework3/database.properties.
     */

    public static Connection getConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return connection;
            }

            Properties props = new Properties();
            try (InputStream input = DatabaseConnection.class.getResourceAsStream("/main/coursework3/database.properties")) {
                if (input == null) {
                    alerts.showError("Помилка конфігурації", "Файл database.properties не знайдено в ресурсах!");
                    return null;
                }
                props.load(input);

                Class.forName(props.getProperty("db.driver"));

                connection = DriverManager.getConnection(
                        props.getProperty("db.url"),
                        props.getProperty("db.user"),
                        props.getProperty("db.password")
                );

                System.out.println("Підключення до MySQL успішне!");
                return connection;

            } catch (IOException | ClassNotFoundException e) {
                alerts.showError("Помилка конфігурації", "Не вдалося завантажити драйвер або конфігурацію: " + e.getMessage());
                return null;
            }

        } catch (SQLException e) {
            alerts.showError("Помилка з'єднання з БД",
                    "Не вдалося підключитися до сервера MySQL.\n" +
                            "Перевірте, чи запущена база даних та чи правильні параметри доступу.\n\n" +
                            "Деталі: " + e.getMessage());
            return null;
        }
    }

    /**
     * Закриває поточне з'єднання з БД.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("З'єднання з БД закрито.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}