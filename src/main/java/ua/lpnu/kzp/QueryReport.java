package ua.lpnu.kzp;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Форматує результати {@link MedicineQueries} для консолі (опція {@code --queries}).
 *
 * <p>Обчислення виконуються в {@link MedicineQueries}; тут лише текст. Числа форматуються
 * з {@link Locale#ROOT}, тому десятковий роздільник не залежить від ОС.
 */
public final class QueryReport {
    /* Забороняє створення екземплярів службового класу. */
    private QueryReport() {
    }

    /**
     * Будує розділ із результатами п'яти запитів.
     *
     * @param medicines коректні препарати
     * @return текст розділу з платформонезалежними переносами рядків
     */
    public static String format(List<Medicine> medicines) {
        List<String> expiring = MedicineQueries.names(MedicineQueries.expiringSoon(medicines));
        String byForm = MedicineQueries.countByForm(medicines).entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
        DoubleSummaryStatistics prices = MedicineQueries.priceStatistics(medicines);

        StringBuilder text = new StringBuilder();
        text.append(String.format(Locale.ROOT, "Потокові запити:%n"));
        text.append(String.format(Locale.ROOT, "  Придатність до %d днів: %s%n",
                MedicineQueries.EXPIRY_THRESHOLD_DAYS, expiring));
        text.append(String.format(Locale.ROOT, "  Назви: %s%n", MedicineQueries.names(medicines)));
        text.append(String.format(Locale.ROOT, "  Кількість за формою: %s%n", byForm));
        text.append(formatPrices(prices));
        text.append(String.format(Locale.ROOT, "  Топ-%d за найближчою придатністю: %s%n",
                MedicineQueries.TOP_N,
                MedicineQueries.nearestExpiry(medicines, MedicineQueries.TOP_N)));
        return text.toString();
    }

    /* Порожня статистика має нескінченні min/max, тому друкуємо прочерк. */
    private static String formatPrices(DoubleSummaryStatistics prices) {
        if (prices.getCount() == 0) {
            return String.format(Locale.ROOT, "  Ціна: кількість=0, min=—, max=—, сума=0.00, середня=—%n");
        }
        return String.format(Locale.ROOT,
                "  Ціна: кількість=%d, min=%.2f, max=%.2f, сума=%.2f, середня=%.2f%n",
                prices.getCount(), prices.getMin(), prices.getMax(),
                prices.getSum(), prices.getAverage());
    }
}
