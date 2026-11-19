"""Обчислення над записами аптеки. Модуль не відкриває файлів і не друкує."""

from kzp_lab06.loader import NUMERIC_FIELDS, Record

#: Поріг відбору варіанта 11: препарати, яким лишилося не більше 30 днів.
DEFAULT_MAX_DAYS = 30


def select_expiring(
    records: list[Record], max_days: int = DEFAULT_MAX_DAYS
) -> list[Record]:
    """Відбирає записи з days_left <= max_days у вхідному порядку."""
    return [record for record in records if int(record["days_left"]) <= max_days]


def unique_forms(records: list[Record]) -> set[str]:
    """Повертає множину лікарських форм без повторів."""
    return {str(record["form"]) for record in records}


def count_by_form(records: list[Record]) -> dict[str, int]:
    """Рахує записи за формою; ключі відсортовано для стабільного звіту."""
    forms = unique_forms(records)
    return {
        form: sum(1 for record in records if record["form"] == form)
        for form in sorted(forms)
    }


def nearest_expiry(records: list[Record]) -> tuple[int, list[str]] | None:
    """Повертає найменший days_left і відсортовані назви з цим терміном.

    Для порожнього списку повертає None: найближчого терміну немає.
    """
    if not records:
        return None
    nearest = min(int(record["days_left"]) for record in records)
    names = sorted(
        str(record["name"]) for record in records if int(record["days_left"]) == nearest
    )
    return nearest, names


def average_price(records: list[Record]) -> float | None:
    """Середня ціна; для порожнього списку None (ділення на нуль не виконується)."""
    if not records:
        return None
    return sum(float(record["price"]) for record in records) / len(records)


def summarize(
    records: list[Record],
    *metrics: str,
    max_days: int = DEFAULT_MAX_DAYS,
    **labels: str,
) -> dict[str, object]:
    """Сумує вибрані числові поля для записів із days_left <= max_days.

    metrics — назви полів (кортеж), labels — підписи для них (словник).
    Невідома метрика або підпис до неї — ValueError. Якщо відібрано нуль
    записів, кожна метрика дорівнює 0.0.
    """
    unknown = sorted(set(metrics) - NUMERIC_FIELDS)
    if unknown:
        raise ValueError(f"Невідома метрика: {', '.join(unknown)}")
    stray = sorted(set(labels) - set(metrics))
    if stray:
        raise ValueError(f"Підпис для невибраної метрики: {', '.join(stray)}")
    selected = select_expiring(records, max_days)
    result: dict[str, object] = {"count": len(selected)}
    for metric in metrics:
        result[labels.get(metric, metric)] = sum(
            float(record[metric]) for record in selected
        )
    return result


def make_summary(
    records: list[Record], max_days: int = DEFAULT_MAX_DAYS
) -> dict[str, object]:
    """Збирає всі показники звіту варіанта 11 в один словник."""
    selected = select_expiring(records, max_days)
    return {
        "total": len(records),
        "max_days": max_days,
        # Спершу ті, у кого термін спливає раніше; при рівності — за назвою.
        "selected": sorted(
            (int(record["days_left"]), str(record["name"])) for record in selected
        ),
        "selected_price": summarize(
            records, "price", max_days=max_days, price="price_sum"
        )["price_sum"],
        "forms": sorted(unique_forms(records)),
        "count_by_form": count_by_form(records),
        "prescription_count": sum(1 for record in records if record["prescription"]),
        "nearest": nearest_expiry(records),
        "average_price": average_price(records),
    }
