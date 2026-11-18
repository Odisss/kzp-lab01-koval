package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StatsTest {
    private static final List<Medicine> SAMPLE = List.of(
            Medicine.of("Парацетамол", "таблетки", 45.50, 420, false),
            Medicine.of("Амоксицилін", "капсули", 132.00, 300, true),
            Medicine.of("Цитрамон", "таблетки", 32.20, 60, false),
            Medicine.of("Інсулін", "розчин", 520.00, 90, true));

    /* Усі чотири показники на відомому наборі. */
    @Test
    void computesAllFourIndicators() {
        Stats stats = Stats.compute(SAMPLE);

        assertEquals(4, stats.count());
        assertEquals(182.425, stats.averagePrice(), 0.0001);
        assertEquals(60, stats.minDaysToExpire());
        assertEquals(2, stats.prescriptionCount());
    }

    /* Середнє має дробову частину, а не відкинуте цілочислове ділення. */
    @Test
    void averageKeepsFractionalPart() {
        Stats stats = Stats.compute(List.of(
                Medicine.of("А", "б", 1.0, 1, false),
                Medicine.of("В", "г", 2.0, 2, false)));

        assertEquals(1.5, stats.averagePrice(), 0.0001);
    }

    /* Один запис: мінімум і середнє дорівнюють його значенням. */
    @Test
    void singleRecord() {
        Stats stats = Stats.compute(List.of(Medicine.of("А", "б", 10.0, 7, true)));

        assertEquals(1, stats.count());
        assertEquals(10.0, stats.averagePrice(), 0.0001);
        assertEquals(7, stats.minDaysToExpire());
        assertEquals(1, stats.prescriptionCount());
    }

    /* Порожній набір не ділить на нуль. */
    @Test
    void emptyInputGivesZeroCount() {
        Stats stats = Stats.compute(List.of());

        assertEquals(0, stats.count());
        assertEquals(0.0, stats.averagePrice(), 0.0001);
        assertEquals(-1, stats.minDaysToExpire());
        assertEquals(0, stats.prescriptionCount());
    }

    /* Невалідні рядки не змінюють показників. */
    @Test
    void invalidLinesDoNotAffectStats() {
        ParseResult parsed = MedicineParser.parseAll(List.of(
                "Парацетамол;таблетки;45.50;420;false",
                "Нурофен;сироп;ціна;200;false",
                "Валеріана;краплі;28.00;-5;false",
                "Інсулін;розчин;520.00;90;true"));
        Stats stats = Stats.compute(parsed.medicines());

        assertEquals(2, stats.count());
        assertEquals(282.75, stats.averagePrice(), 0.0001);
        assertEquals(90, stats.minDaysToExpire());
        assertEquals(1, stats.prescriptionCount());
    }
}
