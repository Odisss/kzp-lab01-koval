package ua.lpnu.kzp;

import java.util.List;

/**
 * Незмінний підсумок «термін і рецепт» для набору препаратів (допоміжний record варіанта 11).
 *
 * <p>Компілятор сам створює конструктор, методи доступу {@code minDaysToExpire()} і
 * {@code prescriptionCount()}, а також {@code equals}, {@code hashCode} і {@code toString}.
 *
 * @param minDaysToExpire   найменший термін придатності в днях; {@link #NO_DATA}, якщо
 *                          препаратів немає
 * @param prescriptionCount кількість рецептурних препаратів
 */
public record ExpiryPrescription(int minDaysToExpire, int prescriptionCount) {
    /** Значення {@code minDaysToExpire} для порожнього набору (як у лабораторній роботі № 1). */
    public static final int NO_DATA = -1;

    /**
     * Перевіряє допустимість компонентів.
     *
     * @throws IllegalArgumentException якщо термін менший за {@link #NO_DATA} або кількість
     *                                  рецептурних препаратів від'ємна
     */
    public ExpiryPrescription {
        if (minDaysToExpire < NO_DATA) {
            throw new IllegalArgumentException("термін придатності не може бути від'ємним");
        }
        if (prescriptionCount < 0) {
            throw new IllegalArgumentException("кількість рецептурних не може бути від'ємною");
        }
    }

    /**
     * Обчислює підсумок для списку препаратів.
     *
     * @param medicines коректні препарати
     * @return підсумок; для порожнього списку термін дорівнює {@link #NO_DATA}
     */
    public static ExpiryPrescription of(List<Medicine> medicines) {
        if (medicines.isEmpty()) {
            return new ExpiryPrescription(NO_DATA, 0);
        }
        int minDays = Integer.MAX_VALUE;
        int prescriptions = 0;
        for (Medicine medicine : medicines) {
            minDays = Math.min(minDays, medicine.daysToExpire());
            if (medicine.prescription()) {
                prescriptions++;
            }
        }
        return new ExpiryPrescription(minDays, prescriptions);
    }
}
