"""Тести аналізу ЛР № 7: спільний протокол і незмінний підсумок."""

import dataclasses

import pytest

from kzp_lab07.analysis import (
    average_price,
    count_by_form,
    least_available,
    make_summary,
    nearest_expiry,
    rank_by_availability,
    select_expiring,
)
from kzp_lab07.models import FreeMedicine, PrescriptionMedicine, ReportSummary

ITEMS = [
    FreeMedicine("Б", "сироп", 10.0, 40),
    FreeMedicine("А", "таблетки", 20.0, 30),
    PrescriptionMedicine("В", "сироп", 30.0, 5),
    FreeMedicine("Г", "гель", 40.0, 5),
]


class StockItem:
    """Чужий клас без наслідування Medicine, але з тим самим контрактом."""

    def __init__(self, name: str, days_left: int, score: float) -> None:
        self.name = name
        self.form = "пластир"
        self.price = 1.0
        self.days_left = days_left
        self.prescription = False
        self._score = score

    def availability_score(self) -> float:
        return self._score


def test_select_expiring_includes_boundary() -> None:
    """days_left == 30 входить у відбір, 40 — ні; порядок вхідний."""
    assert [item.name for item in select_expiring(ITEMS)] == ["А", "В", "Г"]


def test_count_by_form_counts_duplicates_and_sorts() -> None:
    """Дублікати рахуються, ключі відсортовано."""
    result = count_by_form(reversed(ITEMS))

    assert list(result.items()) == [("гель", 1), ("сироп", 2), ("таблетки", 1)]


def test_nearest_expiry_lists_all_ties() -> None:
    """Однаковий найменший термін у двох препаратів різних підтипів."""
    assert nearest_expiry(ITEMS) == (5, ("В", "Г"))


def test_rank_uses_subtype_formulas() -> None:
    """Ранжування викликає availability_score обох підтипів."""
    # Г і В мають однаковий термін 5 дн., але різні формули підтипів.
    assert rank_by_availability(ITEMS) == [
        ("Г", 1.37),
        ("В", 1.67),
        ("А", 8.22),
        ("Б", 10.96),
    ]


def test_duck_typing_without_inheritance() -> None:
    """Функції аналізу приймають будь-який об'єкт із контрактом Available."""
    items = [StockItem("Пластир", 3, 2.0), FreeMedicine("Йод", "розчин", 1, 365)]

    assert least_available(items) is items[0]
    assert select_expiring(items) == [items[0]]
    assert make_summary(items).prescription_count == 0


def test_empty_input_rules() -> None:
    """Порожній вхід: порожні колекції й None замість ділення на нуль."""
    assert select_expiring([]) == []
    assert count_by_form([]) == {}
    assert nearest_expiry([]) is None
    assert average_price([]) is None
    assert least_available([]) is None
    assert rank_by_availability([]) == []


def test_make_summary_returns_dataclass() -> None:
    """Повний підсумок — ReportSummary з упорядкованими кортежами."""
    summary = make_summary(ITEMS)

    assert isinstance(summary, ReportSummary)
    assert summary.total == 4
    assert summary.selected == ((5, "В"), (5, "Г"), (30, "А"))
    assert summary.selected_price == pytest.approx(90.0)
    assert summary.prescription_count == 1
    assert summary.forms == ("гель", "сироп", "таблетки")
    assert summary.average_price == pytest.approx(25.0)


def test_summary_is_immutable() -> None:
    """frozen=True забороняє змінювати поля."""
    summary = make_summary(ITEMS)

    with pytest.raises(dataclasses.FrozenInstanceError):
        summary.total = 5  # type: ignore[misc]


def test_summary_equality_and_hash() -> None:
    """Однакові дані дають рівні й хешовані підсумки незалежно від порядку."""
    first = make_summary(ITEMS)
    second = make_summary(list(reversed(ITEMS)))

    assert first == second
    assert hash(first) == hash(second)
    assert first != make_summary(ITEMS, max_days=5)


def test_summary_created_directly() -> None:
    """Dataclass створюється з полів і має типове availability."""
    summary = ReportSummary(0, 30, (), 0.0, (), (), 0, None, None)

    assert summary.availability == ()
    assert "total=0" in repr(summary)
