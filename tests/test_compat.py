"""Сумісність: словникова модель ЛР № 6 і об'єктна ЛР № 7 дають той самий звіт."""

from pathlib import Path

import pytest

from kzp_lab06.analysis import make_summary as dict_summary
from kzp_lab06.loader import load_records
from kzp_lab06.reporting import format_report as dict_report
from kzp_lab07.analysis import make_summary as object_summary
from kzp_lab07.loader import load_medicines
from kzp_lab07.reporting import format_report as object_report

DATA_DIR = Path(__file__).resolve().parents[1] / "data"


@pytest.mark.parametrize("name", ["lab06.csv", "lab06-empty.csv", "lab07.csv"])
@pytest.mark.parametrize("max_days", [0, 30, 365])
def test_reports_are_identical(name: str, max_days: int) -> None:
    """Однаковий CSV і поріг — побайтово однаковий текст звіту."""
    path = DATA_DIR / name

    old = dict_report(dict_summary(load_records(path), max_days))
    new = object_report(object_summary(load_medicines(path), max_days))

    assert new == old


def test_object_report_matches_lab06_expected_file() -> None:
    """Еталон ЛР № 6 відтворюється об'єктною моделлю."""
    expected = (DATA_DIR / "lab06-expected-report.txt").read_text(encoding="utf-8")

    assert object_report(object_summary(load_medicines(DATA_DIR / "lab06.csv"))) == (
        expected
    )


def test_same_rows_rejected_by_both_models(write_csv) -> None:
    """Обидві моделі відхиляють той самий рядок з однаковим номером."""
    from kzp_lab06.errors import DataError as DictError
    from kzp_lab07.errors import DataError as ObjectError

    path = write_csv(
        "name,form,price,days_left,prescription\nА,гель,1,5,false\nБ,гель,-1,5,false\n"
    )

    with pytest.raises(DictError, match="Рядок 3: поле price"):
        load_records(path)
    with pytest.raises(ObjectError, match="Рядок 3: поле price"):
        load_medicines(path)
