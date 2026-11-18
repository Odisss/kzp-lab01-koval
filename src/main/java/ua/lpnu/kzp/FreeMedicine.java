package ua.lpnu.kzp;

/**
 * Безрецептурний препарат вільного продажу.
 *
 * <p>Додаткових полів не має: усі інваріанти спільні й перевіряються в {@link Medicine}.
 * Відрізняється від {@link PrescriptionMedicine} категорією та правилом оцінки доступності.
 */
public final class FreeMedicine extends Medicine {

    /**
     * Створює безрецептурний препарат.
     *
     * @param name         назва препарату, не порожня
     * @param form         форма випуску, не порожня
     * @param price        ціна в гривнях, не менша за нуль
     * @param daysToExpire кількість днів до закінчення терміну придатності, не менша за нуль
     * @throws NullPointerException     якщо {@code name} або {@code form} дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено спільний інваріант
     */
    public FreeMedicine(String name, String form, double price, int daysToExpire) {
        super(name, form, price, daysToExpire);
    }

    /**
     * Повертає категорію {@link MedicineKind#FREE}.
     *
     * @return {@link MedicineKind#FREE}
     */
    @Override
    public MedicineKind kind() {
        return MedicineKind.FREE;
    }

    /**
     * Обчислює оцінку доступності: препарат купують без рецепта, тому оцінка дорівнює
     * ціновій складовій без знижувального коефіцієнта.
     *
     * @return оцінка в межах {@code [0; MAX_SCORE]}
     */
    @Override
    public double availabilityScore() {
        return priceAvailability();
    }
}
