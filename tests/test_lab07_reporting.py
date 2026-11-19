"""Тести звіту ЛР № 7 і контекстного менеджера ReportWriter."""

from pathlib import Path

import pytest

from kzp_lab07.analysis import make_summary
from kzp_lab07.loader import load_medicines
from kzp_lab07.reporting import ReportWriter, format_report, write_report

DATA_DIR = Path(__file__).resolve().parents[1] / "data"


def test_lab07_report_matches_expected_file() -> None:
    """Звіт із індексом для data/lab07.csv збігається з еталоном ЛР № 7."""
    medicines = load_medicines(DATA_DIR / "lab07.csv")
    expected = (DATA_DIR / "lab07-expected-report.txt").read_text(encoding="utf-8")

    assert format_report(make_summary(medicines), scores=True) == expected


def test_report_does_not_depend_on_row_order() -> None:
    """Перестановка рядків не змінює текст звіту."""
    medicines = load_medicines(DATA_DIR / "lab07.csv")

    assert format_report(make_summary(medicines), scores=True) == format_report(
        make_summary(list(reversed(medicines))), scores=True
    )


def test_empty_report_with_scores() -> None:
    """Порожній вхід пояснює відсутність даних у кожному розділі."""
    text = format_report(make_summary([]), scores=True)

    assert "Усього записів: 0\n" in text
    assert "Середня ціна: немає даних\n" in text
    assert text.endswith("Індекс доступності (від найменшого):\n  немає записів\n")


def test_write_report_utf8_lf_and_creates_dir(tmp_path: Path) -> None:
    """UTF-8, LF, вкладений каталог створюється, тимчасових файлів не лишається."""
    output = tmp_path / "reports" / "summary.txt"

    write_report(output, "Готово\nДругий рядок\n")

    assert output.read_bytes() == "Готово\nДругий рядок\n".encode()
    assert [path.name for path in output.parent.iterdir()] == ["summary.txt"]


def test_writer_closes_and_cleans_up_on_exception(tmp_path: Path) -> None:
    """Виняток у with: потік закрито, тимчасовий файл видалено, старий звіт цілий."""
    output = tmp_path / "summary.txt"
    output.write_text("старий звіт\n", encoding="utf-8")
    writer = ReportWriter(output)

    with pytest.raises(RuntimeError, match="збій"):
        with writer as target:
            target.write("неповний ")
            raise RuntimeError("збій")

    assert writer.closed
    assert output.read_text(encoding="utf-8") == "старий звіт\n"
    assert [path.name for path in tmp_path.iterdir()] == ["summary.txt"]


def test_writer_replace_failure_cleans_up(tmp_path: Path) -> None:
    """Ціль — каталог: OSError передається далі, тимчасовий файл прибрано."""
    target_dir = tmp_path / "report.txt"
    target_dir.mkdir()

    with pytest.raises(OSError):
        write_report(target_dir, "текст\n")
    assert [path.name for path in tmp_path.iterdir()] == ["report.txt"]
