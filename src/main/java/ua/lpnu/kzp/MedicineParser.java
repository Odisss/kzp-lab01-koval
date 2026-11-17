package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;

/**
 * Перетворює рядки вхідного файла на список {@link Medicine}.
 *
 * <p>Розбір одного рядка й правила коректності тепер належать класу {@link Medicine}
 * ({@link Medicine#fromCsv(String)} і конструктор). Тут лишився тільки цикл читання: він
 * перехоплює помилку одного рядка й додає до неї номер рядка.
 */
public final class MedicineParser {
    /* Забороняє створення екземплярів службового класу. */
    private MedicineParser() {
    }

    /**
     * Розбирає всі рядки; хибні рядки не зупиняють обробку, а потрапляють у список помилок.
     *
     * @param lines рядки вхідного файла (нумерація починається з 1)
     * @return коректні записи та повідомлення про пропущені рядки
     */
    public static ParseResult parseAll(List<String> lines) {
        List<Medicine> medicines = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            int number = index + 1;
            String line = lines.get(index);
            // BOM на початку UTF-8 файла (його додає, наприклад, Блокнот Windows) не є даними.
            if (index == 0 && line.startsWith("﻿")) {
                line = line.substring(1);
            }
            try {
                // Запис потрапляє до списку лише після успішної перевірки.
                medicines.add(Medicine.fromCsv(line));
            } catch (IllegalArgumentException exception) {
                errors.add("Рядок %d: %s".formatted(number, exception.getMessage()));
            }
        }
        return new ParseResult(medicines, errors);
    }
}
