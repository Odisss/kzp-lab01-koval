package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MedicineCsvStoreTest {
    private static final String HEADER = "придатність,форма,назва,рецепт,ціна";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    /* П'ять записів варіанта + один із комою й лапками + один із перенесенням рядка. */
    private static final List<MedicineRecord> RECORDS = List.of(
            new MedicineRecord("Парацетамол", "таблетки", 45.5, LocalDate.of(2028, 1, 12), MedicineKind.FREE),
            new MedicineRecord("Амоксицилін", "капсули", 132.0, LocalDate.of(2027, 9, 14), MedicineKind.PRESCRIPTION),
            new MedicineRecord("Ібупрофен", "гель", 98.75, LocalDate.of(2027, 5, 17), MedicineKind.FREE),
            new MedicineRecord("Цитрамон", "таблетки", 32.2, LocalDate.of(2027, 1, 17), MedicineKind.FREE),
            new MedicineRecord("Інсулін", "розчин", 520.0, LocalDate.of(2027, 2, 16), MedicineKind.PRESCRIPTION),
            new MedicineRecord("Вітаміни \"Сонечко\", дитячі", "сироп", 89.9, LocalDate.of(2027, 11, 18), MedicineKind.FREE),
            new MedicineRecord("Мазь\nдля суглобів", "мазь", 0.0, LocalDate.of(2027, 6, 1), MedicineKind.FREE));

    /* Створює тимчасовий файл із текстом. */
    private static Path csv(String text) throws IOException {
        Path file = Files.createTempDirectory("lab05").resolve("medicines.csv");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        return file;
    }

    /* Round-trip: об'єкти → CSV → об'єкти рівні за equals і за ключем. */
    @Test
    void roundTripRestoresEqualRecords() throws IOException, DataStorageException {
        Path file = Files.createTempDirectory("lab05").resolve("out").resolve("medicines.csv");

        MedicineCsvStore.write(file, RECORDS);
        List<MedicineRecord> restored = MedicineCsvStore.read(file);

        assertEquals(RECORDS, restored);
        assertEquals(RECORDS.stream().map(MedicineRecord::key).toList(),
                restored.stream().map(MedicineRecord::key).toList());
        assertTrue(Files.readString(file, StandardCharsets.UTF_8).startsWith(HEADER));
    }

    /* Екранований рядок у файлі та multiline-поле займає два фізичні рядки. */
    @Test
    void escapedRowsInFile() throws IOException, DataStorageException {
        Path file = Files.createTempDirectory("lab05").resolve("medicines.csv");
        MedicineCsvStore.write(file, RECORDS);
        String text = Files.readString(file, StandardCharsets.UTF_8);

        assertTrue(text.contains("2027-11-18,сироп,\"Вітаміни \"\"Сонечко\"\", дитячі\",FREE,89.9"), text);
        assertTrue(text.contains("\"Мазь\nдля суглобів\""), text);
        assertEquals(RECORDS.size() + 2, text.lines().count());
    }

    /* Модель попередніх робіт: Medicine → запис → Medicine без втрат. */
    @Test
    void conversionToModelAndBack() {
        Medicine insulin = Medicine.of("Інсулін", "розчин", 520.0, 90, true);
        MedicineRecord record = MedicineRecord.from(insulin, TODAY);

        assertEquals(LocalDate.of(2027, 2, 16), record.expiresOn());
        assertEquals(MedicineKind.PRESCRIPTION, record.prescription());
        assertEquals(insulin, record.toMedicine(TODAY));
        assertThrows(IllegalArgumentException.class, () -> record.toMedicine(LocalDate.of(2028, 1, 1)));
    }

    /* Порожній файл і файл лише із заголовком – порожній список. */
    @Test
    void emptyFileAndHeaderOnly() throws IOException, DataStorageException {
        assertEquals(List.of(), MedicineCsvStore.read(csv("")));
        assertEquals(List.of(), MedicineCsvStore.read(csv(HEADER + "\n")));

        Path file = Files.createTempDirectory("lab05").resolve("empty.csv");
        MedicineCsvStore.write(file, List.of());
        assertEquals(HEADER + System.lineSeparator(), Files.readString(file, StandardCharsets.UTF_8));
    }

    /* Хибна кількість полів – виняток без cause, з номером запису. */
    @Test
    void wrongFieldCount() throws IOException {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> MedicineCsvStore.read(csv(HEADER + "\n2027-01-01,таблетки,Аспірин,FREE\n")));

        assertTrue(exception.getMessage().startsWith("Запис 1:"), exception.getMessage());
        assertTrue(exception.getMessage().contains("знайдено 4"), exception.getMessage());
        assertNull(exception.getCause());
    }

    /* Хибне число – cause NumberFormatException. */
    @Test
    void badNumberKeepsCause() throws IOException {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> MedicineCsvStore.read(csv(HEADER
                        + "\n2027-03-01,таблетки,Парацетамол,FREE,45.5"
                        + "\n2027-01-10,капсули,Амоксицилін,PRESCRIPTION,дорого\n")));

        assertTrue(exception.getMessage().startsWith("Запис 2:"), exception.getMessage());
        assertInstanceOf(NumberFormatException.class, exception.getCause());
    }

    /* Невідомий enum і хибна дата – cause збережено. */
    @Test
    void badEnumAndDateKeepCause() throws IOException {
        DataStorageException badEnum = assertThrows(DataStorageException.class,
                () -> MedicineCsvStore.read(csv(HEADER + "\n2027-03-01,таблетки,Аспірин,МОЖЛИВО,41\n")));
        DataStorageException badDate = assertThrows(DataStorageException.class,
                () -> MedicineCsvStore.read(csv(HEADER + "\n31.12.2027,таблетки,Аспірин,FREE,41\n")));

        assertInstanceOf(IllegalArgumentException.class, badEnum.getCause());
        assertTrue(badEnum.getMessage().contains("МОЖЛИВО"), badEnum.getMessage());
        assertInstanceOf(DateTimeParseException.class, badDate.getCause());
    }

    /* Порушення інваріанту моделі в CSV – власний виняток із причиною. */
    @Test
    void invariantViolationInCsv() throws IOException {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> MedicineCsvStore.read(csv(HEADER + "\n2027-03-01,таблетки,Но-шпа,FREE,-15\n")));

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    /* Хибний заголовок і пошкоджений CSV. */
    @Test
    void wrongHeaderAndBrokenQuotes() throws IOException {
        assertThrows(DataStorageException.class,
                () -> MedicineCsvStore.read(csv("назва,форма,ціна\nА,б,1\n")));
        assertThrows(DataStorageException.class,
                () -> MedicineCsvStore.read(csv(HEADER + "\n2027-03-01,таблетки,\"Аспірин,FREE,41\n")));
    }

    /* Відсутній файл – cause NoSuchFileException. */
    @Test
    void missingFileKeepsIoCause() throws IOException {
        Path missing = Files.createTempDirectory("lab05").resolve("nope.csv");

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> MedicineCsvStore.read(missing));
        assertInstanceOf(NoSuchFileException.class, exception.getCause());
    }

    /* Конструктор запису перевіряє інваріанти моделі. */
    @Test
    void recordValidation() {
        LocalDate date = LocalDate.of(2027, 1, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new MedicineRecord(" ", "таблетки", 1, date, MedicineKind.FREE));
        assertThrows(IllegalArgumentException.class,
                () -> new MedicineRecord("А", "таблетки", Double.NaN, date, MedicineKind.FREE));
        assertThrows(IllegalArgumentException.class,
                () -> new MedicineRecord("А", "таблетки", 0, date, MedicineKind.PRESCRIPTION));
        assertThrows(NullPointerException.class,
                () -> new MedicineRecord("А", "таблетки", 1, null, MedicineKind.FREE));
        assertThrows(NullPointerException.class,
                () -> new MedicineRecord("А", "таблетки", 1, date, null));
    }

    /* equals і hashCode узгоджені; ключ – назва + форма. */
    @Test
    void equalityAndKey() {
        MedicineRecord a = new MedicineRecord("Аспірин", "таблетки", 41, LocalDate.of(2027, 1, 1), MedicineKind.FREE);
        MedicineRecord b = new MedicineRecord("Аспірин", "таблетки", 41, LocalDate.of(2027, 1, 1), MedicineKind.FREE);
        MedicineRecord c = new MedicineRecord("Аспірин", "таблетки", 41, LocalDate.of(2027, 1, 2), MedicineKind.FREE);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(!a.equals(c));
        assertEquals("Аспірин|таблетки", a.key());
    }
}
