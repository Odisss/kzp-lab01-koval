package ua.lpnu.kzp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Консольна програма «Аптека» (варіант 11): читає записи про препарати, перевіряє їх,
 * обчислює чотири показники та виводить звіт у консоль і файл.
 *
 * <p>Коректні препарати зберігаються в {@link Repository}; опція {@code --export}
 * записує їх у CSV через {@link CsvExporter}, читає назад і порівнює (round-trip), а
 * {@code --import} читає раніше збережений CSV.
 */
public final class Main {
    /** Код завершення: успіх. */
    static final int EXIT_OK = 0;
    /** Код завершення: помилка виконання (немає файла, немає коректних записів). */
    static final int EXIT_ERROR = 1;
    /** Код завершення: неправильні аргументи командного рядка. */
    static final int EXIT_USAGE = 2;

    private static final String DEFAULT_INPUT = "data/input.csv";
    private static final String DEFAULT_OUTPUT = "out/report.txt";

    /* Забороняє створення екземплярів класу запуску. */
    private Main() {
    }

    /**
     * Точка входу програми.
     *
     * @param args аргументи: {@code --help}, {@code --version}, {@code --input <файл>},
     *             {@code --output <файл>}, {@code --availability}, {@code --queries},
     *             {@code --export <csv>}, {@code --import <csv>}
     */
    public static void main(String[] args) {
        // Явний UTF-8 для консолі, щоб українські літери не залежали від налаштувань ОС.
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        int code = run(args, out, err);
        if (code != EXIT_OK) {
            System.exit(code);
        }
    }

    /**
     * Виконує програму й повертає код завершення (окремо від {@link #main}, щоб це можна
     * було перевірити в тестах).
     *
     * @param args аргументи командного рядка
     * @param out  потік звичайного виводу
     * @param err  потік повідомлень про помилки
     * @return {@link #EXIT_OK}, {@link #EXIT_ERROR} або {@link #EXIT_USAGE}
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        // --version розпізнається до будь-якої іншої обробки аргументів.
        if (args.length == 1 && "--version".equals(args[0])) {
            out.printf("%s%n", BuildInfo.version());
            return EXIT_OK;
        }

        String input = DEFAULT_INPUT;
        String output = DEFAULT_OUTPUT;
        boolean availability = false;
        boolean queries = false;
        String export = null;
        String importCsv = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help", "-h" -> {
                    printHelp(out);
                    return EXIT_OK;
                }
                case "--input" -> {
                    if (i + 1 >= args.length) {
                        return usageError(err, "після --input потрібен шлях до файла");
                    }
                    input = args[++i];
                }
                case "--output" -> {
                    if (i + 1 >= args.length) {
                        return usageError(err, "після --output потрібен шлях до файла");
                    }
                    output = args[++i];
                }
                case "--availability" -> availability = true;
                case "--queries" -> queries = true;
                case "--export" -> {
                    if (i + 1 >= args.length) {
                        return usageError(err, "після --export потрібен шлях до CSV");
                    }
                    export = args[++i];
                }
                case "--import" -> {
                    if (i + 1 >= args.length) {
                        return usageError(err, "після --import потрібен шлях до CSV");
                    }
                    importCsv = args[++i];
                }
                default -> {
                    return usageError(err, "невідомий аргумент: " + arg);
                }
            }
        }

        Path inputPath;
        Path outputPath;
        Path exportPath;
        try {
            if (importCsv != null) {
                return importCsv(Path.of(importCsv), out, err);
            }
            inputPath = Path.of(input);
            outputPath = Path.of(output);
            exportPath = export == null ? null : Path.of(export);
        } catch (InvalidPathException exception) {
            return usageError(err, "некоректний шлях: " + exception.getInput());
        }
        return process(inputPath, outputPath, new Options(availability, queries, exportPath),
                out, err);
    }

    /* Додаткові дії після основного звіту. */
    private record Options(boolean availability, boolean queries, Path export) {
    }

    /**
     * Читає CSV, створений {@code --export}, і друкує відновлені записи.
     *
     * @param csv шлях до CSV
     * @param out потік звичайного виводу
     * @param err потік повідомлень про помилки
     * @return код завершення
     */
    private static int importCsv(Path csv, PrintStream out, PrintStream err) {
        try {
            List<MedicineRecord> records = MedicineCsvStore.read(csv);
            out.printf("Імпортовано з %s: %d записів%n", csv, records.size());
            records.forEach(record -> out.printf("  %s%n", record));
            return EXIT_OK;
        } catch (DataStorageException exception) {
            printStorageError(err, exception);
            return EXIT_ERROR;
        }
    }

