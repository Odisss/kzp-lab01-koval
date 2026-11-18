package ua.lpnu.kzp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Типобезпечне сховище об'єктів у пам'яті.
 *
 * <p>Параметр типу {@code T} задається під час створення ({@code Repository<Medicine>},
 * {@code Repository<MedicineRecord>}), тому компілятор не дозволить додати об'єкт іншого
 * типу, а читання не потребує приведень.
 *
 * @param <T> тип записів сховища
 */
public final class Repository<T> {
    private final List<T> items = new ArrayList<>();

    /**
     * Створює порожнє сховище.
     */
    public Repository() {
        // Список створюється в ініціалізаторі поля.
    }

    /**
     * Додає непорожній запис до сховища.
     *
     * @param item запис
     * @throws NullPointerException якщо {@code item} дорівнює {@code null}
     */
    public void add(T item) {
        items.add(Objects.requireNonNull(item, "запис не може бути null"));
    }

    /**
     * Додає всі записи списку в тому самому порядку.
     *
     * @param newItems записи
     * @throws NullPointerException якщо список або будь-який запис дорівнює {@code null}
     */
    public void addAll(List<? extends T> newItems) {
        Objects.requireNonNull(newItems, "список не може бути null");
        newItems.forEach(this::add);
    }

    /**
     * Повертає незмінний знімок усіх записів у порядку додавання. Зміни сховища після
     * виклику знімок не змінюють, а сам знімок змінити неможливо.
     *
     * @return незмінний список записів
     */
    public List<T> all() {
        return List.copyOf(items);
    }

    /**
     * Повертає записи, що задовольняють умову. Умова може приймати {@code T} або його
     * супертип ({@code Predicate<Object>}, {@code Predicate<Medicine>} для підтипу тощо).
     *
     * @param condition умова відбору
     * @return незмінний список знайдених записів у порядку додавання
     * @throws NullPointerException якщо {@code condition} дорівнює {@code null}
     */
    public List<T> find(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "умова не може бути null");
        return items.stream().filter(condition).toList();
    }

    /**
     * Повертає кількість записів.
     *
     * @return кількість записів, не менша за нуль
     */
    public int size() {
        return items.size();
    }
}
