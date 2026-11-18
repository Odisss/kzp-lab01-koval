package ua.lpnu.kzp;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Спільний абстрактний тип препарату аптеки (варіант 11).
 *
 * <p>Базовий клас зберігає спільний стан (назва, форма, ціна, термін придатності) і
 * перевіряє його інваріанти один раз для всіх підтипів:
 * <ul>
 *   <li>назва й форма випуску не {@code null} і не порожні;</li>
 *   <li>ціна скінченна й не менша за нуль;</li>
 *   <li>термін придатності не менший за нуль (0 – термін спливає сьогодні).</li>
 * </ul>
 *
 * <p>Відмінна поведінка реалізується в підтипах {@link PrescriptionMedicine} і
 * {@link FreeMedicine}: кожен повертає власну {@link #kind() категорію} та по-своєму
 * обчислює {@link #availabilityScore() оцінку доступності}. Код звіту працює лише з типом
 * {@code Medicine} і не перевіряє, який підтип отримано.
 *
 * <p>Методи читання названо так само, як компоненти record із лабораторної роботи № 1
 * ({@code name()}, {@code price()} …), тому код звіту попередніх робіт не змінюється.
 */
public abstract class Medicine {
    /** Кількість полів у CSV-рядку. */
    static final int FIELD_COUNT = 5;

    /** Найбільше значення оцінки доступності. */
    public static final double MAX_SCORE = 100.0;

    // Явні шаблони відсікають значення, які Double.parseDouble приймає «з сюрпризами»
    // (NaN, Infinity, 1e3, 5d), а також кому замість крапки.
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");

    /* Скільки гривень ціни знімають один бал доступності. */
    private static final double PRICE_PER_POINT = 10.0;

    private final String name;
    private final String form;
    private final double price;
    private final int daysToExpire;

    /**
     * Створює спільну частину препарату після перевірки всіх спільних інваріантів.
     *
     * <p>Перевірки виконуються в аргументах делегованого виклику {@code this(...)}, тобто
     * ще до створення об'єкта. Тому навіть у підкласі некоректний препарат не з'являється.
     *
     * @param name         назва препарату, не порожня
     * @param form         форма випуску (таблетки, сироп тощо), не порожня
     * @param price        ціна в гривнях, скінченна й не менша за нуль
     * @param daysToExpire кількість днів до закінчення терміну придатності, не менша за нуль
     * @throws NullPointerException     якщо {@code name} або {@code form} дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено будь-який інший інваріант
     */
    protected Medicine(String name, String form, double price, int daysToExpire) {
        this(validateName(name), validateForm(form), validatePrice(price),
                validateDays(daysToExpire), true);
    }

    /* Присвоює вже перевірений стан під час внутрішнього делегування. */
    private Medicine(String name, String form, double price, int daysToExpire,
                     boolean validated) {
        this.name = name;
        this.form = form;
        this.price = price;
        this.daysToExpire = daysToExpire;
    }

    /* Перевіряє назву препарату. */
    private static String validateName(String name) {
        Objects.requireNonNull(name, "назва препарату не може бути null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("порожня назва препарату");
        }
        return name;
    }

    /* Перевіряє форму випуску. */
    private static String validateForm(String form) {
        Objects.requireNonNull(form, "форма випуску не може бути null");
        if (form.isBlank()) {
            throw new IllegalArgumentException("порожня форма випуску");
        }
        return form;
    }

    /* Перевіряє ціну. */
    private static double validatePrice(double price) {
        if (!Double.isFinite(price)) {
            throw new IllegalArgumentException("ціна має бути скінченним числом");
        }
        if (price < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "від'ємна ціна: %.2f", price));
        }
        return price;
    }

    /* Перевіряє термін придатності. */
    private static int validateDays(int daysToExpire) {
        if (daysToExpire < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "від'ємний термін придатності: %d", daysToExpire));
        }
        return daysToExpire;
    }

    /**
     * Створює препарат потрібного підтипу за ознакою рецепта.
     *
     * <p>Це єдине місце, де вибирається конкретний клас; далі програма працює лише з
     * типом {@code Medicine}.
     *
     * @param name         назва препарату
     * @param form         форма випуску
     * @param price        ціна в гривнях
     * @param daysToExpire кількість днів до закінчення терміну придатності
     * @param prescription {@code true} для рецептурного препарату
     * @return {@link PrescriptionMedicine} або {@link FreeMedicine}
     * @throws NullPointerException     якщо {@code name} або {@code form} дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено інваріант базового типу або підтипу
     */
    public static Medicine of(String name, String form, double price, int daysToExpire,
                              boolean prescription) {
        return prescription
                ? new PrescriptionMedicine(name, form, price, daysToExpire)
                : new FreeMedicine(name, form, price, daysToExpire);
    }

    /**
     * Створює препарат з одного CSV-рядка {@code name;form;price;daysToExpire;prescription}.
     *
     * <p>Фабрика відповідає за структуру рядка й типи полів, а конструктори – за інваріанти,
     * тому правила не дублюються. Формат рядка той самий, що й у попередніх роботах.
     *
     * @param line рядок файла без символу кінця рядка
     * @return коректний препарат відповідного підтипу
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
        boolean prescription = parseBoolean(fields[4].trim());
        try {
            return of(fields[0].trim(), fields[1].trim(),
                    Double.parseDouble(priceText), Integer.parseInt(daysText), prescription);
        } catch (NumberFormatException exception) {
            // Сюди потрапляє, наприклад, ціле число, що виходить за межі int.
            throw new IllegalArgumentException("число поза допустимим діапазоном", exception);
        }
    }

    /**
     * Перетворює текст на логічне значення, дозволяючи лише {@code true} і {@code false}.
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
     * Повертає категорію препарату. Кожен підтип повертає власну константу.
     *
     * @return категорія препарату, не {@code null}
     */
    public abstract MedicineKind kind();

    /**
     * Обчислює оцінку доступності препарату для покупця за шкалою від 0 до
     * {@link #MAX_SCORE}. Це поліморфна операція варіанта 11: кожен підтип має власне
     * правило.
     *
     * @return оцінка доступності в межах {@code [0; MAX_SCORE]}
     */
    public abstract double availabilityScore();

    /**
     * Обчислює спільну для всіх підтипів складову оцінки: чим дорожчий препарат, тим
     * нижча оцінка (мінус 1 бал за кожні 10 грн, але не менше 0).
     *
     * @return цінова складова оцінки в межах {@code [0; MAX_SCORE]}
     */
    protected final double priceAvailability() {
        return Math.max(0.0, MAX_SCORE - price / PRICE_PER_POINT);
    }

    /**
     * Повертає назву препарату.
     *
     * @return непорожня назва
     */
    public final String name() {
        return name;
    }

    /**
     * Повертає форму випуску.
     *
     * @return непорожня форма випуску
     */
    public final String form() {
        return form;
    }

    /**
     * Повертає ціну.
     *
     * @return ціна в гривнях, не менша за нуль
     */
    public final double price() {
        return price;
    }

    /**
     * Повертає кількість днів до закінчення терміну придатності.
     *
     * @return кількість днів, не менша за нуль
     */
    public final int daysToExpire() {
        return daysToExpire;
    }

    /**
     * Повідомляє, чи відпускається препарат за рецептом. Значення бере з {@link #kind()},
     * тому окремого поля-прапорця більше немає.
     *
     * @return {@code true} для рецептурного препарату
     */
    public final boolean prescription() {
        return kind().requiresPrescription();
    }

    /**
     * Порівнює препарати: рівні лише об'єкти одного класу з однаковими назвою, формою,
     * ціною й терміном придатності.
     *
     * <p>Перевірка {@code getClass()} не дає вважати рівними рецептурний і безрецептурний
     * препарати з однаковими полями.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо клас і всі поля збігаються
     */
    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Medicine that = (Medicine) other;
        return Double.compare(price, that.price) == 0
                && daysToExpire == that.daysToExpire
                && name.equals(that.name)
                && form.equals(that.form);
    }

    /**
     * Обчислює хеш-код, узгоджений з {@link #equals(Object)}: враховує ті самі поля й клас.
     *
     * @return хеш-код
     */
    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), name, form, price, daysToExpire);
    }

    /**
     * Повертає текстове подання препарату, наприклад
     * {@code Інсулін (розчин): 520.00 грн, 90 дн., за рецептом}.
     *
     * <p>Формат не змінився порівняно з лабораторною роботою № 2. {@link Locale#ROOT}
     * гарантує крапку як десятковий роздільник у будь-якій ОС.
     *
     * @return текстове подання
     */
    @Override
    public final String toString() {
        return String.format(Locale.ROOT, "%s (%s): %.2f грн, %d дн., %s",
                name, form, price, daysToExpire, kind().saleLabel());
    }
}
