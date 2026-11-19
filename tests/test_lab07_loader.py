"""Тести loader ЛР № 7: CSV → об'єкти відповідного підтипу."""

from pathlib import Path

import pytest

from kzp_lab07.errors import DataError
from kzp_lab07.loader import iter_medicines, load_medicines
from kzp_lab07.models import FreeMedicine, PrescriptionMedicine

DATA_DIR = Path(__file__).resolve().parents[1] / "data"
HEADER = "name,form,price,days_left,prescription\n"


def test_rows_become_subtypes(write_csv) -> None:
    """Ознака рецепта обирає підтип; текст нормалізовано."""
    path = write_csv(
        HEADER + "  Цитрамон ,Таблетки,32.20,12,FALSE\nІнсулін,розчин,520,90,true\n"
    )

    assert load_medicines(path) == [
        FreeMedicine("Цитрамон", "таблетки", 32.2, 12),
        PrescriptionMedicine("Інсулін", "розчин", 520.0, 90),
    ]


def test_variant_file_has_both_subtypes_and_duplicate() -> None:
    """Контрольний CSV ЛР № 7: 8 записів, обидва підтипи, рівний дублікат."""
    medicines = load_medicines(DATA_DIR / "lab07.csv")

    assert len(medicines) == 8
    assert {type(item) for item in medicines} == {FreeMedicine, PrescriptionMedicine}
    assert medicines[3] == medicines[7]
    assert medicines[3] is not medicines[7]


def test_header_only_gives_empty_list() -> None:
    """Файл лише з заголовком — коректний порожній набір."""
    assert load_medicines(DATA_DIR / "lab06-empty.csv") == []


def test_completely_empty_file_is_error(write_csv) -> None:
    """Файл без заголовка відхиляється."""
    with pytest.raises(DataError, match="Рядок 1: файл порожній"):
        load_medicines(write_csv(""))


def test_setter_error_gets_row_number() -> None:
    """Від'ємна ціна відхиляється setter-ом, loader додає номер рядка."""
    with pytest.raises(DataError, match="Рядок 3: поле price не може") as caught:
        load_medicines(DATA_DIR / "lab07-invalid.csv")
    assert isinstance(caught.value.__cause__, ValueError)


@pytest.mark.parametrize(
    ("line", "message"),
    [
        ("А,таблетки,ціна,5,false", "price має бути числом"),
        ("А,таблетки,nan,5,false", "price має бути скінченним"),
        ("А,таблетки,1,2.5,false", "days_left має бути цілим"),
        ("А,таблетки,1,-5,false", "days_left не може бути від'ємним"),
        ("А,таблетки,1,5,так", "prescription має бути true або false"),
        (" ,таблетки,1,5,false", "поле name порожнє"),
        ("А,,1,5,false", "поле form порожнє"),
        ("А,гель,1", "відсутнє значення поля days_left"),
        ("А,гель,1,5,false,зайве", "зайві значення"),
    ],
)
def test_invalid_rows(write_csv, line: str, message: str) -> None:
    """Пошкоджений рядок — DataError з номером рядка й назвою поля."""
    with pytest.raises(DataError, match=f"Рядок 2: .*{message}"):
        load_medicines(write_csv(HEADER + line + "\n"))


def test_header_errors(write_csv) -> None:
    """Відсутній і зайвий стовпці."""
    with pytest.raises(DataError, match="відсутні стовпці: days_left"):
        load_medicines(write_csv("name,form,price,prescription\n"))
    with pytest.raises(DataError, match="невідомі стовпці: color"):
        load_medicines(write_csv(HEADER.strip() + ",color\n"))


def test_missing_file_keeps_cause(tmp_path: Path) -> None:
    """Відсутній файл — DataError з OSError як першопричиною."""
    with pytest.raises(DataError, match="Не вдалося прочитати") as caught:
        load_medicines(tmp_path / "немає.csv")
    assert isinstance(caught.value.__cause__, OSError)


def test_iter_medicines_is_lazy() -> None:
    """Генератор повертає коректний об'єкт до рядка з помилкою."""
    medicines = iter_medicines(
        iter([HEADER, "А,гель,1,5,true\n", "Б,гель,x,5,false\n"])
    )

    assert isinstance(next(medicines), PrescriptionMedicine)
    with pytest.raises(DataError, match="Рядок 3"):
        next(medicines)
