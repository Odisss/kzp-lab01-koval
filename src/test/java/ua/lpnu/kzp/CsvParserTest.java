package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {
    /* Кома в лапках – частина значення, а не роздільник (на відміну від split). */
    @Test
    void commaInsideQuotesIsData() throws DataStorageException {
        String line = "A-03,ART,\"Карти, що говорять\",1988";

        assertEquals(List.of("A-03", "ART", "Карти, що говорять", "1988"), CsvParser.parse(line));
        assertEquals(5, line.split(",").length);
    }

    /* Подвоєна лапка всередині лапок – одна лапка. */
    @Test
    void doubledQuoteBecomesOneQuote() throws DataStorageException {
        assertEquals(List.of("Вітаміни \"Сонечко\"", "x"),
                CsvParser.parse("\"Вітаміни \"\"Сонечко\"\"\",x"));
    }

    /* Перенесення рядка в лапках не завершує запис. */
    @Test
    void multilineFieldIsOneRecord() throws DataStorageException {
        List<List<String>> records = CsvParser.parseDocument("a,b\n\"рядок1\nрядок2\",c\n");

        assertEquals(List.of(List.of("a", "b"), List.of("рядок1\nрядок2", "c")), records);
    }

    /* \r\n і \n дають однаковий результат; BOM пропускається. */
    @Test
    void acceptsWindowsAndUnixLineEndings() throws DataStorageException {
        List<List<String>> unix = CsvParser.parseDocument("a,b\n1,2\n");
        List<List<String>> windows = CsvParser.parseDocument("﻿a,b\r\n1,2\r\n");

        assertEquals(unix, windows);
        assertEquals(2, unix.size());
    }

    /* Порожній текст – немає записів; порожні поля зберігаються. */
    @Test
    void emptyTextAndEmptyFields() throws DataStorageException {
        assertEquals(List.of(), CsvParser.parseDocument(""));
        assertEquals(List.of(List.of("", "", "")), CsvParser.parseDocument(",,"));
        assertEquals(List.of(List.of("")), CsvParser.parseDocument("\"\""));
    }

    /* Хибний формат – власний виняток. */
    @Test
    void malformedCsvFails() {
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"не закрито,1"));
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("\"a\"b,1"));
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument("ab\"c,1"));
        assertThrows(DataStorageException.class, () -> CsvParser.parseDocument(null));
        assertThrows(DataStorageException.class, () -> CsvParser.parse("a\nb"));
    }
}
