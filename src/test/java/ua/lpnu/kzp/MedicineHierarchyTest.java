package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Тести ієрархії препаратів: базовий тип, підтипи, поліморфізм і рівність. */
class MedicineHierarchyTest {
    private static final double DELTA = 0.0001;

    // ---------- Базовий тип ----------

    /* Базовий тип відхиляє неправильний спільний стан для будь-якого підтипу. */
    @Test
    void baseTypeRejectsInvalidCommonStateInBothSubtypes() {
        assertThrows(IllegalArgumentException.class,
                () -> new FreeMedicine(" ", "таблетки", 1.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new PrescriptionMedicine(" ", "таблетки", 1.0, 1));
        assertThrows(NullPointerException.class,
                () -> new FreeMedicine("А", null, 1.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new PrescriptionMedicine("А", "б", -1.0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new FreeMedicine("А", "б", 1.0, -1));
    }

    /* Фабрика вибирає підтип за ознакою рецепта. */
    @Test
    void factoryChoosesSubtypeByPrescriptionFlag() {
        assertInstanceOf(PrescriptionMedicine.class, Medicine.of("А", "б", 1.0, 1, true));
        assertInstanceOf(FreeMedicine.class, Medicine.of("А", "б", 1.0, 1, false));
    }

    /* fromCsv створює об'єкти різних підтипів із того самого формату рядка. */
    @Test
    void fromCsvCreatesBothSubtypes() {
        Medicine insulin = Medicine.fromCsv("Інсулін;розчин;520.00;90;true");
        Medicine paracetamol = Medicine.fromCsv("Парацетамол;таблетки;45.50;420;false");

        assertInstanceOf(PrescriptionMedicine.class, insulin);
        assertInstanceOf(FreeMedicine.class, paracetamol);
    }

    // ---------- Підтипи ----------

    /* Рецептурний препарат приймає коректні поля. */
    @Test
    void prescriptionMedicineAcceptsValidFields() {
        Medicine medicine = new PrescriptionMedicine("Інсулін", "розчин", 520.0, 90);

        assertEquals("Інсулін", medicine.name());
        assertSame(MedicineKind.PRESCRIPTION, medicine.kind());
        assertTrue(medicine.prescription());
    }

    /* Рецептурний препарат відхиляє нульову ціну (власне правило підтипу). */
    @Test
    void prescriptionMedicineRejectsZeroPrice() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PrescriptionMedicine("Інсулін", "розчин", 0.0, 90));

        assertEquals("рецептурний препарат має мати додатну ціну", exception.getMessage());
    }

    /* Безрецептурний препарат приймає коректні поля, зокрема граничну нульову ціну. */
    @Test
    void freeMedicineAcceptsValidFieldsIncludingZeroPrice() {
        Medicine medicine = new FreeMedicine("Проба", "гель", 0.0, 0);

        assertSame(MedicineKind.FREE, medicine.kind());
        assertFalse(medicine.prescription());
        assertEquals(Medicine.MAX_SCORE, medicine.availabilityScore(), DELTA);
    }

    // ---------- enum ----------

    /* enum має дві категорії з підписами для звіту. */
    @Test
    void medicineKindHasLabels() {
        assertEquals(2, MedicineKind.values().length);
        assertEquals("рецептурний", MedicineKind.PRESCRIPTION.label());
        assertEquals("безрецептурний", MedicineKind.FREE.label());
        assertTrue(MedicineKind.PRESCRIPTION.requiresPrescription());
        assertFalse(MedicineKind.FREE.requiresPrescription());
    }

    /* Перетворення ознаки рецепта на enum і назви константи на enum. */
    @Test
    void medicineKindConversions() {
        assertSame(MedicineKind.PRESCRIPTION, MedicineKind.of(true));
        assertSame(MedicineKind.FREE, MedicineKind.of(false));
        assertSame(MedicineKind.FREE, MedicineKind.valueOf("FREE"));
        assertThrows(IllegalArgumentException.class, () -> MedicineKind.valueOf("OTHER"));
    }

    // ---------- Поліморфізм ----------

    /* Підтипи з однаковими полями дають різну оцінку через спільний метод. */
    @Test
    void subtypesCalculateAvailabilityDifferently() {
        Medicine prescription = new PrescriptionMedicine("Амоксицилін", "капсули", 132.0, 300);
        Medicine free = new FreeMedicine("Амоксицилін", "капсули", 132.0, 300);

        assertEquals(43.4, prescription.availabilityScore(), DELTA);
        assertEquals(86.8, free.availabilityScore(), DELTA);
        assertNotEquals(prescription.availabilityScore(), free.availabilityScore());
    }

    /* Граничний випадок: дуже дорогий препарат має оцінку 0, а не від'ємну. */
    @Test
    void availabilityIsNeverNegative() {
        assertEquals(0.0, new FreeMedicine("А", "б", 5000.0, 1).availabilityScore(), DELTA);
        assertEquals(0.0,
                new PrescriptionMedicine("А", "б", 5000.0, 1).availabilityScore(), DELTA);
    }

    /* Поліморфна колекція: звіт обробляє обидва підтипи одним циклом. */
    @Test
    void availabilityReportProcessesMixedCollection() {
        List<Medicine> medicines = List.of(
                Medicine.of("Парацетамол", "таблетки", 45.50, 420, false),
                Medicine.of("Інсулін", "розчин", 520.00, 90, true));

        String report = AvailabilityReport.format(medicines).replace("\r\n", "\n");

        assertEquals("""
                Оцінка доступності (0–100):
                  Парацетамол [безрецептурний]: 95.45
                  Інсулін [рецептурний]: 24.00
                Середня оцінка: 59.73
                """, report);
    }

    /* Порожня колекція не призводить до ділення на нуль. */
    @Test
    void availabilityReportHandlesEmptyList() {
        String report = AvailabilityReport.format(List.of());

        assertTrue(report.contains("Середня оцінка: —"));
    }

    // ---------- Рівність ----------

    /* Рівні об'єкти мають однаковий hashCode. */
    @Test
    void equalObjectsHaveSameHashCode() {
        Medicine first = new FreeMedicine("Цитрамон", "таблетки", 32.2, 60);
        Medicine second = Medicine.fromCsv("Цитрамон;таблетки;32.20;60;false");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    /* HashSet не дублює логічно рівні об'єкти. */
    @Test
    void hashSetDoesNotDuplicateEqualMedicines() {
        Set<Medicine> unique = new HashSet<>();
        unique.add(new PrescriptionMedicine("Інсулін", "розчин", 520.0, 90));
        unique.add(new PrescriptionMedicine("Інсулін", "розчин", 520.0, 90));
        unique.add(new FreeMedicine("Парацетамол", "таблетки", 45.5, 420));

        assertEquals(2, unique.size());
    }

    /* HashMap знаходить значення за рівним, але іншим об'єктом-ключем. */
    @Test
    void hashMapFindsValueByEqualKey() {
        Map<Medicine, Integer> stock = new HashMap<>();
        stock.put(new FreeMedicine("Ібупрофен", "гель", 98.75, 180), 12);

        assertEquals(Integer.valueOf(12), stock.get(Medicine.fromCsv("Ібупрофен;гель;98.75;180;false")));
    }

    /* Різні підтипи з однаковими полями не рівні. */
    @Test
    void differentSubtypesWithSameFieldsAreNotEqual() {
        Medicine prescription = new PrescriptionMedicine("Аспірин", "таблетки", 41.0, 250);
        Medicine free = new FreeMedicine("Аспірин", "таблетки", 41.0, 250);
        Set<Medicine> set = new HashSet<>(List.of(prescription, free));

        assertNotEquals(prescription, free);
        assertNotEquals(free, prescription);
        assertEquals(2, set.size());
    }
}
