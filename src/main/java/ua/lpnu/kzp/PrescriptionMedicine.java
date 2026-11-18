package ua.lpnu.kzp;

/**
 * Рецептурний препарат: відпускається лише за рецептом лікаря.
 *
 * <p>Власне правило підтипу: ціна має бути додатною, бо рецептурний препарат у звіті
 * аптеки не може бути безкоштовним зразком. Спільні інваріанти перевіряє {@link Medicine}.
 */
public final class PrescriptionMedicine extends Medicine {
    /** Частка цінової оцінки, що залишається через потребу в рецепті. */
    static final double PRESCRIPTION_FACTOR = 0.5;

    /**
     * Створює рецептурний препарат.
     *
     * @param name         назва препарату, не порожня
     * @param form         форма випуску, не порожня
     * @param price        ціна в гривнях, більша за нуль
     * @param daysToExpire кількість днів до закінчення терміну придатності, не менша за нуль
     * @throws NullPointerException     якщо {@code name} або {@code form} дорівнює {@code null}
     * @throws IllegalArgumentException якщо порушено спільний інваріант або ціна дорівнює 0
     */
    public PrescriptionMedicine(String name, String form, double price, int daysToExpire) {
        // Спочатку базовий клас перевіряє спільний стан, потім підтип – власне правило.
        super(name, form, price, daysToExpire);
        if (price == 0.0) {
            throw new IllegalArgumentException("рецептурний препарат має мати додатну ціну");
        }
    }

    /**
     * Повертає категорію {@link MedicineKind#PRESCRIPTION}.
     *
     * @return {@link MedicineKind#PRESCRIPTION}
     */
    @Override
    public MedicineKind kind() {
        return MedicineKind.PRESCRIPTION;
    }

    /**
     * Обчислює оцінку доступності: потреба в рецепті вдвічі знижує цінову оцінку.
     *
     * @return оцінка в межах {@code [0; MAX_SCORE / 2]}
     */
    @Override
    public double availabilityScore() {
        return priceAvailability() * PRESCRIPTION_FACTOR;
    }
}
