package ua.lpnu.kzp;

/**
 * Один коректний запис про препарат аптеки.
 *
 * <p>У лабораторній роботі № 2 цей тип можна розвинути у повноцінний клас.
 *
 * @param name         назва препарату
 * @param form         форма випуску (таблетки, сироп тощо)
 * @param price        ціна в гривнях, не менша за нуль
 * @param daysToExpire кількість днів до закінчення терміну придатності, не менша за нуль
 * @param prescription {@code true}, якщо препарат відпускається за рецептом
 */
public record Medicine(String name, String form, double price, int daysToExpire,
                       boolean prescription) {
}
