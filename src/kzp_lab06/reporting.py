"""Формування та запис текстового звіту. Модуль не читає CSV."""

from pathlib import Path
from typing import Any


def _money(value: float) -> str:
    """Форматує суму з двома знаками після крапки й позначкою гривні."""
    return f"{value:.2f} грн"


def format_report(summary: dict[str, Any]) -> str:
    """Перетворює підсумок make_summary на детермінований текст звіту.

    Порядок рядків фіксований, словники й списки вже відсортовані, числа мають
    явний формат, тож однакові дані завжди дають однаковий текст.
    """
    selected = summary["selected"]
    count_by_form = summary["count_by_form"]
    nearest = summary["nearest"]
    average = summary["average_price"]
    forms = summary["forms"]

    lines = [
        "Звіт аптеки (варіант 11)",
        f"Усього записів: {summary['total']}",
        f"Рецептурних: {summary['prescription_count']}",
        f"Лікарські форми ({len(forms)}): {', '.join(forms) or 'немає'}",
        "Кількість за формою:",
    ]
    lines += [f"  {form}: {count}" for form, count in count_by_form.items()]
    if not count_by_form:
        lines.append("  немає записів")
    lines.append(f"Термін спливає за <= {summary['max_days']} дн.: {len(selected)}")
    lines += [f"  {name} — {days} дн." for days, name in selected]
    if not selected:
        lines.append("  немає таких препаратів")
    lines.append(
        f"Вартість препаратів із терміном, що спливає: "
        f"{_money(float(summary['selected_price']))}"
    )
    if nearest is None:
        lines.append("Найближчий термін: немає даних")
    else:
        days, names = nearest
        lines.append(f"Найближчий термін: {days} дн. ({', '.join(names)})")
    if average is None:
        lines.append("Середня ціна: немає даних")
    else:
        lines.append(f"Середня ціна: {_money(float(average))}")
    return "\n".join(lines) + "\n"


def write_report(path: Path, text: str) -> None:
    """Записує звіт у файл UTF-8 з LF; батьківський каталог створюється."""
    path.parent.mkdir(parents=True, exist_ok=True)
    # with гарантує закриття файла навіть у разі помилки запису.
    with path.open("w", encoding="utf-8", newline="\n") as target:
        target.write(text)
