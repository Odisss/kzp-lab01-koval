package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MedicineParserTest {
    /* Допоміжний метод: розбирає один рядок. */
    private static ParseResult parse(String line) {
        return MedicineParser.parseAll(List.of(line));
    }

    /* Коректний рядок з українським текстом перетворюється на запис. */
    @Test
    void validLineIsParsed() {
        ParseResult result = parse("Парацетамол;таблетки;45.50;420;false");

        assertEquals(1, result.medicines().size());
        assertTrue(result.errors().isEmpty());
        Medicine medicine = result.medicines().get(0);
        assertEquals("Парацетамол", medicine.name());
        assertEquals("таблетки", medicine.form());
        assertEquals(45.5, medicine.price(), 0.0001);
        assertEquals(420, medicine.daysToExpire());
        assertFalse(medicine.prescription());
    }

    /* Порожній рядок пропускається з номером рядка. */
    @Test
    void blankLineIsReportedWithLineNumber() {
        ParseResult result = MedicineParser.parseAll(List.of("А;б;1;1;true", "   "));

        assertEquals(1, result.medicines().size());
        assertEquals(List.of("Рядок 2: порожній рядок"), result.errors());
    }

    /* Неправильна кількість полів: замало і забагато. */
    @Test
    void wrongFieldCountIsRejected() {
        assertEquals(1, parse("Аспірин;таблетки;41.00;250").errors().size());
        assertEquals(1, parse("Аспірин;таблетки;41.00;250;false;зайве").errors().size());
        assertTrue(parse("Аспірин;таблетки;41.00;250").medicines().isEmpty());
    }

    /* Порожнє останнє поле зберігається завдяки split(";", -1) і дає помилку, а не виняток. */
    @Test
    void emptyLastFieldIsRejected() {
        ParseResult result = parse("Аспірин;таблетки;41.00;250;");

        assertTrue(result.medicines().isEmpty());
        assertEquals(1, result.errors().size());
    }

    /* Порожні обов'язкові текстові поля. */
    @Test
    void emptyTextFieldsAreRejected() {
        assertEquals(1, parse(";таблетки;20.00;100;false").errors().size());
        assertEquals(1, parse("Аспірин;  ;20.00;100;false").errors().size());
    }

    /* Нечислові значення в числових полях, зокрема NaN та експоненційний запис. */
    @Test
    void nonNumericValuesAreRejected() {
        assertEquals(1, parse("Нурофен;сироп;ціна;200;false").errors().size());
        assertEquals(1, parse("Нурофен;сироп;45.5;багато;false").errors().size());
        assertEquals(1, parse("Нурофен;сироп;NaN;200;false").errors().size());
        assertEquals(1, parse("Нурофен;сироп;1e3;200;false").errors().size());
        assertEquals(1, parse("Нурофен;сироп;45,5;200;false").errors().size());
        assertEquals(1, parse("Нурофен;сироп;45.5;20.5;false").errors().size());
    }

    /* Від'ємні значення не мають сенсу ні для ціни, ні для терміну. */
    @Test
    void negativeValuesAreRejected() {
        assertEquals("Рядок 1: від'ємна ціна: -15.00",
                parse("Но-шпа;таблетки;-15.00;365;false").errors().get(0));
        assertEquals("Рядок 1: від'ємний термін придатності: -5",
                parse("Валеріана;краплі;28.00;-5;false").errors().get(0));
    }

    /* Нульові ціна й термін допустимі (термін закінчується сьогодні). */
    @Test
    void zeroValuesAreAccepted() {
        ParseResult result = parse("Проба;гель;0;0;false");

        assertEquals(1, result.medicines().size());
        assertTrue(result.errors().isEmpty());
    }

    /* Ціле число поза діапазоном int не валить програму. */
    @Test
    void integerOverflowIsRejected() {
        assertEquals(1, parse("А;б;1.00;99999999999;false").errors().size());
    }

    /* Ознака рецепта: лише true/false, будь-який регістр; «можливо» — помилка. */
    @Test
    void prescriptionMustBeBoolean() {
        assertTrue(parse("А;б;1;1;TRUE").medicines().get(0).prescription());
        assertFalse(parse("А;б;1;1;False").medicines().get(0).prescription());
        assertEquals(1, parse("А;б;1;1;можливо").errors().size());
        assertEquals(1, parse("А;б;1;1;yes").errors().size());
    }

    /* BOM на початку файла не псує перший рядок. */
    @Test
    void byteOrderMarkIsIgnored() {
        ParseResult result = parse("﻿Парацетамол;таблетки;45.50;420;false");

        assertEquals(1, result.medicines().size());
        assertEquals("Парацетамол", result.medicines().get(0).name());
    }

    /* Хибний рядок не впливає на решту: нумерація й обробка тривають. */
    @Test
    void invalidLineDoesNotStopProcessing() {
        ParseResult result = MedicineParser.parseAll(List.of(
                "А;б;1;1;true", "сміття", "В;г;2;2;false"));

        assertEquals(2, result.medicines().size());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).startsWith("Рядок 2:"));
    }
}
