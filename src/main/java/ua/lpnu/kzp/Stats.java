package ua.lpnu.kzp;

import java.util.List;

/**
 * Чотири показники варіанта 11 «Аптека».
 *
 * @param count             кількість коректних записів
 * @param averagePrice      середня ціна (0, якщо записів немає)
 * @param minDaysToExpire   найменший термін придатності в днях (-1, якщо записів немає)
 * @param prescriptionCount кількість рецептурних препаратів
 */
public record Stats(int count, double averagePrice, int minDaysToExpire,
                    int prescriptionCount) {

    /**
     * Обчислює показники за коректними записами.
     *
     * @param medicines коректні записи
     * @return показники; для порожнього набору середня ціна 0, мінімальний термін -1
     */
    public static Stats compute(List<Medicine> medicines) {
        int count = medicines.size();
        if (count == 0) {
            // Окремо обробляємо порожній набір, щоб не ділити на нуль.
            return new Stats(0, 0.0, -1, 0);
        }
        double totalPrice = 0.0;
        int minDays = Integer.MAX_VALUE;
        int prescriptions = 0;
        for (Medicine medicine : medicines) {
            totalPrice += medicine.price();
            minDays = Math.min(minDays, medicine.daysToExpire());
            if (medicine.prescription()) {
                prescriptions++;
            }
        }
        // Приведення до double не дає цілочисловому діленню відкинути дробову частину.
        return new Stats(count, totalPrice / count, minDays, prescriptions);
    }
}
