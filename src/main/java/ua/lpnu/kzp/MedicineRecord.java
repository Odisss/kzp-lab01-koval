package ua.lpnu.kzp;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Плоский запис препарату для CSV-експорту (варіант 11 «Аптека»).
 *
 * <p>Поля й типи за варіантом: {@code name} – String, {@code form} – String,
 * {@code price} – double, {@code expiresOn} – {@link LocalDate},
 * {@code prescription} – enum {@link MedicineKind}. Кожне поле позначене
 * {@link CsvColumn}; {@link CsvExporter} упорядковує стовпці за Java-іменами полів,
 * тому фактичний заголовок: {@code придатність,форма,назва,рецепт,ціна}.
 *
 * <p>Ключ відновлення (round-trip) – назва + форма ({@link #key()}). Запис незмінний,
 * {@link #equals(Object)} і {@link #hashCode()} узгоджені й враховують усі поля.
 *
 * <p>Зв'язок із моделлю попередніх робіт: {@link #from(Medicine, LocalDate)} переводить
 * «днів до кінця придатності» в календарну дату, {@link #toMedicine(LocalDate)} – назад.
 */
public final class MedicineRecord {
    /** Кількість CSV-полів запису. */
    public static final int FIELD_COUNT = 5;

    @CsvColumn("назва")
    private final String name;

    @CsvColumn("форма")
    private final String form;

    @CsvColumn("ціна")
    private final double price;

    @CsvColumn("придатність")
    private final LocalDate expiresOn;

    @CsvColumn("рецепт")
    private final MedicineKind prescription;

    /**
     * Створює запис після перевірки інваріантів моделі.
     *
     * @param name         назва, не порожня
     * @param form         форма випуску, не порожня
     * @param price        ціна, скінченна й не менша за нуль; для рецептурного – більша за 0
     * @param expiresOn    дата закінчення придатності, не {@code null}
     * @param prescription категорія відпуску, не {@code null}
     * @throws NullPointerException     якщо текстове поле, дата або категорія – {@code null}
     * @throws IllegalArgumentException якщо порушено інший інваріант
     */
    public MedicineRecord(String name, String form, double price, LocalDate expiresOn,
                          MedicineKind prescription) {
        this.name = requireText(name, "назва");
        this.form = requireText(form, "форма");
        this.price = requirePrice(price, prescription);
        this.expiresOn = Objects.requireNonNull(expiresOn, "дата придатності не може бути null");
        this.prescription = Objects.requireNonNull(prescription, "категорія не може бути null");
    }

    /* Перевіряє непорожній текст. */
    private static String requireText(String value, String what) {
        Objects.requireNonNull(value, what + " не може бути null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("порожнє поле: " + what);
        }
        return value;
    }

    /* Перевіряє ціну з урахуванням категорії. */
    private static double requirePrice(double price, MedicineKind prescription) {
        if (!Double.isFinite(price)) {
            throw new IllegalArgumentException("ціна має бути скінченним числом");
        }
        if (price < 0) {
            throw new IllegalArgumentException(
                    String.format(Locale.ROOT, "від'ємна ціна: %.2f", price));
        }
        if (price == 0.0 && prescription == MedicineKind.PRESCRIPTION) {
            throw new IllegalArgumentException("рецептурний препарат має мати додатну ціну");
        }
        return price;
    }

    /**
     * Створює запис із препарату моделі: дата придатності = {@code today} + днів до кінця.
     *
     * @param medicine препарат
     * @param today    дата, від якої відлічуються дні
     * @return плоский запис
     */
    public static MedicineRecord from(Medicine medicine, LocalDate today) {
        Objects.requireNonNull(medicine, "препарат не може бути null");
        Objects.requireNonNull(today, "дата не може бути null");
        return new MedicineRecord(medicine.name(), medicine.form(), medicine.price(),
                today.plusDays(medicine.daysToExpire()), medicine.kind());
    }

    /**
     * Перетворює запис назад на препарат моделі потрібного підтипу.
     *
     * @param today дата, від якої відлічуються дні
     * @return {@link PrescriptionMedicine} або {@link FreeMedicine}
     * @throws IllegalArgumentException якщо на дату {@code today} термін уже минув
     */
    public Medicine toMedicine(LocalDate today) {
        Objects.requireNonNull(today, "дата не може бути null");
        long days = ChronoUnit.DAYS.between(today, expiresOn);
        if (days < 0 || days > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("термін придатності минув: " + expiresOn);
        }
        return Medicine.of(name, form, price, (int) days, prescription.requiresPrescription());
    }

    /**
     * Відновлює запис зі значень одного CSV-запису в порядку стовпців
     * {@code придатність,форма,назва,рецепт,ціна}.
     *
     * @param fields значення полів
     * @return коректний запис
     * @throws DataStorageException якщо кількість полів не 5 (без cause) або значення
     *                              хибне: дата, число, категорія чи інваріант моделі (з
     *                              cause – початковим винятком розбору)
     */
    public static MedicineRecord fromFields(List<String> fields) throws DataStorageException {
        Objects.requireNonNull(fields, "список полів не може бути null");
        if (fields.size() != FIELD_COUNT) {
            throw new DataStorageException("очікується %d полів, знайдено %d"
                    .formatted(FIELD_COUNT, fields.size()));
        }
        LocalDate expiresOn;
        try {
            expiresOn = LocalDate.parse(fields.get(0).trim());
        } catch (DateTimeParseException exception) {
            throw new DataStorageException(
                    "поле 1 (придатність): хибна дата '%s'".formatted(fields.get(0)), exception);
        }
        double price;
        try {
            price = Double.parseDouble(fields.get(4).trim());
        } catch (NumberFormatException exception) {
            throw new DataStorageException(
                    "поле 5 (ціна): не є числом '%s'".formatted(fields.get(4)), exception);
        }
        MedicineKind kind;
        try {
            kind = MedicineKind.valueOf(fields.get(3).trim());
        } catch (IllegalArgumentException exception) {
            throw new DataStorageException(
                    "поле 4 (рецепт): невідома категорія '%s'".formatted(fields.get(3)),
                    exception);
        }
        try {
            return new MedicineRecord(fields.get(2), fields.get(1), price, expiresOn, kind);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new DataStorageException("некоректний запис: " + exception.getMessage(),
                    exception);
        }
    }

    /**
     * Повертає ключ відновлення за варіантом: назва + форма.
     *
     * @return ключ у вигляді {@code назва|форма}
     */
    public String key() {
        return name + "|" + form;
    }

    /**
     * Повертає назву.
     *
     * @return назва
     */
    public String name() {
        return name;
    }

    /**
     * Повертає форму випуску.
     *
     * @return форма випуску
     */
    public String form() {
        return form;
    }

    /**
     * Повертає ціну.
     *
     * @return ціна в гривнях
     */
    public double price() {
        return price;
    }

    /**
     * Повертає дату закінчення придатності.
     *
     * @return дата
     */
    public LocalDate expiresOn() {
        return expiresOn;
    }

    /**
     * Повертає категорію відпуску.
     *
     * @return категорія
     */
    public MedicineKind prescription() {
        return prescription;
    }

    /**
     * Порівнює записи за всіма полями.
     *
     * @param other інший об'єкт
     * @return {@code true}, якщо всі поля рівні
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MedicineRecord that)) {
            return false;
        }
        return Double.compare(price, that.price) == 0
                && name.equals(that.name)
                && form.equals(that.form)
                && expiresOn.equals(that.expiresOn)
                && prescription == that.prescription;
    }

    /**
     * Обчислює хеш-код, узгоджений з {@link #equals(Object)}.
     *
     * @return хеш-код
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, form, price, expiresOn, prescription);
    }

    /**
     * Повертає текстове подання, наприклад
     * {@code Інсулін (розчин): 520.00 грн, до 2027-02-16, PRESCRIPTION}.
     *
     * @return текстове подання
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%s): %.2f грн, до %s, %s",
                name, form, price, expiresOn, prescription);
    }
}
