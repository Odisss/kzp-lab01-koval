package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvExporterTest {
    private static final String NL = System.lineSeparator();

    /* Довільний клас іншої предметної області: експортер його не знає. */
    static class Point {
        @CsvColumn("y")
        private final int y;
        @CsvColumn("x")
        private final int x;
        private final String ignored = "не експортується";

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /* Підклас: поля суперкласу теж експортуються. */
    static final class NamedPoint extends Point {
        @CsvColumn("мітка")
        private final String label;

        NamedPoint(int x, int y, String label) {
            super(x, y);
            this.label = label;
        }
    }

    /* Клас без анотованих полів. */
    static final class Plain {
        private final int value = 1;
    }

    /* Анотація з RetentionPolicy.CLASS не видна рефлексії. */
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.FIELD)
    @interface ClassOnly {
    }

    /* Клас з анотацією, що недоступна під час виконання. */
    static final class Invisible {
        @ClassOnly
        private final int value = 1;
    }

    /* Анотація доступна під час виконання й обмежена полями. */
    @Test
    void annotationIsRuntimeAndFieldOnly() throws NoSuchFieldException {
        Retention retention = CsvColumn.class.getAnnotation(Retention.class);
        Target target = CsvColumn.class.getAnnotation(Target.class);
        Field name = MedicineRecord.class.getDeclaredField("name");

        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertEquals(List.of(ElementType.FIELD), List.of(target.value()));
        assertEquals("назва", name.getAnnotation(CsvColumn.class).value());
        assertEquals(null, Invisible.class.getDeclaredField("value").getAnnotation(ClassOnly.class));
    }

    /* Порядок стовпців – за Java-іменами полів, назви – з анотації. */
    @Test
    void headerOfVariantClassIsSortedByFieldName() throws DataStorageException {
        assertEquals("придатність,форма,назва,рецепт,ціна",
                CsvExporter.header(MedicineRecord.class));
    }

    /* Експортер працює з довільним класом і проходить ієрархію. */
    @Test
    void exportsAnyAnnotatedClassIncludingSuperclassFields() throws DataStorageException {
        String plain = CsvExporter.toCsv(Point.class, List.of(new Point(1, 2)));
        String named = CsvExporter.toCsv(NamedPoint.class,
                List.of(new NamedPoint(3, 4, "A, B")));

        assertEquals("x,y" + NL + "1,2" + NL, plain);
        assertEquals("мітка,x,y" + NL + "\"A, B\",3,4" + NL, named);
    }

    /* Порожній список: лише заголовок, клас переданий окремо. */
    @Test
    void emptyListGivesHeaderOnly() throws DataStorageException {
        assertEquals("x,y" + NL, CsvExporter.toCsv(Point.class, List.of()));
    }

    /* Кома, лапка й перенесення рядка екрануються; звичайний текст – ні. */
    @Test
    void escapesSpecialCharacters() {
        assertEquals("Парацетамол", CsvExporter.escape("Парацетамол"));
        assertEquals("\"Карти, що говорять\"", CsvExporter.escape("Карти, що говорять"));
        assertEquals("\"Вітаміни \"\"Сонечко\"\"\"", CsvExporter.escape("Вітаміни \"Сонечко\""));
        assertEquals("\"рядок1\nрядок2\"", CsvExporter.escape("рядок1\nрядок2"));
        assertEquals("", CsvExporter.escape(""));
    }

    /* Клас без @CsvColumn – власний виняток із поясненням. */
    @Test
    void classWithoutAnnotatedFieldsFails() {
        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.toCsv(Plain.class, List.of(new Plain())));
        assertTrue(exception.getMessage().contains("@CsvColumn"), exception.getMessage());
        assertThrows(DataStorageException.class,
                () -> CsvExporter.toCsv(Invisible.class, List.of()));
    }

    /* Експорт не змінює ні записів, ні списку. */
    @Test
    void exportDoesNotChangeInput() throws DataStorageException {
        List<Point> points = List.of(new Point(1, 2), new Point(5, 6));
        CsvExporter.toCsv(Point.class, points);

        assertEquals(2, points.size());
        assertEquals(1, points.get(0).x);
        assertNotNull(points.get(0).ignored);
    }

    /* Файл пишеться в UTF-8 і перезаписується повністю. */
    @Test
    void writeTruncatesOldContentAndUsesUtf8() throws IOException, DataStorageException {
        Path dir = Files.createTempDirectory("lab05");
        Path csv = dir.resolve("nested").resolve("points.csv");
        CsvExporter.write(csv, Point.class, List.of(new Point(1, 2), new Point(3, 4)));
        CsvExporter.write(csv, NamedPoint.class, List.of(new NamedPoint(1, 2, "Ґанок")));

        assertEquals("мітка,x,y" + NL + "Ґанок,1,2" + NL,
                Files.readString(csv, StandardCharsets.UTF_8));
    }

    /* Помилка файлової системи зберігається як cause. */
    @Test
    void fileErrorKeepsIoExceptionAsCause() throws IOException {
        Path dir = Files.createTempDirectory("lab05");
        Path notADirectory = Files.writeString(dir.resolve("file.txt"), "x");
        Path csv = notADirectory.resolve("points.csv");

        DataStorageException exception = assertThrows(DataStorageException.class,
                () -> CsvExporter.write(csv, Point.class, List.of(new Point(1, 2))));
        assertInstanceOf(IOException.class, exception.getCause());
        assertTrue(exception.getMessage().contains("points.csv"), exception.getMessage());
    }
}
