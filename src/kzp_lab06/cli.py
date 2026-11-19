"""Командний рядок: CSV → звіт. Єдина межа, де помилки стають повідомленнями."""

import argparse
import io
import sys
from collections.abc import Sequence
from pathlib import Path

from kzp_lab06 import __version__
from kzp_lab06.analysis import DEFAULT_MAX_DAYS, make_summary
from kzp_lab06.errors import DataError
from kzp_lab06.loader import load_records
from kzp_lab06.reporting import format_report, write_report

EXIT_OK = 0
EXIT_WRITE_ERROR = 1
EXIT_DATA_ERROR = 2


def _use_utf8(stream: object) -> None:
    """Перемикає консольний потік на UTF-8 (консоль Windows може мати cp866)."""
    if isinstance(stream, io.TextIOWrapper):
        stream.reconfigure(encoding="utf-8")


def _non_negative_int(text: str) -> int:
    """Перетворює аргумент --max-days на невід'ємне ціле."""
    try:
        value = int(text)
    except ValueError as error:
        raise argparse.ArgumentTypeError(f"очікується ціле число: {text!r}") from error
    if value < 0:
        raise argparse.ArgumentTypeError("значення не може бути від'ємним")
    return value


def build_parser() -> argparse.ArgumentParser:
    """Описує аргументи CLI."""
    parser = argparse.ArgumentParser(
        prog="kzp-lab06",
        description="Читає CSV аптеки та записує текстовий звіт.",
    )
    parser.add_argument("input_csv", type=Path, help="вхідний CSV (UTF-8)")
    parser.add_argument("output_report", type=Path, help="файл звіту, що створюється")
    parser.add_argument(
        "--max-days",
        type=_non_negative_int,
        default=DEFAULT_MAX_DAYS,
        help=f"поріг відбору days_left (типово {DEFAULT_MAX_DAYS})",
    )
    parser.add_argument("--version", action="version", version=__version__)
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    """Запускає обробку; повертає 0, 2 (помилка даних) або 1 (помилка запису)."""
    _use_utf8(sys.stdout)
    _use_utf8(sys.stderr)
    args = build_parser().parse_args(argv)
    if args.input_csv.resolve() == args.output_report.resolve():
        print("Помилка: звіт не можна записати у вхідний файл", file=sys.stderr)
        return EXIT_DATA_ERROR
    try:
        records = load_records(args.input_csv)
    except DataError as error:
        print(f"Помилка даних: {error}", file=sys.stderr)
        return EXIT_DATA_ERROR
    text = format_report(make_summary(records, args.max_days))
    try:
        write_report(args.output_report, text)
    except OSError as error:
        print(f"Не вдалося записати звіт: {error}", file=sys.stderr)
        return EXIT_WRITE_ERROR
    print(f"Звіт записано: {args.output_report} (записів: {len(records)})")
    return EXIT_OK


if __name__ == "__main__":
    raise SystemExit(main())
