package ua.lpnu.kzp;

import java.util.List;
import java.util.Objects;

/**
 * Чотири показники варіанта 11 «Аптека».
 *
 * @param count              кількість коректних записів
 * @param averagePrice       середня ціна (0, якщо записів немає)
 * @param expiryPrescription найменший термін придатності та кількість рецептурних препаратів
 */
public record Stats(int count, double averagePrice, ExpiryPrescription expiryPrescription) {

    /**
     * Перевіряє допустимість показників.
     *
     * @throws NullPointerException     якщо {@code expiryPrescription} дорівнює {@code null}
     * @throws IllegalArgumentException якщо кількість від'ємна або середнє некоректне
     */
    public Stats {
        Objects.requireNonNull(expiryPrescription, "expiryPrescription");
        if (count < 0 || averagePrice < 0 || !Double.isFinite(averagePrice)) {
            throw new IllegalArgumentException("показники не можуть бути від'ємними");
        }
    }

    /**
     * Створює показники з чотирьох чисел (форма з лабораторної роботи № 1).
     *
     * @param count             кількість коректних записів
     * @param averagePrice      середня ціна
     * @param minDaysToExpire   найменший термін придатності або -1
     * @param prescriptionCount кількість рецептурних препаратів
     */
    public Stats(int count, double averagePrice, int minDaysToExpire, int prescriptionCount) {
        this(count, averagePrice, new ExpiryPrescription(minDaysToExpire, prescriptionCount));
    }

    /**
     * Обчислює показники за коректними записами.
     *
     * @param medicines коректні записи
     * @return показники; для порожнього набору середня ціна 0, мінімальний термін -1
     */
    public static Stats compute(List<Medicine> medicines) {
        int count = medicines.size();
        double totalPrice = 0.0;
        for (Medicine medicine : medicines) {
            totalPrice += medicine.price();
        }
        // Окремо обробляємо порожній набір, щоб не ділити на нуль; ділення виконується в double.
        double average = count == 0 ? 0.0 : totalPrice / count;
        return new Stats(count, average, ExpiryPrescription.of(medicines));
    }

    /**
     * Повертає найменший термін придатності.
     *
     * @return кількість днів або -1, якщо записів немає
     */
    public int minDaysToExpire() {
        return expiryPrescription.minDaysToExpire();
    }

    /**
     * Повертає кількість рецептурних препаратів.
     *
     * @return кількість препаратів із {@code prescription = true}
     */
    public int prescriptionCount() {
        return expiryPrescription.prescriptionCount();
    }
}
