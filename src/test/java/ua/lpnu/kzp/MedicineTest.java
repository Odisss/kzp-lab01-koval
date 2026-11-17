package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Тести класу-сутності {@link Medicine}: конструктор, fromCsv, equals і toString. */
class MedicineTest {

    // ---------- Конструктор ----------

    /* Коректний конструктор зберігає всі поля. */
    @Test
    void constructorStoresAllFields() {
        // Arrange і Act
        Medicine medicine = new Medicine("Інсулін", "розчин", 520.0, 90, true);

        // Assert
        assertEquals("Інсулін", medicine.name());
        assertEquals("розчин", medicine.form());
        assertEquals(520.0, medicine.price(), 0.0001);
        assertEquals(90, medicine.daysToExpire());
        assertTrue(medicine.prescription());
    }

    /* Граничний випадок: нульові ціна й термін допустимі. */
    @Test
    void constructorAcceptsZeroPriceAndDays() {
        Medicine medicine = new Medicine("Проба", "гель", 0.0, 0, false);

        assertEquals(0.0, medicine.price(), 0.0001);
        assertEquals(0, medicine.daysToExpire());
    }

    /* null у текстових полях відхиляється як відсутнє значення. */
    @Test
    void constructorRejectsNullText() {
        assertThrows(NullPointerException.class,
                () -> new Medicine(null, "таблетки", 1.0, 1, false));
        assertThrows(NullPointerException.class,
                () -> new Medicine("Аспірин", null, 1.0, 1, false));
    }

    /* Порожній текст і самі пробіли відхиляються з поясненням. */
    @Test
    void constructorRejectsBlankText() {
        IllegalArgumentException name = assertThrows(IllegalArgumentException.class,
                () -> new Medicine("  ", "таблетки", 1.0, 1, false));
        IllegalArgumentException form = assertThrows(IllegalArgumentException.class,
                () -> new Medicine("Аспірин", "", 1.0, 1, false));

        assertEquals("порожня назва препарату", name.getMessage());
        assertEquals("порожня форма випуску", form.getMessage());
    }

    /* Від'ємна, NaN і нескінченна ціна відхиляються. */
    @Test
    void constructorRejectsInvalidPrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new Medicine("А", "б", -0.01, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Medicine("А", "б", Double.NaN, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> new Medicine("А", "б", Double.POSITIVE_INFINITY, 1, false));
    }

    /* Від'ємний термін відхиляється, повідомлення збігається з лабораторною роботою № 1. */
    @Test
    void constructorRejectsNegativeDays() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new Medicine("А", "б", 1.0, -1, false));

        assertEquals("від'ємний термін придатності: -1", exception.getMessage());
    }

    // ---------- fromCsv ----------

    /* Коректний рядок перетворюється на об'єкт; пробіли навколо полів ігноруються. */
    @Test
    void fromCsvParsesValidLine() {
        Medicine medicine = Medicine.fromCsv(" Амоксицилін ; капсули ; 132.00 ; 300 ; TRUE ");

        assertEquals(new Medicine("Амоксицилін", "капсули", 132.0, 300, true), medicine);
    }

    /* Неправильна кількість полів: замало і забагато. */
    @Test
    void fromCsvRejectsWrongFieldCount() {
        IllegalArgumentException few = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Аспірин;таблетки;41.00;250"));
        assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Аспірин;таблетки;41.00;250;false;зайве"));

        assertEquals("очікується 5 полів, знайдено 4", few.getMessage());
    }

    /* Неправильне число в ціні й терміні. */
    @Test
    void fromCsvRejectsWrongNumber() {
        IllegalArgumentException price = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Нурофен;сироп;ціна;200;false"));
        assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Нурофен;сироп;45,5;200;false"));
        assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Нурофен;сироп;45.5;20.5;false"));

        assertEquals("ціна не є числом: 'ціна'", price.getMessage());
    }

    /* Синтаксично коректний рядок, що порушує інваріант, відхиляє конструктор. */
    @Test
    void fromCsvRejectsInvariantViolation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Но-шпа;таблетки;-15.00;365;false"));

        assertEquals("від'ємна ціна: -15.00", exception.getMessage());
    }

    /* Ознака рецепта лише true/false. */
    @Test
    void fromCsvRejectsWrongBoolean() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Medicine.fromCsv("Вітамін C;таблетки;75.00;500;можливо"));

        assertEquals("ознака рецепта має бути true або false: 'можливо'",
                exception.getMessage());
    }

    /* Порожній рядок і null. */
    @Test
    void fromCsvRejectsBlankAndNull() {
        assertThrows(IllegalArgumentException.class, () -> Medicine.fromCsv("   "));
        assertThrows(NullPointerException.class, () -> Medicine.fromCsv(null));
    }

    // ---------- equals і toString ----------

    /* Рівність за значеннями: однакові поля – рівні об'єкти й хеш-коди. */
    @Test
    void equalsComparesValues() {
        Medicine first = new Medicine("Цитрамон", "таблетки", 32.2, 60, false);
        Medicine same = new Medicine("Цитрамон", "таблетки", 32.2, 60, false);
        Medicine other = new Medicine("Цитрамон", "таблетки", 32.2, 61, false);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, other);
        assertFalse(first.equals(null));
    }

    /* toString використовує крапку навіть за української локалі. */
    @Test
    void toStringUsesRootLocale() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("uk-UA"));
            Medicine medicine = new Medicine("Інсулін", "розчин", 520.0, 90, true);

            assertEquals("Інсулін (розчин): 520.00 грн, 90 дн., за рецептом",
                    medicine.toString());
        } finally {
            Locale.setDefault(saved);
        }
    }
}
