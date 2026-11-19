"""Обчислення над препаратами через протокол Available.

Модуль не відкриває файлів, не друкує й не перевіряє конкретний підтип:
відмінна поведінка викликається через prescription і availability_score().
"""

from collections import Counter
from collections.abc import Iterable, Sequence

from kzp_lab07.models import Available, ReportSummary

#: Поріг відбору варіанта 11: препарати, яким лишилося не більше 30 днів.
DEFAULT_MAX_DAYS = 30


def expiry_key(item: Available) -> tuple[int, str]:
    """Ключ сортування: спершу менший залишок терміну, далі назва."""
    return item.days_left, item.name


def availability_key(item: Available) -> tuple[float, str]:
    """Ключ сортування: спершу найменш доступний препарат, далі назва."""
    return item.availability_score(), item.name


def select_expiring(
    items: Iterable[Available], max_days: int = DEFAULT_MAX_DAYS
) -> list[Available]:
    """Відбирає препарати з days_left <= max_days у вхідному порядку."""
    return [item for item in items if item.days_left <= max_days]


def count_by_form(items: Iterable[Available]) -> dict[str, int]:
    """Рахує препарати за формою; ключі відсортовано для стабільного звіту."""
    counts = Counter(item.form for item in items)
    return {form: counts[form] for form in sorted(counts)}


def nearest_expiry(items: Sequence[Available]) -> tuple[int, tuple[str, ...]] | None:
    """Повертає найменший days_left і відсортовані назви з ним або None."""
    if not items:
        return None
    nearest = min(item.days_left for item in items)
    names = sorted(item.name for item in items if item.days_left == nearest)
    return nearest, tuple(names)


def average_price(items: Sequence[Available]) -> float | None:
    """Середня ціна; для порожнього входу None (ділення на нуль не виконується)."""
    if not items:
        return None
    return sum(item.price for item in items) / len(items)


def rank_by_availability(items: Iterable[Available]) -> list[tuple[str, float]]:
    """Повертає пари (назва, індекс) від найменш доступного препарату.

    Формула індексу належить підтипу; тут викликається лише спільний метод.
    """
    return [
        (item.name, item.availability_score())
        for item in sorted(items, key=availability_key)
    ]


def least_available(items: Iterable[Available]) -> Available | None:
    """Повертає найменш доступний препарат або None для порожнього входу."""
    return min(items, key=availability_key, default=None)


def make_summary(
    items: Sequence[Available], max_days: int = DEFAULT_MAX_DAYS
) -> ReportSummary:
    """Збирає всі показники звіту варіанта 11 в незмінний ReportSummary."""
    selected = sorted(select_expiring(items, max_days), key=expiry_key)
    return ReportSummary(
        total=len(items),
        max_days=max_days,
        selected=tuple(expiry_key(item) for item in selected),
        selected_price=sum(item.price for item in selected),
        forms=tuple(sorted({item.form for item in items})),
        count_by_form=tuple(count_by_form(items).items()),
        prescription_count=sum(1 for item in items if item.prescription),
        nearest=nearest_expiry(items),
        average_price=average_price(items),
        availability=tuple(rank_by_availability(items)),
    )
