"""Спільні фікстури тестів."""

from pathlib import Path

import pytest


@pytest.fixture
def write_csv(tmp_path: Path):
    """Повертає функцію, що записує UTF-8 CSV у тимчасовий каталог."""

    def _write(text: str, name: str = "input.csv") -> Path:
        path = tmp_path / name
        path.write_text(text, encoding="utf-8", newline="")
        return path

    return _write
