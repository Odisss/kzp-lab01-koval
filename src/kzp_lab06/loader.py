"""Читання CSV аптеки та перетворення рядків на типізовані записи."""

import csv
import math
from collections.abc import Iterable, Iterator
from pathlib import Path

from kzp_lab06.errors import DataError

#: Обов'язковий заголовок CSV у фіксованому порядку.
FIELDS: tuple[str, ...] = ("name", "form", "price", "days_left", "prescription")

#: Числові поля запису; лише їх можна підсумовувати в analysis.summarize.
NUMERIC_FIELDS: frozenset[str] = frozenset({"price", "days_left"})

_BOOLEAN_VALUES = {"true": True, "false": False}

Record = dict[str, object]


def _text(row: dict[str, str | None], field: str, row_number: int) -> str:
    """Повертає непорожнє текстове поле без пробілів по краях."""
    value = row.get(field)
    if value is None:
        raise DataError(f"Рядок {row_number}: відсутнє значення поля {field}")
    value = value.strip()
    if not value:
        raise DataError(f"Рядок {row_number}: поле {field} порожнє")
    return value


def _non_negative(value: float, field: str, row_number: int) -> None:
    """Відхиляє від'ємне числове значення."""
    if value < 0:
        raise DataError(f"Рядок {row_number}: поле {field} не може бути від'ємним")


def parse_price(row: dict[str, str | None], row_number: int) -> float:
    """Перетворює ціну на float (крапка як десятковий роздільник, не менше 0)."""
    text = _text(row, "price", row_number)
    try:
        price = float(text)
    except ValueError as error:
        raise DataError(
            f"Рядок {row_number}: поле price має бути числом, отримано {text!r}"
        ) from error
    if not math.isfinite(price):
        raise DataError(f"Рядок {row_number}: поле price має бути скінченним числом")
    _non_negative(price, "price", row_number)
    return price


def parse_days_left(row: dict[str, str | None], row_number: int) -> int:
    """Перетворює кількість днів до кінця терміну на int (не менше 0)."""
    text = _text(row, "days_left", row_number)
    try:
        days = int(text)
    except ValueError as error:
        raise DataError(
            f"Рядок {row_number}: поле days_left має бути цілим числом, "
            f"отримано {text!r}"
        ) from error
    _non_negative(days, "days_left", row_number)
    return days


def parse_prescription(row: dict[str, str | None], row_number: int) -> bool:
    """Перетворює ознаку рецепта true/false (без урахування регістру) на bool."""
    text = _text(row, "prescription", row_number).lower()
    try:
        return _BOOLEAN_VALUES[text]
    except KeyError as error:
        raise DataError(
            f"Рядок {row_number}: поле prescription має бути true або false, "
            f"отримано {text!r}"
        ) from error


def parse_row(row: dict[str, str | None], row_number: int) -> Record:
    """Перетворює один рядок DictReader на запис із типізованими значеннями."""
    if None in row:
        # DictReader складає зайві значення рядка під ключ None (restkey).
        raise DataError(f"Рядок {row_number}: зайві значення поза заголовком")
    return {
        "name": _text(row, "name", row_number),
        "form": _text(row, "form", row_number).lower(),
        "price": parse_price(row, row_number),
        "days_left": parse_days_left(row, row_number),
        "prescription": parse_prescription(row, row_number),
    }


def _check_header(fieldnames: list[str] | None) -> None:
    """Перевіряє, що заголовок є і містить рівно обов'язкові стовпці."""
    if fieldnames is None:
        raise DataError("Рядок 1: файл порожній, відсутній заголовок CSV")
    header = [name.strip() for name in fieldnames]
    missing = [field for field in FIELDS if field not in header]
    if missing:
        raise DataError(f"Рядок 1: відсутні стовпці: {', '.join(missing)}")
    unknown = sorted(set(header) - set(FIELDS))
    if unknown:
        raise DataError(f"Рядок 1: невідомі стовпці: {', '.join(unknown)}")
    if len(header) != len(set(header)):
        raise DataError("Рядок 1: стовпці в заголовку повторюються")


def iter_records(lines: Iterable[str]) -> Iterator[Record]:
    """Ліниво читає CSV-рядки й повертає записи по одному.

    Заголовок перевіряється перед першим записом. Номер рядка відповідає рядку
    файла (заголовок — рядок 1), тому його можна знайти в редакторі.
    """
    reader = csv.DictReader(lines)
    _check_header(reader.fieldnames)
    reader.fieldnames = [name.strip() for name in reader.fieldnames or []]
    for row in reader:
        yield parse_row(row, reader.line_num)


def load_records(path: Path) -> list[Record]:
    """Читає UTF-8 CSV-файл і повертає список записів.

    Файл лише з заголовком дає порожній список. Файл без заголовка, пошкоджений
    рядок або недоступний файл — DataError із першопричиною.
    """
    try:
        # newline="" передає керування роздільниками рядків модулю csv.
        with path.open("r", encoding="utf-8", newline="") as source:
            return list(iter_records(source))
    except UnicodeDecodeError as error:
        raise DataError(f"Файл {path} не в кодуванні UTF-8") from error
    except csv.Error as error:
        raise DataError(f"Файл {path}: пошкоджений CSV ({error})") from error
    except OSError as error:
        raise DataError(f"Не вдалося прочитати файл {path}: {error}") from error
