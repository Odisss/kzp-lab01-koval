package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Читає текстові рядки формату {@code name;form;price;daysToExpire;prescription},
 * перевіряє їх і перетворює на {@link Medicine}.
 */
public final class MedicineParser {
    /** Кількість полів у рядку. */
    static final int FIELD_COUNT = 5;

    // Явні шаблони відсікають значення, які Double.parseDouble приймає «з сюрпризами»
    // (NaN, Infinity, 1e3, 5d).
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

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
            String problem = tryParse(line, medicines);
            if (problem != null) {
                errors.add("Рядок %d: %s".formatted(number, problem));
            }
        }
        return new ParseResult(medicines, errors);
    }

    /**
     * Розбирає один рядок.
     *
     * @param line   рядок файла
     * @param target список, до якого додається коректний запис
     * @return {@code null}, якщо рядок коректний, інакше причина пропуску
     */
    private static String tryParse(String line, List<Medicine> target) {
        if (line.isBlank()) {
            return "порожній рядок";
        }
        // Другий аргумент -1 зберігає порожнє останнє поле.
        String[] fields = line.split(";", -1);
        if (fields.length != FIELD_COUNT) {
            return "очікується %d полів, знайдено %d".formatted(FIELD_COUNT, fields.length);
        }
        String name = fields[0].trim();
        String form = fields[1].trim();
        if (name.isEmpty()) {
            return "порожня назва препарату";
        }
        if (form.isEmpty()) {
            return "порожня форма випуску";
        }
        String priceText = fields[2].trim();
        if (!DECIMAL.matcher(priceText).matches()) {
            return "ціна не є числом: '%s'".formatted(priceText);
        }
        String daysText = fields[3].trim();
        if (!INTEGER.matcher(daysText).matches()) {
            return "термін придатності не є цілим числом: '%s'".formatted(daysText);
        }
        Optional<Boolean> prescription = parseBoolean(fields[4].trim());
        if (prescription.isEmpty()) {
            return "ознака рецепта має бути true або false: '%s'".formatted(fields[4].trim());
        }
        try {
            double price = Double.parseDouble(priceText);
            int days = Integer.parseInt(daysText);
            if (price < 0) {
                return "від'ємна ціна: %s".formatted(priceText);
            }
            if (days < 0) {
                return "від'ємний термін придатності: %s".formatted(daysText);
            }
            target.add(new Medicine(name, form, price, days, prescription.get()));
            return null;
        } catch (NumberFormatException exception) {
            // Сюди потрапляє, наприклад, ціле число, що виходить за межі int.
            return "число поза допустимим діапазоном";
        }
    }

    /**
     * Перетворює текст на логічне значення, дозволяючи лише {@code true} і {@code false}.
     *
     * <p>{@link Boolean#parseBoolean(String)} не підходить: будь-який інший текст він мовчки
     * вважає {@code false}.
     *
     * @param text текст поля
     * @return значення або порожній {@link Optional}, якщо текст не є true/false
     */
    private static Optional<Boolean> parseBoolean(String text) {
        if ("true".equalsIgnoreCase(text)) {
            return Optional.of(Boolean.TRUE);
        }
        if ("false".equalsIgnoreCase(text)) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }
}
