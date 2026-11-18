package ua.lpnu.kzp;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Чисті потокові запити варіанта 11 «Аптека» до колекції препаратів.
 *
 * <p>Кожен метод створює новий потік із вхідного списку, не змінює його і повертає один
 * результат. Форматування для консолі винесено в {@link QueryReport}. Підтипи препаратів
 * обробляються через спільний тип {@link Medicine} без перевірок за конкретним класом.
 */
public final class MedicineQueries {
    /** Поріг відбору «придатність до 30 днів» (межа входить у результат). */
    public static final int EXPIRY_THRESHOLD_DAYS = 30;

    /** Кількість записів у запиті top-N за домовленістю курсу. */
    public static final int TOP_N = 5;

    /**
     * Порядок top-N: спершу найближча придатність (за зростанням), за рівності — назва
     * за абеткою.
     */
    public static final Comparator<Medicine> BY_NEAREST_EXPIRY_THEN_NAME = Comparator
            .comparingInt(Medicine::daysToExpire)
            .thenComparing(Medicine::name);

    /* Забороняє створення екземплярів класу статичних запитів. */
    private MedicineQueries() {
    }

    /**
     * Відбирає препарати, термін придатності яких не перевищує {@code thresholdDays}.
     *
     * @param medicines     препарати
     * @param thresholdDays поріг у днях, не від'ємний; межа входить у результат
     * @return незмінний список у вхідному порядку
     * @throws IllegalArgumentException якщо поріг від'ємний
     */
    public static List<Medicine> expiringWithin(List<Medicine> medicines, int thresholdDays) {
        if (thresholdDays < 0) {
            throw new IllegalArgumentException("поріг не може бути від'ємним: " + thresholdDays);
        }
        return streamOf(medicines)
                .filter(medicine -> medicine.daysToExpire() <= thresholdDays)
                .toList();
    }

    /**
     * Відбирає препарати з терміном придатності до {@link #EXPIRY_THRESHOLD_DAYS} днів.
     *
     * @param medicines препарати
     * @return незмінний список у вхідному порядку
     */
    public static List<Medicine> expiringSoon(List<Medicine> medicines) {
        return expiringWithin(medicines, EXPIRY_THRESHOLD_DAYS);
    }

    /**
     * Повертає назви препаратів у вхідному порядку (дублікати зберігаються).
     *
     * @param medicines препарати
     * @return незмінний список назв
     */
    public static List<String> names(List<Medicine> medicines) {
        return streamOf(medicines)
                .map(Medicine::name)
                .toList();
    }

    /**
     * Рахує препарати за формою випуску.
     *
     * <p>Використовується {@link TreeMap}, тому порядок форм у звіті стабільний (за абеткою).
     *
     * @param medicines препарати
     * @return карта «форма → кількість»; для порожнього входу порожня
     */
    public static Map<String, Long> countByForm(List<Medicine> medicines) {
        return streamOf(medicines)
                .collect(Collectors.groupingBy(
                        Medicine::form,
                        TreeMap::new,
                        Collectors.counting()));
    }

    /**
     * Обчислює зведену статистику ціни.
     *
     * <p>Для порожнього входу кількість і сума дорівнюють 0, мінімум —
     * {@code Double.POSITIVE_INFINITY}, максимум — {@code Double.NEGATIVE_INFINITY};
     * звіт обробляє цей випадок окремо.
     *
     * @param medicines препарати
     * @return новий об'єкт статистики
     */
    public static DoubleSummaryStatistics priceStatistics(List<Medicine> medicines) {
        return streamOf(medicines)
                .collect(Collectors.summarizingDouble(Medicine::price));
    }

    /**
     * Повертає назви перших {@code limit} препаратів за порядком
     * {@link #BY_NEAREST_EXPIRY_THEN_NAME}.
     *
     * @param medicines препарати
     * @param limit     скільки записів залишити; 0 дає порожній список, більше за розмір —
     *                  усі записи
     * @return незмінний список назв
     * @throws IllegalArgumentException якщо {@code limit} від'ємний
     */
    public static List<String> nearestExpiry(List<Medicine> medicines, int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("ліміт не може бути від'ємним: " + limit);
        }
        // Спершу повне впорядкування, потім limit: так отримуємо найкращі записи всієї колекції.
        return streamOf(medicines)
                .sorted(BY_NEAREST_EXPIRY_THEN_NAME)
                .limit(limit)
                .map(Medicine::name)
                .toList();
    }

    /**
     * Шукає перший препарат із точно такою назвою.
     *
     * @param medicines препарати
     * @param name      назва для пошуку
     * @return перший знайдений препарат у вхідному порядку або {@link Optional#empty()}
     * @throws NullPointerException якщо {@code name} дорівнює {@code null}
     */
    public static Optional<Medicine> findByName(List<Medicine> medicines, String name) {
        Objects.requireNonNull(name, "назва не може бути null");
        return streamOf(medicines)
                .filter(medicine -> medicine.name().equals(name))
                .findFirst();
    }

    /* Перевіряє список і повертає новий потік для одного запиту. */
    private static Stream<Medicine> streamOf(List<Medicine> medicines) {
        return Objects.requireNonNull(medicines, "список не може бути null").stream();
    }
}
