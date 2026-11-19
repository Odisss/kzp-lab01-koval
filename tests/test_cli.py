"""Тести командного рядка: коди завершення та повідомлення."""

import subprocess
import sys
from pathlib import Path

import pytest

from kzp_lab06.cli import EXIT_DATA_ERROR, EXIT_OK, EXIT_WRITE_ERROR, main

DATA_DIR = Path(__file__).resolve().parents[1] / "data"
HEADER = "name,form,price,days_left,prescription\n"


def test_success(tmp_path: Path, capsys) -> None:
    """Коректний CSV: код 0, звіт записано, повідомлення в stdout."""
    output = tmp_path / "summary.txt"

    code = main([str(DATA_DIR / "lab06.csv"), str(output)])

    assert code == EXIT_OK
    assert output.read_text(encoding="utf-8").startswith("Звіт аптеки")
    assert "записів: 7" in capsys.readouterr().out


def test_data_error_gives_code_2_and_no_report(tmp_path: Path, capsys) -> None:
    """Пошкоджений CSV: код 2, повідомлення в stderr, звіт не створено."""
    output = tmp_path / "summary.txt"

    code = main([str(DATA_DIR / "lab06-invalid.csv"), str(output)])

    assert code == EXIT_DATA_ERROR
    assert "Рядок 3: поле price" in capsys.readouterr().err
    assert not output.exists()


def test_max_days_option(write_csv, tmp_path: Path) -> None:
    """--max-days змінює поріг відбору."""
    path = write_csv(HEADER + "А,гель,1,40,false\n")
    output = tmp_path / "summary.txt"

    assert main([str(path), str(output), "--max-days", "45"]) == EXIT_OK
    assert "Термін спливає за <= 45 дн.: 1" in output.read_text(encoding="utf-8")


def test_negative_max_days_is_usage_error(tmp_path: Path) -> None:
    """Від'ємний поріг відхиляє argparse (код 2)."""
    with pytest.raises(SystemExit) as caught:
        main([str(DATA_DIR / "lab06.csv"), str(tmp_path / "r.txt"), "--max-days=-1"])
    assert caught.value.code == 2


def test_refuses_to_overwrite_input(write_csv) -> None:
    """Вихідний файл не може збігатися з вхідним."""
    path = write_csv(HEADER)

    assert main([str(path), str(path)]) == EXIT_DATA_ERROR
    assert path.read_text(encoding="utf-8") == HEADER


def test_write_error_gives_code_1(tmp_path: Path, capsys) -> None:
    """Звіт неможливо записати (шлях — каталог): код 1."""
    code = main([str(DATA_DIR / "lab06.csv"), str(tmp_path)])

    assert code == EXIT_WRITE_ERROR
    assert "Не вдалося записати звіт" in capsys.readouterr().err


def test_module_entry_point(tmp_path: Path) -> None:
    """python -m kzp_lab06.cli у окремому процесі повертає код 2 для помилки."""
    result = subprocess.run(
        [
            sys.executable,
            "-m",
            "kzp_lab06.cli",
            str(DATA_DIR / "lab06-invalid.csv"),
            str(tmp_path / "r.txt"),
        ],
        capture_output=True,
        check=False,
    )

    assert result.returncode == EXIT_DATA_ERROR
    assert "Рядок 3" in result.stderr.decode("utf-8")
