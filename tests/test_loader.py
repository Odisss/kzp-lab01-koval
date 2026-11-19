"""Тести читання та перетворення CSV."""

from pathlib import Path

import pytest

from kzp_lab06.errors import DataError
from kzp_lab06.loader import iter_records, load_records

DATA_DIR = Path(__file__).resolve().parents[1] / "data"
HEADER = "name,form,price,days_left,prescription\n"


def test_valid_row_is_typed_and_normalized(write_csv) -> None:
    """Перевіряє типи полів і нормалізацію тексту."""
    path = write_csv(HEADER + "  Цитрамон ,Таблетки,32.20,12,TRUE\n")

    assert load_records(path) == [
        {
            "name": "Цитрамон",
            "form": "таблетки",
            "price": 32.2,
            "days_left": 12,
            "prescription": True,
        }
    ]


def test_variant_file_has_seven_records() -> None:
    """Перевіряє контрольний файл варіанта, зокрема поле з комою й лапками."""
    records = load_records(DATA_DIR / "lab06.csv")

    assert len(records) == 7
    assert records[5]["name"] == 'Вітаміни "Сонечко", дитячі'


def test_header_only_gives_empty_list(write_csv) -> None:
    """Файл лише з заголовком — коректний порожній набір."""
    assert load_records(write_csv(HEADER)) == []


def test_completely_empty_file_is_error(write_csv) -> None:
    """Файл без заголовка відхиляється."""
    with pytest.raises(DataError, match="Рядок 1: файл порожній"):
        load_records(write_csv(""))


def test_bad_number_reports_row_field_and_cause(write_csv) -> None:
    """Хибне число: номер рядка, назва поля й першопричина ValueError."""
    path = write_csv(HEADER + "А,таблетки,1.0,5,false\nБ,сироп,ціна,5,false\n")

    with pytest.raises(DataError, match="Рядок 3: поле price") as caught:
        load_records(path)
    assert isinstance(caught.value.__cause__, ValueError)


@pytest.mark.parametrize(
    ("line", "message"),
    [
        ("А,таблетки,-1,5,false", "price не може бути від'ємним"),
        ("А,таблетки,1,-5,false", "days_left не може бути від'ємним"),
        ("А,таблетки,1,2.5,false", "days_left має бути цілим"),
        ("А,таблетки,nan,5,false", "price має бути скінченним"),
        ("А,таблетки,1,5,так", "prescription має бути true або false"),
        (" ,таблетки,1,5,false", "поле name порожнє"),
    ],
)
def test_invalid_values(write_csv, line: str, message: str) -> None:
    """Від'ємні, дробові, нескінченні та нерозпізнані значення відхиляються."""
    with pytest.raises(DataError, match=f"Рядок 2: .*{message}"):
        load_records(write_csv(HEADER + line + "\n"))


def test_zero_values_are_boundary_valid(write_csv) -> None:
    """Нуль — допустима межа для ціни й днів."""
    record = load_records(write_csv(HEADER + "А,гель,0,0,false\n"))[0]

    assert (record["price"], record["days_left"]) == (0.0, 0)


def test_missing_column(write_csv) -> None:
    """Відсутній стовпець у заголовку."""
    path = write_csv("name,form,price,prescription\nА,гель,1,false\n")

    with pytest.raises(DataError, match="відсутні стовпці: days_left"):
        load_records(path)


def test_unknown_column(write_csv) -> None:
    """Зайвий стовпець у заголовку."""
    path = write_csv(HEADER.strip() + ",color\nА,гель,1,5,false,red\n")

    with pytest.raises(DataError, match="невідомі стовпці: color"):
        load_records(path)


def test_missing_value_in_row(write_csv) -> None:
    """Рядок коротший за заголовок."""
    path = write_csv(HEADER + "А,гель,1\n")

    with pytest.raises(DataError, match="Рядок 2: відсутнє значення поля days_left"):
        load_records(path)


def test_extra_value_in_row(write_csv) -> None:
    """Рядок довший за заголовок."""
    path = write_csv(HEADER + "А,гель,1,5,false,зайве\n")

    with pytest.raises(DataError, match="Рядок 2: зайві значення"):
        load_records(path)


def test_missing_file_keeps_cause(tmp_path: Path) -> None:
    """Відсутній файл — DataError з OSError як першопричиною."""
    with pytest.raises(DataError, match="Не вдалося прочитати") as caught:
        load_records(tmp_path / "немає.csv")
    assert isinstance(caught.value.__cause__, OSError)


def test_not_utf8_file(tmp_path: Path) -> None:
    """Файл у cp1251 відхиляється з поясненням про кодування."""
    path = tmp_path / "cp1251.csv"
    path.write_bytes((HEADER + "Аспірин,гель,1,5,false\n").encode("cp1251"))

    with pytest.raises(DataError, match="UTF-8"):
        load_records(path)


def test_iter_records_is_lazy() -> None:
    """Генератор не читає рядки після першої помилки, доки його не попросять."""
    lines = iter([HEADER, "А,гель,1,5,false\n", "Б,гель,x,5,false\n"])
    records = iter_records(lines)

    assert next(records)["name"] == "А"
    with pytest.raises(DataError, match="Рядок 3"):
        next(records)


def test_input_file_is_not_modified() -> None:
    """Читання не змінює вхідний файл."""
    path = DATA_DIR / "lab06.csv"
    before = path.read_bytes()

    load_records(path)

    assert path.read_bytes() == before