    /**
     * Експортує препарати сховища в CSV, читає файл назад і порівнює списки.
     *
     * @param repository сховище коректних препаратів
     * @param csv        шлях до CSV
     * @param out        потік звичайного виводу
     * @param err        потік повідомлень про помилки
     * @return {@code true}, якщо експорт і round-trip успішні
     */
    private static boolean exportCsv(Repository<Medicine> repository, Path csv,
                                     PrintStream out, PrintStream err) {
        LocalDate today = LocalDate.now();
        List<MedicineRecord> original = repository.all().stream()
                .map(medicine -> MedicineRecord.from(medicine, today))
                .toList();
        try {
            MedicineCsvStore.write(csv, original);
            List<MedicineRecord> restored = MedicineCsvStore.read(csv);
            List<Medicine> restoredMedicines = restored.stream()
                    .map(record -> record.toMedicine(today))
                    .toList();
            boolean equal = original.equals(restored)
                    && repository.all().equals(restoredMedicines);
            out.printf("CSV записано: %s (%d записів)%n", csv, original.size());
            out.printf("Round-trip: відновлено %d записів, рівні: %s%n",
                    restored.size(), equal ? "так" : "ні");
            if (!equal) {
                err.printf("Помилка: відновлені записи відрізняються від початкових%n");
            }
            return equal;
        } catch (DataStorageException exception) {
            printStorageError(err, exception);
            return false;
        }
    }

    /* Друкує повідомлення власного винятку разом із ланцюгом причин. */
    private static void printStorageError(PrintStream err, DataStorageException exception) {
        err.printf("Помилка: %s%n", exception.getMessage());
        for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
            err.printf("  причина: %s%n", cause);
        }
    }

    /**
     * Читає файл, обчислює показники, друкує звіт і записує його у файл.
     *
     * @param input  шлях до вхідного файла
     * @param output шлях до файла звіту
     * @param options додаткові розділи та шлях експорту
     * @param out    потік звичайного виводу
     * @param err    потік повідомлень про помилки
     * @return код завершення
     */
    private static int process(Path input, Path output, Options options,
                               PrintStream out, PrintStream err) {
        List<String> lines;
        try {
            if (!Files.isRegularFile(input)) {
                err.printf("Помилка: файл не знайдено: %s%n", input);
                return EXIT_ERROR;
            }
            lines = FileReport.readLines(input);
        } catch (IOException exception) {
            // Найчастіше це файл не в UTF-8 (MalformedInputException) або немає прав доступу.
            err.printf("Помилка читання %s: %s%n", input, exception);
            return EXIT_ERROR;
        }

        // Послідовність однакова для всіх рядків: прочитати → перевірити → обчислити → вивести.
        ParseResult parsed = MedicineParser.parseAll(lines);
        // Коректні записи зберігаються в параметризованому сховищі, а не в «голому» списку.
        Repository<Medicine> repository = new Repository<>();
        repository.addAll(parsed.medicines());
        List<Medicine> medicines = repository.all();
        Stats stats = Stats.compute(medicines);
        String report = ReportFormatter.format(stats, parsed.errors());

        out.print(report);
        if (options.availability()) {
            // Додатковий розділ лише в консолі: файл звіту лишається сумісним із ЛР1–ЛР2.
            out.print(AvailabilityReport.format(medicines));
        }
        if (options.queries()) {
            // Як і --availability: лише консоль, файл звіту не змінюється.
            out.print(QueryReport.format(medicines));
        }
        try {
            FileReport.writeReport(output, report);
            out.printf("Звіт записано: %s%n", output);
        } catch (IOException exception) {
            err.printf("Помилка запису %s: %s%n", output, exception);
            return EXIT_ERROR;
        }

        if (options.export() != null && !exportCsv(repository, options.export(), out, err)) {
            return EXIT_ERROR;
        }
        if (stats.count() == 0) {
            err.printf("Помилка: у файлі немає жодного коректного запису%n");
            return EXIT_ERROR;
        }
        return EXIT_OK;
    }

    /**
     * Повідомляє про неправильні аргументи.
     *
     * @param err     потік помилок
     * @param message опис проблеми
     * @return {@link #EXIT_USAGE}
     */
    private static int usageError(PrintStream err, String message) {
        err.printf("Помилка: %s%n", message);
        err.printf("Довідка: java -jar lab01.jar --help%n");
        return EXIT_USAGE;
    }

    /**
     * Друкує довідку.
     *
     * @param out потік виводу
     */
    private static void printHelp(PrintStream out) {
        out.printf("Аптека: звіт за файлом препаратів (лабораторні роботи № 1–5, варіант 11)%n");
        out.printf("%n");
        out.printf("Використання:%n");
        out.printf("  java -jar lab01.jar [--input <файл>] [--output <файл>] [--availability] [--queries]%n");
        out.printf("                      [--export <csv>]%n");
        out.printf("  java -jar lab01.jar --import <csv>%n");
        out.printf("  java -jar lab01.jar --help | --version%n");
        out.printf("%n");
        out.printf("Параметри:%n");
        out.printf("  --input <файл>   вхідний CSV, UTF-8 (типово %s)%n", DEFAULT_INPUT);
        out.printf("  --output <файл>  файл звіту (типово %s)%n", DEFAULT_OUTPUT);
        out.printf("  --availability   додатково вивести оцінку доступності препаратів%n");
        out.printf("  --queries        додатково вивести результати потокових запитів%n");
        out.printf("  --export <csv>   зберегти коректні записи в CSV (UTF-8) і перевірити round-trip%n");
        out.printf("  --import <csv>   прочитати CSV, створений --export, і вивести записи%n");
        out.printf("  --version        версія та номер збірки, напр. 1.4.0+local%n");
        out.printf("  --help           ця довідка%n");
        out.printf("%n");
        out.printf("Формат рядка: name;form;price;daysToExpire;prescription%n");
    }
}
