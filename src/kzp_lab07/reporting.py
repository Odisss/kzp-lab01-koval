"""Формування та безпечний запис текстового звіту. Модуль не читає CSV."""

import os
import tempfile
from pathlib import Path
from types import TracebackType
from typing import TextIO

from kzp_lab07.models import ReportSummary


def _money(value: float) -> str:
    """Форматує суму з двома знаками після крапки й позначкою гривні."""
    return f"{value:.2f} грн"


def format_report(summary: ReportSummary, *, scores: bool = False) -> str:
    """Перетворює ReportSummary на детермінований текст звіту.

    Без scores текст збігається зі звітом ЛР № 6 (data/lab06-expected-report.txt).
    Із scores додається розділ індексу доступності.
    """
    lines = [
        "Звіт аптеки (варіант 11)",
        f"Усього записів: {summary.total}",
        f"Рецептурних: {summary.prescription_count}",
        f"Лікарські форми ({len(summary.forms)}): "
        f"{', '.join(summary.forms) or 'немає'}",
        "Кількість за формою:",
    ]
    lines += [f"  {form}: {count}" for form, count in summary.count_by_form]
    if not summary.count_by_form:
        lines.append("  немає записів")
    lines.append(
        f"Термін спливає за <= {summary.max_days} дн.: {len(summary.selected)}"
    )
    lines += [f"  {name} — {days} дн." for days, name in summary.selected]
    if not summary.selected:
        lines.append("  немає таких препаратів")
    lines.append(
        f"Вартість препаратів із терміном, що спливає: {_money(summary.selected_price)}"
    )
    if summary.nearest is None:
        lines.append("Найближчий термін: немає даних")
    else:
        days, names = summary.nearest
        lines.append(f"Найближчий термін: {days} дн. ({', '.join(names)})")
    if summary.average_price is None:
        lines.append("Середня ціна: немає даних")
    else:
        lines.append(f"Середня ціна: {_money(summary.average_price)}")
    if scores:
        lines.append("Індекс доступності (від найменшого):")
        lines += [f"  {name}: {score:.2f}" for name, score in summary.availability]
        if not summary.availability:
            lines.append("  немає записів")
    return "\n".join(lines) + "\n"


class ReportWriter:
    """Контекстний менеджер безпечного запису звіту.

    Текст пишеться в тимчасовий файл у тому ж каталозі. Лише після успішного
    виходу з with тимчасовий файл атомарно замінює цільовий. Якщо в тілі with
    виникає виняток, файл закривається й видаляється, а попередній звіт (якщо
    був) лишається без змін. Виняток не поглинається.
    """

    def __init__(self, path: Path) -> None:
        """Запам'ятовує цільовий шлях; файл відкривається в __enter__."""
        self.path = path
        self._temp_path: Path | None = None
        self._stream: TextIO | None = None

    @property
    def closed(self) -> bool:
        """Чи закрито потік запису (або його ще не відкривали)."""
        return self._stream is None or self._stream.closed

    def __enter__(self) -> TextIO:
        """Створює каталог і тимчасовий файл UTF-8 з LF; повертає потік."""
        self.path.parent.mkdir(parents=True, exist_ok=True)
        descriptor, name = tempfile.mkstemp(
            dir=self.path.parent, prefix=f".{self.path.name}.", suffix=".tmp"
        )
        self._temp_path = Path(name)
        self._stream = os.fdopen(descriptor, "w", encoding="utf-8", newline="\n")
        return self._stream

    def __exit__(
        self,
        exc_type: type[BaseException] | None,
        exc: BaseException | None,
        traceback: TracebackType | None,
    ) -> None:
        """Закриває потік; за успіху замінює ціль, за помилки прибирає файл."""
        assert self._stream is not None and self._temp_path is not None
        self._stream.close()
        if exc_type is None:
            try:
                os.replace(self._temp_path, self.path)
            except OSError:
                self._temp_path.unlink(missing_ok=True)
                raise
        else:
            self._temp_path.unlink(missing_ok=True)


def write_report(path: Path, text: str) -> None:
    """Записує звіт у файл UTF-8 з LF через ReportWriter."""
    with ReportWriter(path) as target:
        target.write(text)
