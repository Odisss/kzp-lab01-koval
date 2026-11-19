"""Тести командного рядка ЛР № 7."""

import subprocess
import sys
from pathlib import Path

from kzp_lab07.cli import EXIT_DATA_ERROR, EXIT_OK, EXIT_WRITE_ERROR, main

DATA_DIR = Path(__file__).resolve().parents[1] / "data"


def test_success_matches_lab06_report(tmp_path: Path, capsys) -> None:
    """Без --scores CLI ЛР № 7 дає звіт, ідентичний еталону ЛР № 6."""
    output = tmp_path / "summary.txt"

    code = main([str(DATA_DIR / "lab06.csv"), str(output)])

    assert code == EXIT_OK
    assert output.read_bytes() == (DATA_DIR / "lab06-expected-report.txt").read_bytes()
    assert "записів: 7" in capsys.readouterr().out


def test_scores_option(tmp_path: Path) -> None:
    """--scores додає розділ індексу доступності."""
    output = tmp_path / "summary.txt"

    assert main([str(DATA_DIR / "lab07.csv"), str(output), "--scores"]) == EXIT_OK
    assert output.read_bytes() == (DATA_DIR / "lab07-expected-report.txt").read_bytes()


def test_data_error_gives_code_2_and_no_report(tmp_path: Path, capsys) -> None:
    """Пошкоджений CSV: код 2, рядок помилки в stderr, звіт не створено."""
    output = tmp_path / "summary.txt"

    code = main([str(DATA_DIR / "lab07-invalid.csv"), str(output)])

    assert code == EXIT_DATA_ERROR
    assert "Рядок 3: поле price не може бути від'ємним" in capsys.readouterr().err
    assert not output.exists()


def test_write_error_gives_code_1(tmp_path: Path, capsys) -> None:
    """Звіт неможливо записати (шлях — каталог): код 1."""
    code = main([str(DATA_DIR / "lab06.csv"), str(tmp_path)])

    assert code == EXIT_WRITE_ERROR
    assert "Не вдалося записати звіт" in capsys.readouterr().err


def test_import_does_not_run_cli() -> None:
    """Імпорт модуля не запускає main (захист if __name__ == "__main__")."""
    result = subprocess.run(
        [sys.executable, "-c", "import kzp_lab07.cli"],
        capture_output=True,
        check=False,
    )

    assert result.returncode == 0
    assert result.stdout == b""


def test_module_entry_point(tmp_path: Path) -> None:
    """python -m kzp_lab07.cli в окремому процесі повертає код 2 для помилки."""
    result = subprocess.run(
        [
            sys.executable,
            "-m",
            "kzp_lab07.cli",
            str(DATA_DIR / "lab07-invalid.csv"),
            str(tmp_path / "r.txt"),
        ],
        capture_output=True,
        check=False,
    )

    assert result.returncode == EXIT_DATA_ERROR
    assert "Рядок 3" in result.stderr.decode("utf-8")
