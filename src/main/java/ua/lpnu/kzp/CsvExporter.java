package ua.lpnu.kzp;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Універсальний рефлексивний CSV-експортер.
 *
 * <p>Експортер не знає жодного предметного класу: він отримує {@code Class<T>} і список
 * записів, рефлексією знаходить поля з {@link CsvColumn}, бере заголовки з анотації, а
 * значення – з полів. Порядок стовпців детермінований: поля сортуються за Java-іменем
 * (за рівності імен у різних класах ієрархії – за іменем класу). Поля успадкованих класів
 * також враховуються: експортер проходить ланцюжок до {@link Object}.
 *
 * <p>Файл записується в UTF-8 з {@link StandardOpenOption#TRUNCATE_EXISTING}, тому
 * старі рядки не залишаються після коротшого нового експорту. Рядки розділяються
 * {@link System#lineSeparator()}; {@link CsvParser} приймає і {@code \n}, і {@code \r\n}.
 */
public final class CsvExporter {
    /** Роздільник полів. */
    public static final char SEPARATOR = ',';

    /** Символ лапок для екранування. */
    public static final char QUOTE = '"';

    /* Порядок стовпців: Java-ім'я поля, потім ім'я класу, у якому поле оголошено. */
    private static final Comparator<Field> FIELD_ORDER = Comparator
            .comparing(Field::getName)
            .thenComparing(field -> field.getDeclaringClass().getName());

    /* Забороняє створення екземплярів службового класу. */
    private CsvExporter() {
    }

    /**
     * Записує анотовані поля об'єктів у CSV-файл (UTF-8). Батьківський каталог
     * створюється за потреби, наявний файл перезаписується повністю.
     *
     * @param <T>     тип записів
     * @param path    шлях до CSV-файла
     * @param type    клас записів; потрібен окремо, щоб побудувати заголовок і для
     *                порожнього списку
     * @param records записи для експорту; список і записи не змінюються
     * @throws DataStorageException якщо в класі немає анотованих полів, поле неможливо
     *                              прочитати або файл неможливо записати (тоді
     *                              {@code cause} – початковий {@link IOException})
     */
    public static <T> void write(Path path, Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(path, "шлях не може бути null");
        String csv = toCsv(type, records);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, csv, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new DataStorageException("Не вдалося записати CSV: " + path, exception);
        }
    }

    /**
     * Формує повний текст CSV: заголовок і по одному запису на кожен об'єкт.
     *
     * @param <T>     тип записів
     * @param type    клас записів
     * @param records записи
     * @return текст CSV із завершальним роздільником рядка
     * @throws DataStorageException якщо в класі немає анотованих полів або поле
     *                              неможливо прочитати
     */
    public static <T> String toCsv(Class<T> type, List<? extends T> records)
            throws DataStorageException {
        Objects.requireNonNull(type, "клас не може бути null");
        Objects.requireNonNull(records, "список записів не може бути null");
        List<Field> fields = annotatedFields(type);
        String newline = System.lineSeparator();
        StringBuilder csv = new StringBuilder(header(fields)).append(newline);
        int number = 0;
        for (T record : records) {
            number++;
            if (record == null) {
                throw new DataStorageException("Запис %d дорівнює null".formatted(number));
            }
            csv.append(row(fields, record, number)).append(newline);
        }
        return csv.toString();
    }

    /**
     * Повертає рядок заголовка CSV для класу (без роздільника рядка).
     *
     * @param type клас записів
     * @return екрановані назви стовпців через кому
     * @throws DataStorageException якщо в класі немає анотованих полів
     */
    public static String header(Class<?> type) throws DataStorageException {
        return header(annotatedFields(type));
    }

    /**
     * Знаходить усі нестатичні поля з {@link CsvColumn} у класі та його суперкласах і
     * відкриває до них доступ.
     *
     * @param type клас записів
     * @return поля в детермінованому порядку
     * @throws DataStorageException якщо анотованих полів немає, назва стовпця порожня або
     *                              доступ до поля заборонено
     */
    public static List<Field> annotatedFields(Class<?> type) throws DataStorageException {
        Objects.requireNonNull(type, "клас не може бути null");
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(CsvColumn.class)
                        && !Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
        }
        if (fields.isEmpty()) {
            throw new DataStorageException(
                    "Клас %s не має полів з @CsvColumn".formatted(type.getName()));
        }
        fields.sort(FIELD_ORDER);
        for (Field field : fields) {
            if (field.getAnnotation(CsvColumn.class).value().isBlank()) {
                throw new DataStorageException(
                        "Порожня назва стовпця для поля " + field.getName());
            }
            if (!field.trySetAccessible()) {
                throw new DataStorageException("Немає доступу до поля " + field.getName());
            }
        }
        return List.copyOf(fields);
    }

    /**
     * Екранує одне значення за правилами CSV: значення з комою, лапкою або
     * перенесенням рядка береться в лапки, лапка всередині подвоюється.
     *
     * @param value текст значення, не {@code null}
     * @return значення, готове до запису в CSV
     */
    public static String escape(String value) {
        Objects.requireNonNull(value, "значення не може бути null");
        boolean needsQuotes = value.indexOf(SEPARATOR) >= 0 || value.indexOf(QUOTE) >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        if (!needsQuotes) {
            return value;
        }
        return QUOTE + value.replace("\"", "\"\"") + QUOTE;
    }

    /* Будує заголовок з анотацій. */
    private static String header(List<Field> fields) {
        return fields.stream()
                .map(field -> escape(field.getAnnotation(CsvColumn.class).value()))
                .collect(Collectors.joining(String.valueOf(SEPARATOR)));
    }

    /* Будує рядок значень одного запису; null подається порожнім полем. */
    private static String row(List<Field> fields, Object record, int number)
            throws DataStorageException {
        List<String> values = new ArrayList<>(fields.size());
        for (Field field : fields) {
            Object value;
            try {
                value = field.get(record);
            } catch (IllegalAccessException | IllegalArgumentException exception) {
                throw new DataStorageException("Запис %d: не вдалося прочитати поле %s"
                        .formatted(number, field.getName()), exception);
            }
            values.add(escape(value == null ? "" : String.valueOf(value)));
        }
        return String.join(String.valueOf(SEPARATOR), values);
    }
}
