package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MedicineQueriesTest {
    /*
     * Контрольний набір (7 записів): межа 30 днів, значення до й після порога,
     * дубльована назва «Цитрамон», однаковий термін 10 днів (перевіряє другий критерій),
     * шостий і сьомий записи — для перевірки усікання top-5.
     */
    private static final Medicine CITRAMON_FIRST = Medicine.of("Цитрамон", "таблетки", 32.20, 10, false);
    private static final List<Medicine> ITEMS = List.of(
            Medicine.of("Парацетамол", "таблетки", 45.50, 420, false),
            CITRAMON_FIRST,
            Medicine.of("Амоксицилін", "капсули", 132.00, 30, true),
            Medicine.of("Інсулін", "розчин", 520.00, 31, true),
            Medicine.of("Аскорбінка", "таблетки", 12.00, 10, false),
            Medicine.of("Цитрамон", "капсули", 40.00, 200, false),
            Medicine.of("Йод", "розчин", 18.30, 29, false));

    /* filter: межа 30 входить, 31 — ні; порядок вхідний. */
    @Test
    void filtersExpiringWithinThresholdIncludingBoundary() {
        assertEquals(List.of("Цитрамон", "Амоксицилін", "Аскорбінка", "Йод"),
                MedicineQueries.names(MedicineQueries.expiringSoon(ITEMS)));
    }

    /* filter: від'ємний поріг — помилка вхідних даних. */
    @Test
    void rejectsNegativeThreshold() {
        assertThrows(IllegalArgumentException.class,
                () -> MedicineQueries.expiringWithin(ITEMS, -1));
    }

    /* map: назви у вхідному порядку, дублікат зберігається. */
    @Test
    void mapsNamesKeepingDuplicates() {
        assertEquals(List.of("Парацетамол", "Цитрамон", "Амоксицилін", "Інсулін",
                        "Аскорбінка", "Цитрамон", "Йод"),
                MedicineQueries.names(ITEMS));
    }

    /* groupingBy: кількість за формою, ключі за абеткою. */
    @Test
    void countsByFormInStableOrder() {
        Map<String, Long> byForm = MedicineQueries.countByForm(ITEMS);
        assertEquals(Map.of("таблетки", 3L, "капсули", 2L, "розчин", 2L), byForm);
        assertEquals(List.of("капсули", "розчин", "таблетки"), List.copyOf(byForm.keySet()));
    }

    /* Статистика ціни. */
    @Test
    void summarizesPrices() {
        DoubleSummaryStatistics stats = MedicineQueries.priceStatistics(ITEMS);
        assertEquals(7, stats.getCount());
        assertEquals(12.00, stats.getMin(), 1e-9);
        assertEquals(520.00, stats.getMax(), 1e-9);
        assertEquals(800.00, stats.getSum(), 1e-9);
    }

    /* Статистика збігається з попередньою цикловою реалізацією. */
    @Test
    void priceSumMatchesLegacyLoop() {
        double legacyTotal = 0.0;
        for (Medicine medicine : ITEMS) {
            legacyTotal += medicine.price();
        }
        assertEquals(legacyTotal, MedicineQueries.priceStatistics(ITEMS).getSum(), 1e-9);
    }

    /* top-5: термін за зростанням, за рівності (10 днів) — назва; шостий запис відсікається. */
    @Test
    void ordersTopFiveByNearestExpiryThenName() {
        assertEquals(List.of("Аскорбінка", "Цитрамон", "Йод", "Амоксицилін", "Інсулін"),
                MedicineQueries.nearestExpiry(ITEMS, MedicineQueries.TOP_N));
    }

    /* top-N: N = 0, N більший за розмір, від'ємний N. */
    @Test
    void handlesLimitEdgeCases() {
        assertEquals(List.of(), MedicineQueries.nearestExpiry(ITEMS, 0));
        assertEquals(ITEMS.size(), MedicineQueries.nearestExpiry(ITEMS, 100).size());
        assertThrows(IllegalArgumentException.class,
                () -> MedicineQueries.nearestExpiry(ITEMS, -1));
    }

    /* Optional: за дубльованої назви повертається перший запис. */
    @Test
    void findsFirstRecordForDuplicatedName() {
        Optional<Medicine> found = MedicineQueries.findByName(ITEMS, "Цитрамон");
        assertTrue(found.isPresent());
        assertSame(CITRAMON_FIRST, found.get());
    }

    /* Optional: відсутня назва дає порожній результат, а не null. */
    @Test
    void reportsMissingNameAsEmptyOptional() {
        assertTrue(MedicineQueries.findByName(ITEMS, "Аспірин").isEmpty());
        assertThrows(NullPointerException.class,
                () -> MedicineQueries.findByName(ITEMS, null));
    }

    /* Порожній вхід: усі запити дають порожній або нульовий результат. */
    @Test
    void handlesEmptyInput() {
        List<Medicine> empty = List.of();
        assertTrue(MedicineQueries.expiringSoon(empty).isEmpty());
        assertTrue(MedicineQueries.names(empty).isEmpty());
        assertTrue(MedicineQueries.countByForm(empty).isEmpty());
        DoubleSummaryStatistics stats = MedicineQueries.priceStatistics(empty);
        assertEquals(0, stats.getCount());
        assertEquals(Double.POSITIVE_INFINITY, stats.getMin());
        assertEquals(Double.NEGATIVE_INFINITY, stats.getMax());
        assertTrue(MedicineQueries.nearestExpiry(empty, 5).isEmpty());
        assertTrue(MedicineQueries.findByName(empty, "Цитрамон").isEmpty());
    }

    /* Запити не змінюють порядок і склад вхідного змінного списку. */
    @Test
    void queriesDoNotMutateInput() {
        List<Medicine> input = new ArrayList<>(ITEMS);
        List<Medicine> before = List.copyOf(input);

        MedicineQueries.expiringSoon(input);
        MedicineQueries.names(input);
        MedicineQueries.countByForm(input);
        MedicineQueries.priceStatistics(input);
        MedicineQueries.nearestExpiry(input, 5);
        MedicineQueries.findByName(input, "Йод");

        assertEquals(before, input);
    }

    /* Розділ консолі для порожнього входу не містить нескінченностей. */
    @Test
    void queryReportHandlesEmptyInput() {
        String text = QueryReport.format(List.of());
        assertTrue(text.contains("кількість=0, min=—"), text);
        assertTrue(!text.contains("Infinity"), text);
    }
}
