package ua.lpnu.kzp;

/**
 * Повідомляє про помилку зберігання або обробки даних: запис чи читання CSV, хибний
 * формат рядка, некоректне значення поля або помилку рефлексії.
 *
 * <p>Виняток перевірюваний (checked): викликач зобов'язаний вирішити, як реагувати на
 * збій файлу чи пошкоджені дані. Технічна першопричина ({@code IOException},
 * {@code NumberFormatException}, {@code IllegalAccessException} тощо) зберігається як
 * {@link #getCause() cause} і не губиться.
 */
public class DataStorageException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Створює виняток із повідомленням.
     *
     * @param message опис операції, що не вдалася
     */
    public DataStorageException(String message) {
        super(message);
    }

    /**
     * Створює виняток із повідомленням і першопричиною.
     *
     * @param message опис операції, що не вдалася
     * @param cause   початкова технічна помилка
     */
    public DataStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
