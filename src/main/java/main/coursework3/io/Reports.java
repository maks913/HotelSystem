package main.coursework3.io;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Map;

public class Reports {
    public static int convertMonthToNumber(String monthName) {
        switch (monthName.trim().toLowerCase()) {
            case "січень": return 1;
            case "лютий": return 2;
            case "березень": return 3;
            case "квітень": return 4;
            case "травень": return 5;
            case "червень": return 6;
            case "липень": return 7;
            case "серпень": return 8;
            case "вересень": return 9;
            case "жовтень": return 10;
            case "листопад": return 11;
            case "грудень": return 12;
            default:
                try {
                    return Integer.parseInt(monthName);
                } catch (NumberFormatException e) {
                    return 1;
                }
        }
    }

    public static String buildFinancialReport(String month, int year, double[] sData, double[] bData) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("              ФІНАНСОВИЙ ЗВІТ ГОТЕЛЮ              \n");
        sb.append("=========================================\n");
        sb.append(String.format("Період: %s %d року\n\n", month, year));

        sb.append("ЗВІТ ПРО ДОХОДИ ГОТЕЛЮ :\n");
        sb.append(String.format("   • Оформлено поселень: %d\n", (int) sData[0]));
        sb.append(String.format("   • ЧИСТИЙ ПРИБУТОК (Оплачено): %.2f грн\n", sData[1]));
        sb.append(String.format("   • Фінансова заборгованість (Не оплачено): %.2f грн\n\n", sData[2]));

        sb.append(String.format("   • Загальна кількість броней: %d\n", (int) bData[0]));
        sb.append(String.format("   • Сума залучених депозитів: %.2f грн\n", bData[1]));
        sb.append(String.format("   • Бронювань без передоплати: %d\n", (int) bData[2]));
        sb.append("=========================================");
        return sb.toString();
    }

    public static String buildOperationalReport(String month, int year, Object[] opStats) {
        int uniqueGuests = (int) opStats[0];
        int totalNights = (int) opStats[1];
        String popularClass = (String) opStats[2];

        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("             ОПЕРАЦІЙНИЙ ЗВІТ ГОТЕЛЮ              \n");
        sb.append("=========================================\n");
        sb.append(String.format("Період: %s %d року\n\n", month, year));

        sb.append("1. ЗАВАНТАЖЕНІСТЬ НОМЕРНОГО ФОНДУ:\n");
        sb.append(String.format("   • Загальна кількість проданих ночей: %d\n", totalNights));
        sb.append(String.format("   • Обслужено унікальних гостей: %d\n\n", uniqueGuests));

        sb.append("2. АНАЛІТИКА ПОПИТУ:\n");
        sb.append(String.format("   • Найпопулярніший клас номерів:\n     %s\n\n", popularClass));

        sb.append("=========================================\n");
        return sb.toString();
    }

    public static String buildAdvancedBusinessAnalysis(Map<String, Object> analysisData) {
        Map<String, double[]> roomStats = (Map<String, double[]>) analysisData.get("roomStats");
        Map<String, Double> classRevenues = (Map<String, Double>) analysisData.get("classRevenues");
        double totalRevenue = (double) analysisData.get("totalRevenue");
        int totalNights = (int) analysisData.get("totalNights");
        int settlementsCount = (int) analysisData.get("settlementsCount");

        double alos = settlementsCount > 0 ? (double) totalNights / settlementsCount : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append("\n=================================================\n");
        sb.append("             МАРКЕТИНГОВИЙ АНАЛІЗ ПОПИТУ         \n");
        sb.append("=================================================\n");
        sb.append(String.format("• Середня тривалість проживання гостя: %.1f доби\n", alos));
        sb.append("• Структура доходів за категоріями номерів:\n");

        String topClass = "Немає даних";
        double maxShare = -1.0;

        for (Map.Entry<String, Double> entry : classRevenues.entrySet()) {
            double classRevenue = entry.getValue();
            double share = totalRevenue > 0 ? (classRevenue / totalRevenue) * 100 : 0.0;
            sb.append(String.format("   - %s: %.1f%% прибутку (%.2f грн.)\n", entry.getKey(), share, classRevenue));

            if (share > maxShare && classRevenue > 0) {
                maxShare = share;
                topClass = entry.getKey();
            }
        }

        sb.append(String.format("👉 НАЙБІЛЬШ СТАБІЛЬНЕ ДЖЕРЕЛО ДОХОДУ: Клас \"%s\"\n", topClass));
        sb.append("=================================================\n");

        sb.append("\n-------------------------------------------------\n");
        sb.append("       ЕФЕКТИВНІСТЬ НОМЕРНОГО ФОНДУ (KPI)        \n");
        sb.append("-------------------------------------------------\n");

        for (Map.Entry<String, double[]> entry : roomStats.entrySet()) {
            double roomOccupancy = entry.getValue()[0];
            double roomRevenue = entry.getValue()[1];

            if (roomOccupancy > 0.0) {
                sb.append(String.format(
                        "Кімната №%s:\n" +
                                "  • Коефіцієнт завантаження: %.1f%%\n" +
                                "  • Прибуток за місяць: %.2f грн.\n\n",
                        entry.getKey(), roomOccupancy, roomRevenue
                ));
            } else {
                sb.append(String.format(
                        "Кімната №%s:\n" +
                                "  • 🚫 Номер пустував (Завантаження: 0.0%%)\n\n",
                        entry.getKey()
                ));
            }
        }

        return sb.toString();
    }
}