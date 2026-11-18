package ua.lpnu.kzp;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Формує додатковий розділ звіту з оцінкою доступності кожного препарату.
 *
 * <p>Метод працює з колекцією спільного типу {@link Medicine} і викликає поліморфні
 * методи {@link Medicine#availabilityScore()} та {@link Medicine#kind()}. Перевірок
 * {@code instanceof} чи {@code switch} за підтипом тут немає: новий підтип препарату не
 * потребує змін у цьому класі.
 */
public final class AvailabilityReport {
    /* Забороняє створення екземплярів службового класу. */
    private AvailabilityReport() {
    }

    /**
     * Будує розділ звіту.
     *
     * @param medicines коректні препарати будь-яких підтипів
     * @return текст розділу з платформонезалежними переносами рядків
     */
    public static String format(List<Medicine> medicines) {
        StringBuilder text = new StringBuilder();
        text.append(String.format(Locale.ROOT, "Оцінка доступності (0–100):%n"));
        // Java сама викликає реалізацію availabilityScore() фактичного підтипу.
        text.append(medicines.stream()
                .map(medicine -> String.format(Locale.ROOT, "  %s [%s]: %.2f%n",
                        medicine.name(), medicine.kind().label(), medicine.availabilityScore()))
                .collect(Collectors.joining()));
        OptionalDouble average = medicines.stream()
                .mapToDouble(Medicine::availabilityScore)
                .average();
        text.append(average.isPresent()
                ? String.format(Locale.ROOT, "Середня оцінка: %.2f%n", average.getAsDouble())
                : String.format(Locale.ROOT, "Середня оцінка: —%n"));
        return text.toString();
    }
}
