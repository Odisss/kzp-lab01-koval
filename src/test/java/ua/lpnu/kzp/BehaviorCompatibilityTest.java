package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Доказ збереження зовнішньої поведінки: після перебудови на класи звіт для контрольного
 * файла збігається зі звітом лабораторної роботи № 1 ({@code data/expected-report.txt}).
 */
class BehaviorCompatibilityTest {

    /* Звіт для data/input.csv рядок у рядок збігається з контрольним виводом. */
    @Test
    void reportMatchesLab01Output() throws IOException {
        // Arrange
        Path output = Files.createTempDirectory("lab02").resolve("report.txt");
        PrintStream silent = new PrintStream(new ByteArrayOutputStream(), true,
                StandardCharsets.UTF_8);

        // Act
        int code = Main.run(new String[] {"--input", "data/input.csv",
            "--output", output.toString()}, silent, silent);

        // Assert: порівнюємо рядки, щоб CRLF/LF різних ОС не впливали на результат.
        assertEquals(Main.EXIT_OK, code);
        assertEquals(
                Files.readAllLines(Path.of("data", "expected-report.txt"), StandardCharsets.UTF_8),
                Files.readAllLines(output, StandardCharsets.UTF_8));
    }
}
