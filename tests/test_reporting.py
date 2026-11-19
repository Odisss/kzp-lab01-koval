"""Тести формування та запису звіту."""

from pathlib import Path

from kzp_lab06.analysis import make_summary
from kzp_lab06.loader import load_records
from kzp_lab06.reporting import format_report, write_report

DATA_DIR = Path(__file__).resolve().parents[1] / "data"


def test_report_matches_expected_file() -> None:
    """Звіт для контрольного CSV збігається з data/lab06-expected-report.txt."""
    records = load_records(DATA_DIR / "lab06.csv")
    expected = (DATA_DIR / "lab06-expected-report.txt").read_text(encoding="utf-8")

    assert format_report(make_summary(records)) == expected


def test_report_does_not_depend_on_row_order() -> None:
    """Перестановка рядків не змінює текст звіту."""
    records = load_records(DATA_DIR / "lab06.csv")

    assert format_report(make_summary(records)) == format_report(
        make_summary(list(reversed(records)))
    )


def test_empty_report() -> None:
    """Звіт для порожнього входу пояснює відсутність даних."""
    text = format_report(make_summary([]))

    assert "Усього записів: 0\n" in text
    assert "Найближчий термін: немає даних\n" in text
    assert "Середня ціна: немає даних\n" in text
    assert text.endswith("\n")


def test_write_report_utf8_lf_and_creates_dir(tmp_path: Path) -> None:
    """Запис через тимчасовий каталог: UTF-8, LF, вкладений каталог створюється."""
    output = tmp_path / "reports" / "summary.txt"

    write_report(output, "Готово\nДругий рядок\n")

    assert output.read_bytes() == "Готово\nДругий рядок\n".encode()
