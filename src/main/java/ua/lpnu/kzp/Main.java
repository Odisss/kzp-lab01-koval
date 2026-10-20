package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/** Головний клас програми для обробки даних аптеки. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path inputPath = Path.of("data", "input.csv");
        
        List<String> lines;
        try {
            lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Помилка читання файлу: " + e.getMessage());
            return;
        }

        List<String> errors = new ArrayList<>();
        int validCount = 0;
        double totalPrice = 0.0;
        int minDaysToExpire = Integer.MAX_VALUE;
        int prescriptionCount = 0;

        for (int i = 0; i < lines.size(); i++) {
            String[] fields = lines.get(i).split(";", -1);
            
            if (fields.length != 5) {
                errors.add("Рядок %d: очікується 5 полів".formatted(i + 1));
                continue;
            }

            if (fields[0].isBlank() || fields[1].isBlank()) {
                errors.add("Рядок %d: порожня форма або назва препарату".formatted(i + 1));
                continue;
            }

            try {
                double price = Double.parseDouble(fields[2]);
                int daysToExpire = Integer.parseInt(fields[3]);
                boolean isPrescription = Boolean.parseBoolean(fields[4]);

                if (price < 0 || daysToExpire < 0) {
                    errors.add("Рядок %d: від'ємне числове значення".formatted(i + 1));
                    continue;
                }

                validCount++;
                totalPrice += price;
                minDaysToExpire = Math.min(minDaysToExpire, daysToExpire);
                if (isPrescription) {
                    prescriptionCount++;
                }

            } catch (NumberFormatException exception) {
                errors.add("Рядок %d: числове поле має помилковий формат".formatted(i + 1));
            }
        }

        double averagePrice = validCount == 0 ? 0.0 : totalPrice / validCount;

        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "Коректних записів: %d%n", validCount));
        report.append(String.format(Locale.ROOT, "Середня ціна: %.2f грн%n", averagePrice));
        report.append(String.format(Locale.ROOT, "Найменший термін: %d днів%n", minDaysToExpire == Integer.MAX_VALUE ? 0 : minDaysToExpire));
        report.append(String.format(Locale.ROOT, "Рецептурних препаратів: %d%n", prescriptionCount));
        report.append(String.format(Locale.ROOT, "Помилок: %d%n", errors.size()));
        
        System.out.print(report.toString());
        errors.forEach(System.out::println);
        try {
            FileReport.writeReport(Path.of("out", "report.txt"), report.toString());
        } catch (IOException e) {
            System.err.println("Помилка запису у файл: " + e.getMessage());
        }
    }
}