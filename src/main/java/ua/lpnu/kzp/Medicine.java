package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Препарат аптеки – клас-сутність варіанта 11.
 *
 * <p>Об'єкт незмінний: усі поля {@code private final}, сеттерів немає. Конструктор перевіряє
 * інваріанти, тому некоректний препарат створити неможливо:
 * <ul>
 *   <li>назва й форма випуску не {@code null} і не порожні;</li>
 *   <li>ціна скінченна й не менша за нуль;</li>
 *   <li>термін придатності не менший за нуль (0 – термін спливає сьогодні).</li>
 * </ul>
 *
 * <p>Методи читання названо так само, як компоненти record із лабораторної роботи № 1
 * ({@code name()}, {@code price()} …), тому код і тести попередньої роботи не змінюються.
 */
public final class Medicine {
    /** Кількість полів у CSV-рядку. */
    static final int FIELD_COUNT = 5;

    // Явні шаблони відсікають значення, які Double.parseDouble приймає «з сюрпризами»
    // (NaN, Infinity, 1e3, 5d), а також кому замість крапки.
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    private final String name;
    private final String form;
    private final double price;
    private final int daysToExpire;
    private final boolean prescription;

    /**
     * Створює препарат після перевірки всіх інваріантів.
     *
     * @param name         назва препарату, не порожня
     * @param form         форма випуску (таблетки, сироп тощо), не порожня
     * @param price        ціна в гривнях, скінченна й не менша за нуль
     * @param daysToExpire кількість днів до закінчення терміну придатності, не менша за нуль
     * @param prescription {@code true}, якщо препарат відпускається за рецептом
     * @throws NullPointerException     якщо {@code name} або {@code form} дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено будь-який інший інваріант
     */
    public Medicine(String name, String form, double price, int daysToExpire,
                    boolean prescription) {
        // Перевірки виконуються до присвоєння, тому некоректний об'єкт не з'являється.
        Objects.requireNonNull(name, "назва препарату не може бути null");
        Objects.requireNonNull(form, "форма випуску не може бути null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("порожня назва препарату");
        }
        if (form.isBlank()) {
            throw new IllegalArgumentException("порожня форма випуску");
        }
        if (!Double.isFinite(price)) {
            throw new IllegalArgumentException("ціна має бути скінченним числом");
        }
        if (price < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "від'ємна ціна: %.2f", price));
        }
        if (daysToExpire < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "від'ємний термін придатності: %d", daysToExpire));
        }
        this.name = name;
        this.form = form;
        this.price = price;
        this.daysToExpire = daysToExpire;
        this.prescription = prescription;
    }

    /**
     * Створює препарат з одного CSV-рядка {@code name;form;price;daysToExpire;prescription}.
     *
     * <p>Фабрика відповідає за структуру рядка й типи полів, а конструктор – за інваріанти,
     * тому правила не дублюються і обійти перевірку іншим способом створення неможливо.
     *
     * @param line рядок файла без символу кінця рядка
     * @return коректний препарат
     * @throws NullPointerException     якщо {@code line} дорівнює {@code null}
     * @throws IllegalArgumentException якщо рядок порожній, має не 5 полів, містить
     *                                  нечислове значення або порушує інваріант; текст
     *                                  винятку пояснює причину
     */
    public static Medicine fromCsv(String line) {
        Objects.requireNonNull(line, "рядок не може бути null");
        if (line.isBlank()) {
            throw new IllegalArgumentException("порожній рядок");
        }
        // Другий аргумент -1 зберігає порожнє останнє поле.
        String[] fields = line.split(";", -1);
        if (fields.length != FIELD_COUNT) {
            throw new IllegalArgumentException(
                    "очікується %d полів, знайдено %d".formatted(FIELD_COUNT, fields.length));
        }
        String priceText = fields[2].trim();
        if (!DECIMAL.matcher(priceText).matches()) {
            throw new IllegalArgumentException("ціна не є числом: '%s'".formatted(priceText));
        }
        String daysText = fields[3].trim();
        if (!INTEGER.matcher(daysText).matches()) {
            throw new IllegalArgumentException(
                    "термін придатності не є цілим числом: '%s'".formatted(daysText));
        }
        String prescriptionText = fields[4].trim();
        boolean prescription = parseBoolean(prescriptionText);
        try {
            return new Medicine(fields[0].trim(), fields[1].trim(),
                    Double.parseDouble(priceText), Integer.parseInt(daysText), prescription);
        } catch (NumberFormatException exception) {
            // Сюди потрапляє, наприклад, ціле число, що виходить за межі int.
            throw new IllegalArgumentException("число поза допустимим діапазоном", exception);
        }
    }

    /**
     * Перетворює текст на логічне значення, дозволяючи лише {@code true} і {@code false}.
     *
     * <p>{@link Boolean#parseBoolean(String)} не підходить: будь-який інший текст він мовчки
     * вважає {@code false}.
     *
     * @param text текст поля
     * @return логічне значення
     * @throws IllegalArgumentException якщо текст не є true/false
     */
    private static boolean parseBoolean(String text) {
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalArgumentException(
                "ознака рецепта має бути true або false: '%s'".formatted(text));
    }

    /**
     * Повертає назву препарату.
     *
     * @return непорожня назва
     */
    public String name() {
        return name;
    }

    /**
     * Повертає форму випуску.
     *
     * @return непорожня форма випуску
     */
    public String form() {
        return form;
    }

    /**
     * Повертає ціну.
     *
     * @return ціна в гривнях, не менша за нуль
     */
    public double price() {
        return price;
    }

    /**
     * Повертає кількість днів до закінчення терміну придатності.
     *
     * @return кількість днів, не менша за нуль
     */
    public int daysToExpire() {
        return daysToExpire;
    }

    /**
     * Повідомляє, чи відпускається препарат за рецептом.
     *
     * @return {@code true} для рецептурного препарату
     */
    public boolean prescription() {
        return prescription;
    }

    /**
     * Порівнює препарати за значеннями всіх полів.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо всі поля збігаються
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Medicine that)) {
            return false;
        }
        return Double.compare(price, that.price) == 0
                && daysToExpire == that.daysToExpire
                && prescription == that.prescription
                && name.equals(that.name)
                && form.equals(that.form);
    }

    /**
     * Обчислює хеш-код, узгоджений з {@link #equals(Object)}.
     *
     * @return хеш-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, form, price, daysToExpire, prescription);
    }

    /**
     * Повертає текстове подання препарату, наприклад
     * {@code Інсулін (розчин): 520.00 грн, 90 дн., за рецептом}.
     *
     * <p>{@link Locale#ROOT} гарантує крапку як десятковий роздільник у будь-якій ОС. Це
     * подання для людини, а не формат CSV.
     *
     * @return текстове подання
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%s): %.2f грн, %d дн., %s",
                name, form, price, daysToExpire, prescription ? "за рецептом" : "без рецепта");
    }
}
