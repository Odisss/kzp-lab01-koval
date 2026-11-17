package ua.lpnu.kzp;

import java.util.List;
import java.util.Locale;

/** Формує текст звіту; не читає файлів і не виконує обчислень. */
public final class ReportFormatter {
    /* Забороняє створення екземплярів службового класу. */
    private ReportFormatter() {
    }

    /**
     * Будує повний текст звіту.
     *
     * @param stats  показники
     * @param errors повідомлення про пропущені рядки
     * @return текст звіту з платформонезалежними переносами рядків
     */
    public static String format(Stats stats, List<String> errors) {
        StringBuilder report = new StringBuilder();
        // Locale.ROOT гарантує крапку як десятковий роздільник у будь-якій ОС.
        report.append(String.format(Locale.ROOT, "Звіт: аптека%n"));
        report.append(String.format(Locale.ROOT, "Коректних записів: %d%n", stats.count()));
        if (stats.count() == 0) {
            report.append(String.format(Locale.ROOT, "Середня ціна: —%n"));
            report.append(String.format(Locale.ROOT, "Найменший термін придатності: —%n"));
        } else {
            report.append(String.format(Locale.ROOT, "Середня ціна: %.2f грн%n",
                    stats.averagePrice()));
            report.append(String.format(Locale.ROOT, "Найменший термін придатності: %d дн.%n",
                    stats.minDaysToExpire()));
        }
        report.append(String.format(Locale.ROOT, "Рецептурних препаратів: %d%n",
                stats.prescriptionCount()));
        report.append(String.format(Locale.ROOT, "Пропущено рядків: %d%n", errors.size()));
        for (String error : errors) {
            report.append(String.format(Locale.ROOT, "  %s%n", error));
        }
        return report.toString();
    }
}
