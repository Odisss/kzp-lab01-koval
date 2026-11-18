package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MainTest {
    /* Результат одного запуску програми. */
    private record Run(int code, String out, String err) {
    }

    /* Запускає програму з перехопленням виводу в UTF-8. */
    private static Run run(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Run(code, out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    /* Записує вхідний файл у тимчасовий каталог. */
    private static Path writeInput(Path dir, String content) throws IOException {
        Path input = dir.resolve("input.csv");
        Files.writeString(input, content, StandardCharsets.UTF_8);
        return input;
    }

    /* --version виводить версію з номером збірки. */
    @Test
    void versionMode() {
        Run result = run("--version");

        assertEquals(Main.EXIT_OK, result.code());
        assertTrue(result.out().strip().matches("\\d+\\.\\d+\\.\\d+\\+\\w+"), result.out());
    }

    /* --help виводить довідку й завершується успішно. */
    @Test
    void helpMode() {
        Run result = run("--help");

        assertEquals(Main.EXIT_OK, result.code());
        assertTrue(result.out().contains("--input"));
        assertTrue(result.out().contains("--output"));
    }

    /* Невідомий аргумент і аргумент без значення — помилка використання. */
    @Test
    void badArgumentsGiveUsageError() {
        assertEquals(Main.EXIT_USAGE, run("--foo").code());
        assertEquals(Main.EXIT_USAGE, run("--input").code());
        assertEquals(Main.EXIT_USAGE, run("--output").code());
    }

    /* Відсутній вхідний файл — зрозуміла помилка без винятку. */
    @Test
    void missingInputFile() throws IOException {
        Path dir = Files.createTempDirectory("lab01");
        Run result = run("--input", dir.resolve("nope.csv").toString(),
                "--output", dir.resolve("r.txt").toString());

        assertEquals(Main.EXIT_ERROR, result.code());
        assertTrue(result.err().contains("не знайдено"), result.err());
    }

    /* Повний цикл: звіт у консолі збігається зі звітом у файлі, помилки мають номери рядків. */
    @Test
    void fullRunWritesSameReportToConsoleAndFile() throws IOException {
        Path dir = Files.createTempDirectory("lab01");
        Path input = writeInput(dir, String.join("\n",
                "Парацетамол;таблетки;45.50;420;false",
                "Амоксицилін;капсули;132.00;300;true",
                "Цитрамон;таблетки;32.20;60;false",
                "Нурофен;сироп;ціна;200;false",
                "Но-шпа;таблетки;-15.00;365;false"));
        Path output = dir.resolve("nested").resolve("report.txt");

        Run result = run("--input", input.toString(), "--output", output.toString());

        assertEquals(Main.EXIT_OK, result.code());
        String file = Files.readString(output, StandardCharsets.UTF_8);
        assertTrue(file.contains("Коректних записів: 3"), file);
        assertTrue(file.contains("Середня ціна: 69.90 грн"), file);
        assertTrue(file.contains("Найменший термін придатності: 60 дн."), file);
        assertTrue(file.contains("Рецептурних препаратів: 1"), file);
        assertTrue(file.contains("Рядок 4:"), file);
        assertTrue(file.contains("Рядок 5:"), file);
        assertTrue(result.out().startsWith(file), "Консоль має містити той самий звіт");
    }

    /* Файл без жодного коректного запису: звіт створюється, код завершення — помилка. */
    @Test
    void noValidRecords() throws IOException {
        Path dir = Files.createTempDirectory("lab01");
        Path input = writeInput(dir, "сміття\n\n;;;;\n");
        Path output = dir.resolve("report.txt");

        Run result = run("--input", input.toString(), "--output", output.toString());

        assertEquals(Main.EXIT_ERROR, result.code());
        assertTrue(Files.exists(output));
        assertTrue(result.err().contains("немає жодного коректного запису"), result.err());
        assertFalse(result.out().contains("NaN"), result.out());
    }

    /* --availability додає розділ у консоль, але файл звіту лишається попереднього формату. */
    @Test
    void availabilityOptionPrintsScoresOnlyToConsole() throws IOException {
        Path dir = Files.createTempDirectory("lab03");
        Path input = writeInput(dir, String.join("\n",
                "Парацетамол;таблетки;45.50;420;false",
                "Інсулін;розчин;520.00;90;true"));
        Path output = dir.resolve("report.txt");

        Run result = run("--input", input.toString(), "--output", output.toString(),
                "--availability");

        assertEquals(Main.EXIT_OK, result.code());
        assertTrue(result.out().contains("Парацетамол [безрецептурний]: 95.45"), result.out());
        assertTrue(result.out().contains("Інсулін [рецептурний]: 24.00"), result.out());
        String file = Files.readString(output, StandardCharsets.UTF_8);
        assertFalse(file.contains("Оцінка доступності"), file);
    }

    /* --queries друкує п'ять потокових запитів лише в консоль. */
    @Test
    void queriesOptionPrintsStreamResultsOnlyToConsole() throws IOException {
        Path dir = Files.createTempDirectory("lab04");
        Path input = writeInput(dir, String.join("\n",
                "Парацетамол;таблетки;45.50;420;false",
                "Цитрамон;таблетки;32.20;30;false",
                "Інсулін;розчин;520.00;90;true"));
        Path output = dir.resolve("report.txt");

        Run result = run("--input", input.toString(), "--output", output.toString(),
                "--queries");

        assertEquals(Main.EXIT_OK, result.code());
        assertTrue(result.out().contains("Придатність до 30 днів: [Цитрамон]"), result.out());
        assertTrue(result.out().contains("Кількість за формою: {розчин=1, таблетки=2}"),
                result.out());
        assertTrue(result.out().contains("Топ-5 за найближчою придатністю: "
                + "[Цитрамон, Інсулін, Парацетамол]"), result.out());
        String file = Files.readString(output, StandardCharsets.UTF_8);
        assertFalse(file.contains("Потокові запити"), file);
    }

    /* --export записує CSV, читає назад і підтверджує round-trip; --import читає файл. */
    @Test
    void exportAndImportRoundTrip() throws IOException {
        Path dir = Files.createTempDirectory("lab05");
        Path input = writeInput(dir, String.join("\n",
                "Парацетамол;таблетки;45.50;420;false",
                "Вітаміни \"Сонечко\", дитячі;сироп;89.90;365;false",
                "Інсулін;розчин;520.00;90;true"));
        Path csv = dir.resolve("csv").resolve("medicines.csv");

        Run export = run("--input", input.toString(), "--output", dir.resolve("r.txt").toString(),
                "--export", csv.toString());
        Run imported = run("--import", csv.toString());

        assertEquals(Main.EXIT_OK, export.code(), export.err());
        assertTrue(export.out().contains("Round-trip: відновлено 3 записів, рівні: так"),
                export.out());
        assertTrue(Files.readString(csv, StandardCharsets.UTF_8)
                .startsWith("придатність,форма,назва,рецепт,ціна"));
        assertEquals(Main.EXIT_OK, imported.code(), imported.err());
        assertTrue(imported.out().contains("Вітаміни \"Сонечко\", дитячі (сироп)"), imported.out());
    }

    /* Хибний CSV і відсутній файл при --import – помилка з причиною, без трасування стеку. */
    @Test
    void importErrorsShowCause() throws IOException {
        Path dir = Files.createTempDirectory("lab05");
        Path bad = dir.resolve("bad.csv");
        Files.writeString(bad, "придатність,форма,назва,рецепт,ціна\n"
                + "2027-01-10,капсули,Амоксицилін,PRESCRIPTION,дорого\n", StandardCharsets.UTF_8);

        Run badRun = run("--import", bad.toString());
        Run missing = run("--import", dir.resolve("nope.csv").toString());

        assertEquals(Main.EXIT_ERROR, badRun.code());
        assertTrue(badRun.err().contains("Запис 1: поле 5 (ціна)"), badRun.err());
        assertTrue(badRun.err().contains("причина: java.lang.NumberFormatException"), badRun.err());
        assertEquals(Main.EXIT_ERROR, missing.code());
        assertTrue(missing.err().contains("NoSuchFileException"), missing.err());
        assertEquals(Main.EXIT_USAGE, run("--export").code());
        assertEquals(Main.EXIT_USAGE, run("--import").code());
    }
}
