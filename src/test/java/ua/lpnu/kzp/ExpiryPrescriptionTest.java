package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Тести допоміжного record {@link ExpiryPrescription}. */
class ExpiryPrescriptionTest {

    /* Компоненти доступні через автоматично створені методи. */
    @Test
    void componentsAreAccessible() {
        ExpiryPrescription summary = new ExpiryPrescription(60, 2);

        assertEquals(60, summary.minDaysToExpire());
        assertEquals(2, summary.prescriptionCount());
    }

    /* record порівнюється за значеннями компонентів. */
    @Test
    void equalityByValue() {
        assertEquals(new ExpiryPrescription(60, 2), new ExpiryPrescription(60, 2));
        assertEquals(new ExpiryPrescription(60, 2).hashCode(),
                new ExpiryPrescription(60, 2).hashCode());
        assertNotEquals(new ExpiryPrescription(60, 2), new ExpiryPrescription(60, 3));
    }

    /* Від'ємна кількість і термін менший за -1 відхиляються; -1 – допустима межа. */
    @Test
    void compactConstructorValidates() {
        assertThrows(IllegalArgumentException.class, () -> new ExpiryPrescription(0, -1));
        assertThrows(IllegalArgumentException.class, () -> new ExpiryPrescription(-2, 0));
        assertEquals(ExpiryPrescription.NO_DATA,
                new ExpiryPrescription(ExpiryPrescription.NO_DATA, 0).minDaysToExpire());
    }

    /* Підсумок для даних варіанта збігається з ручним розрахунком. */
    @Test
    void ofComputesSummary() {
        ExpiryPrescription summary = ExpiryPrescription.of(List.of(
                Medicine.of("Парацетамол", "таблетки", 45.50, 420, false),
                Medicine.of("Амоксицилін", "капсули", 132.00, 300, true),
                Medicine.of("Цитрамон", "таблетки", 32.20, 60, false),
                Medicine.of("Інсулін", "розчин", 520.00, 90, true)));

        assertEquals(new ExpiryPrescription(60, 2), summary);
    }

    /* Порожній набір дає значення NO_DATA, як у попередній роботі. */
    @Test
    void ofEmptyList() {
        assertEquals(new ExpiryPrescription(-1, 0), ExpiryPrescription.of(List.of()));
    }
}
