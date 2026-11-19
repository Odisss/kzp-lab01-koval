"""Читання CSV аптеки та перетворення рядків на об'єкти Medicine.

Loader відповідає лише за текст → типи → об'єкт. Діапазони значень перевіряють
setter-и моделі; їхні ValueError/TypeError тут доповнюються номером рядка.
"""

import csv
import math
from collections.abc import Iterable, Iterator
from pathlib import Path

from kzp_lab07.errors import DataError
from kzp_lab07.models import FreeMedicine, Medicine, PrescriptionMedicine

#: Обов'язковий заголовок CSV у фіксованому порядку (не змінився з ЛР № 6).
FIELDS: tuple[str, ...] = ("name", "form", "price", "days_left", "prescription")

_BOOLEAN_VALUES = {"true": True, "false": False}

#: Вибір підтипу на межі завантаження: ознака рецепта → клас.
_SUBTYPES: dict[bool, type[Medicine]] = {
    True: PrescriptionMedicine,
    False: FreeMedicine,
}


def _cell(row: dict[str, str | None], field: str, row_number: int) -> str:
    """Повертає значення поля без пробілів по краях."""
    value = row.get(field)
    if value is None:
        raise DataError(f"Рядок {row_number}: відсутнє значення поля {field}")
    return value.strip()


def _to_float(text: str, row_number: int) -> float:
    """Перетворює текст ціни на скінченне число з крапкою."""
    try:
        price = float(text)
    except ValueError as error:
        raise DataError(
            f"Рядок {row_number}: поле price має бути числом, отримано {text!r}"
        ) from error
    if not math.isfinite(price):
        raise DataError(f"Рядок {row_number}: поле price має бути скінченним числом")
    return price


def _to_int(text: str, row_number: int) -> int:
    """Перетворює текст days_left на ціле."""
    try:
        return int(text)
    except ValueError as error:
        raise DataError(
            f"Рядок {row_number}: поле days_left має бути цілим числом, "
            f"отримано {text!r}"
        ) from error


def _to_bool(text: str, row_number: int) -> bool:
    """Перетворює true/false (без урахування регістру) на bool."""
    try:
        return _BOOLEAN_VALUES[text.lower()]
    except KeyError as error:
        raise DataError(
            f"Рядок {row_number}: поле prescription має бути true або false, "
            f"отримано {text!r}"
        ) from error


def build_medicine(row: dict[str, str | None], row_number: int) -> Medicine:
    """Перетворює один рядок DictReader на відповідний підтип Medicine."""
    if None in row:
        # DictReader складає зайві значення рядка під ключ None (restkey).
        raise DataError(f"Рядок {row_number}: зайві значення поза заголовком")
    cells = {field: _cell(row, field, row_number) for field in FIELDS}
    subtype = _SUBTYPES[_to_bool(cells["prescription"], row_number)]
    try:
        return subtype(
            name=cells["name"],
            form=cells["form"],
            price=_to_float(cells["price"], row_number),
            days_left=_to_int(cells["days_left"], row_number),
        )
    except (TypeError, ValueError) as error:
        if isinstance(error, DataError):
            raise
        raise DataError(f"Рядок {row_number}: {error}") from error


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


def iter_medicines(lines: Iterable[str]) -> Iterator[Medicine]:
    """Ліниво читає CSV-рядки й повертає об'єкти по одному.

    Номер рядка відповідає рядку файла (заголовок — рядок 1).
    """
    reader = csv.DictReader(lines)
    _check_header(reader.fieldnames)
    reader.fieldnames = [name.strip() for name in reader.fieldnames or []]
    for row in reader:
        yield build_medicine(row, reader.line_num)


def load_medicines(path: Path) -> list[Medicine]:
    """Читає UTF-8 CSV-файл і повертає список препаратів.

    Файл лише з заголовком дає порожній список. Файл без заголовка, пошкоджений
    рядок або недоступний файл — DataError із першопричиною.
    """
    try:
        # newline="" передає керування роздільниками рядків модулю csv.
        with path.open("r", encoding="utf-8", newline="") as source:
            return list(iter_medicines(source))
    except UnicodeDecodeError as error:
        raise DataError(f"Файл {path} не в кодуванні UTF-8") from error
    except csv.Error as error:
        raise DataError(f"Файл {path}: пошкоджений CSV ({error})") from error
    except OSError as error:
        raise DataError(f"Не вдалося прочитати файл {path}: {error}") from error
