package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/**
 * Документний CSV-парсер: машина станів над усім текстом файла.
 *
 * <p>На відміну від {@code split(",")}, парсер розрізняє кому й перенесення рядка
 * всередині лапок (це дані) і поза лапками (це межі поля й запису). Подвоєна лапка
 * всередині лапок означає одну лапку. Результат – список записів, а не фізичних рядків
 * файла: запис із multiline-полем займає кілька рядків файла.
 *
 * <p>Прийняті роздільники записів: {@code \n} і {@code \r\n}, тому файл, створений у
 * Windows, читається в Linux і macOS і навпаки. BOM на початку тексту пропускається.
 */
public final class CsvParser {
    private static final char SEPARATOR = CsvExporter.SEPARATOR;
    private static final char QUOTE = CsvExporter.QUOTE;
    private static final char BOM = '﻿';

    /* Забороняє створення екземплярів службового класу. */
    private CsvParser() {
    }

    /**
     * Розбирає один CSV-запис (наприклад, рядок заголовка).
     *
     * @param line текст одного запису
     * @return значення полів
     * @throws DataStorageException якщо текст містить не один запис або має хибний формат
     */
    public static List<String> parse(String line) throws DataStorageException {
        List<List<String>> records = parseDocument(line);
        if (records.size() != 1) {
            throw new DataStorageException(
                    "Очікується один CSV-запис, знайдено " + records.size());
        }
        return records.get(0);
    }

    /**
     * Розбирає весь CSV-документ. Порожній текст дає порожній список; завершальне
     * перенесення рядка не створює зайвого запису.
     *
     * @param text повний текст файла
     * @return незмінний список записів, кожен – незмінний список значень полів
     * @throws DataStorageException якщо {@code text} дорівнює {@code null}, лапки не
     *                              закрито або після закривальної лапки стоїть не кома й
     *                              не кінець запису, або лапка трапилась у полі без лапок
     */
    public static List<List<String>> parseDocument(String text) throws DataStorageException {
        if (text == null) {
            throw new DataStorageException("CSV-текст відсутній (null)");
        }
        int start = !text.isEmpty() && text.charAt(0) == BOM ? 1 : 0;
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;        // зараз усередині лапок
        boolean afterQuote = false;    // щойно закрито лапки, чекаємо кому або кінець запису
        boolean recordStarted = false; // у поточному записі вже є символи
        int recordNumber = 1;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == QUOTE) {
                    if (i + 1 < text.length() && text.charAt(i + 1) == QUOTE) {
                        value.append(QUOTE);
                        i++;
                    } else {
                        quoted = false;
                        afterQuote = true;
                    }
                } else {
                    value.append(c);
                }
                continue;
            }
            if (c == SEPARATOR) {
                fields.add(value.toString());
                value.setLength(0);
                afterQuote = false;
                recordStarted = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                fields.add(value.toString());
                records.add(List.copyOf(fields));
                fields.clear();
                value.setLength(0);
                afterQuote = false;
                recordStarted = false;
                recordNumber++;
            } else if (afterQuote) {
                throw new DataStorageException("Запис %d: після закривальної лапки очікується кома"
                        .formatted(recordNumber));
            } else if (c == QUOTE) {
                if (value.length() > 0) {
                    throw new DataStorageException("Запис %d: лапка всередині поля без лапок"
                            .formatted(recordNumber));
                }
                quoted = true;
                recordStarted = true;
            } else {
                value.append(c);
                recordStarted = true;
            }
        }
        if (quoted) {
            throw new DataStorageException("Запис %d: не закрито лапки".formatted(recordNumber));
        }
        if (recordStarted || afterQuote) {
            fields.add(value.toString());
            records.add(List.copyOf(fields));
        }
        return List.copyOf(records);
    }
}
