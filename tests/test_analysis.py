"""Тести функцій аналізу."""

import pytest

from kzp_lab06.analysis import (
    average_price,
    count_by_form,
    make_summary,
    nearest_expiry,
    select_expiring,
    summarize,
    unique_forms,
)


def _record(name: str, form: str, price: float, days: int, rx: bool = False):
    return {
        "name": name,
        "form": form,
        "price": price,
        "days_left": days,
        "prescription": rx,
    }


RECORDS = [
    _record("Б", "сироп", 10.0, 40),
    _record("А", "таблетки", 20.0, 30),
    _record("В", "сироп", 30.0, 5, rx=True),
    _record("Г", "гель", 40.0, 5),
]


def test_select_expiring_includes_boundary() -> None:
    """days_left == 30 входить у відбір, 40 — ні; порядок вхідний."""
    assert [r["name"] for r in select_expiring(RECORDS)] == ["А", "В", "Г"]


def test_select_expiring_can_be_empty() -> None:
    """Поріг, нижчий за всі терміни, дає порожній відбір."""
    assert select_expiring(RECORDS, max_days=1) == []


def test_unique_forms_removes_duplicates() -> None:
    """Повторна форма «сироп» потрапляє в множину один раз."""
    assert unique_forms(RECORDS) == {"сироп", "таблетки", "гель"}


def test_count_by_form_sorted_and_counts_duplicates() -> None:
    """Дублікати рахуються, ключі відсортовано незалежно від вхідного порядку."""
    result = count_by_form(list(reversed(RECORDS)))

    assert result == {"гель": 1, "сироп": 2, "таблетки": 1}
    assert list(result) == ["гель", "сироп", "таблетки"]


def test_duplicate_records_are_counted_twice() -> None:
    """Дві однакові партії — два записи, але одна форма."""
    records = [_record("А", "гель", 1.0, 5), _record("А", "гель", 1.0, 5)]

    assert count_by_form(records) == {"гель": 2}
    assert unique_forms(records) == {"гель"}


def test_nearest_expiry_lists_all_ties() -> None:
    """Однаковий найменший термін у двох препаратів — обидва в результаті."""
    assert nearest_expiry(RECORDS) == (5, ["В", "Г"])


def test_average_price() -> None:
    """Середня ціна чотирьох записів."""
    assert average_price(RECORDS) == pytest.approx(25.0)


def test_empty_input_rules() -> None:
    """Порожній вхід: порожні колекції та None замість ділення на нуль."""
    assert select_expiring([]) == []
    assert unique_forms([]) == set()
    assert count_by_form([]) == {}
    assert nearest_expiry([]) is None
    assert average_price([]) is None


def test_summarize_args_default_and_kwargs() -> None:
    """*metrics, типовий max_days=30 і **labels."""
    result = summarize(RECORDS, "price", "days_left", price="Сума")

    assert result == {"count": 3, "Сума": 90.0, "days_left": 40.0}


def test_summarize_custom_threshold() -> None:
    """Іменований параметр max_days змінює відбір."""
    assert summarize(RECORDS, "price", max_days=5) == {"count": 2, "price": 70.0}


def test_summarize_empty_selection_gives_zero() -> None:
    """Відомі метрики порожнього відбору дорівнюють 0.0."""
    assert summarize([], "price") == {"count": 0, "price": 0.0}


def test_summarize_rejects_unknown_metric() -> None:
    """Невідома метрика — ValueError навіть для порожнього входу."""
    with pytest.raises(ValueError, match="Невідома метрика: name"):
        summarize([], "name")


def test_summarize_rejects_stray_label() -> None:
    """Підпис без відповідної метрики — ValueError."""
    with pytest.raises(ValueError, match="Підпис для невибраної метрики"):
        summarize(RECORDS, "price", days_left="Дні")


def test_make_summary() -> None:
    """Повний підсумок для звіту."""
    summary = make_summary(RECORDS)

    assert summary["total"] == 4
    assert summary["selected"] == [(5, "В"), (5, "Г"), (30, "А")]
    assert summary["selected_price"] == 90.0
    assert summary["prescription_count"] == 1
    assert summary["forms"] == ["гель", "сироп", "таблетки"]
