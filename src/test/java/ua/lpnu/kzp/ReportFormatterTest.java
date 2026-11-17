package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class ReportFormatterTest {
    /* Числа виводяться з двома знаками й крапкою, навіть якщо локаль використовує кому. */
    @Test
    void usesDotAndTwoDecimalsRegardlessOfLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("uk-UA"));
            String report = ReportFormatter.format(new Stats(2, 1234.5, 10, 1), List.of());

            assertTrue(report.contains("Середня ціна: 1234.50 грн"), report);
            assertFalse(report.contains("1234,50"), report);
        } finally {
            Locale.setDefault(previous);
        }
    }

    /* Звіт містить усі показники та повідомлення про помилки з номерами рядків. */
    @Test
    void containsIndicatorsAndErrors() {
        String report = ReportFormatter.format(new Stats(3, 10.0, 5, 2),
                List.of("Рядок 4: порожній рядок"));

        assertTrue(report.contains("Коректних записів: 3"), report);
        assertTrue(report.contains("Найменший термін придатності: 5 дн."), report);
        assertTrue(report.contains("Рецептурних препаратів: 2"), report);
        assertTrue(report.contains("Пропущено рядків: 1"), report);
        assertTrue(report.contains("Рядок 4: порожній рядок"), report);
    }

    /* Для порожнього набору замість вигаданих чисел виводиться тире. */
    @Test
    void emptyStatsShowDashes() {
        String report = ReportFormatter.format(new Stats(0, 0.0, -1, 0), List.of());

        assertTrue(report.contains("Середня ціна: —"), report);
        assertTrue(report.contains("Найменший термін придатності: —"), report);
    }
}
