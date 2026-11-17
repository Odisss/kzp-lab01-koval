package ua.lpnu.kzp;

import java.util.List;

/** Результат розбору файла: коректні записи та повідомлення про пропущені рядки. */
public final class ParseResult {
    private final List<Medicine> medicines;
    private final List<String> errors;

    /**
     * Створює результат розбору.
     *
     * @param medicines коректні записи
     * @param errors    повідомлення про пропущені рядки
     */
    public ParseResult(List<Medicine> medicines, List<String> errors) {
        this.medicines = List.copyOf(medicines);
        this.errors = List.copyOf(errors);
    }

    /**
     * Повертає коректні записи.
     *
     * @return незмінний список записів
     */
    public List<Medicine> medicines() {
        return medicines;
    }

    /**
     * Повертає повідомлення про пропущені рядки.
     *
     * @return незмінний список повідомлень із номером рядка та причиною
     */
    public List<String> errors() {
        return errors;
    }
}
