package ua.lpnu.kzp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позначає поле, яке експортується в CSV, і зберігає назву його стовпця.
 *
 * <p>{@link RetentionPolicy#RUNTIME} залишає анотацію в байт-коді й доступною під час
 * виконання, тому {@link CsvExporter} читає її рефлексією. {@link ElementType#FIELD}
 * дозволяє ставити анотацію лише над полями. Сама анотація нічого не виконує: вона лише
 * описує контракт для експортера. Порядок стовпців задає не анотація, а сортування
 * Java-імен полів.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CsvColumn {
    /**
     * Повертає заголовок CSV-стовпця.
     *
     * @return непорожня назва стовпця
     */
    String value();
}
