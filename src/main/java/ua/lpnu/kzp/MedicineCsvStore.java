package ua.lpnu.kzp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Збереження та читання {@link MedicineRecord} у CSV.
 *
 * <p>Запис делегується універсальному {@link CsvExporter}. Читання: увесь файл через
 * {@link Files#readString(Path, java.nio.charset.Charset)} з UTF-8, розбір документним
 * {@link CsvParser}, перевірка заголовка, відновлення кожного запису через
 * {@link MedicineRecord#fromFields(List)}.
 *
 * <p>Контракт порожніх даних: файл нульової довжини і файл лише із заголовком – обидва
 * допустимі й дають порожній список.
 */
public final class MedicineCsvStore {
    /* Забороняє створення екземплярів службового класу. */
    private MedicineCsvStore() {
    }

    /**
     * Записує записи у CSV.
     *
     * @param path    шлях до файла
     * @param records записи
     * @throws DataStorageException якщо файл неможливо записати
     */
    public static void write(Path path, List<MedicineRecord> records)
            throws DataStorageException {
        CsvExporter.write(path, MedicineRecord.class, records);
    }

    /**
     * Читає записи з CSV у тому самому порядку.
     *
     * @param path шлях до файла
     * @return незмінний список записів
     * @throws DataStorageException якщо файл неможливо прочитати ({@code cause} –
     *                              {@link IOException}), заголовок не збігається або
     *                              запис хибний (повідомлення містить номер запису)
     */
    public static List<MedicineRecord> read(Path path) throws DataStorageException {
        Objects.requireNonNull(path, "шлях не може бути null");
        String text;
        try {
            text = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося прочитати CSV: " + path, exception);
        }
        return parse(text);
    }

    /**
     * Відновлює записи з тексту CSV.
     *
     * @param text текст файла
     * @return незмінний список записів
     * @throws DataStorageException якщо заголовок не збігається або запис хибний
     */
    public static List<MedicineRecord> parse(String text) throws DataStorageException {
        List<List<String>> rows = CsvParser.parseDocument(text);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> expected = CsvParser.parse(CsvExporter.header(MedicineRecord.class));
        if (!rows.get(0).equals(expected)) {
            throw new DataStorageException("Хибний заголовок CSV: %s, очікується %s"
                    .formatted(rows.get(0), expected));
        }
        List<MedicineRecord> records = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            try {
                records.add(MedicineRecord.fromFields(rows.get(index)));
            } catch (DataStorageException exception) {
                // Номер запису додається як контекст; першопричина (якщо є) не губиться.
                throw new DataStorageException(
                        "Запис %d: %s".formatted(index, exception.getMessage()),
                        exception.getCause());
            }
        }
        return List.copyOf(records);
    }
}
