"""Тести моделі: властивості, спеціальні методи, підтипи та протокол."""

import math

import pytest

from kzp_lab07.models import (
    Available,
    FreeMedicine,
    Medicine,
    PrescriptionMedicine,
)


def _free(days: int = 12, price: float = 32.2) -> FreeMedicine:
    return FreeMedicine("Цитрамон", "таблетки", price, days)


# --- @property і setter ---------------------------------------------------


def test_constructor_normalizes_text() -> None:
    """Назва очищується від пробілів, форма — у нижньому регістрі."""
    medicine = FreeMedicine("  Цитрамон ", "Таблетки", 32.2, 12)

    assert (medicine.name, medicine.form) == ("Цитрамон", "таблетки")


def test_setter_accepts_valid_value() -> None:
    """Коректне нове значення зберігається."""
    medicine = _free()
    medicine.price = 40
    medicine.days_left = 7

    assert (medicine.price, medicine.days_left) == (40.0, 7)
    assert isinstance(medicine.price, float)


def test_setter_zero_is_boundary_valid() -> None:
    """Нуль — допустима межа для ціни й терміну."""
    medicine = _free()
    medicine.price = 0
    medicine.days_left = 0

    assert (medicine.price, medicine.days_left) == (0.0, 0)


@pytest.mark.parametrize(
    ("field", "value", "error", "message"),
    [
        ("price", -0.01, ValueError, "price не може бути від'ємним"),
        ("price", math.inf, ValueError, "price має бути скінченним"),
        ("price", math.nan, ValueError, "price має бути скінченним"),
        ("price", "10", TypeError, "price має бути числом"),
        ("price", True, TypeError, "price має бути числом"),
        ("days_left", -1, ValueError, "days_left не може бути від'ємним"),
        ("days_left", 2.5, TypeError, "days_left має бути цілим"),
        ("days_left", False, TypeError, "days_left має бути цілим"),
        ("name", "   ", ValueError, "name порожнє"),
        ("form", "", ValueError, "form порожнє"),
    ],
)
def test_setter_rejects_invalid_value(field, value, error, message) -> None:
    """Помилкове значення відхиляється, а попередній стан не змінюється."""
    medicine = _free()
    before = repr(medicine)

    with pytest.raises(error, match=message):
        setattr(medicine, field, value)
    assert repr(medicine) == before


def test_constructor_uses_setters() -> None:
    """Перевірка діє вже під час створення об'єкта."""
    with pytest.raises(ValueError, match="days_left не може бути від'ємним"):
        PrescriptionMedicine("Інсулін", "розчин", 520.0, -1)


def test_prescription_is_read_only_property() -> None:
    """Ознака рецепта задається підтипом і не має setter-а."""
    medicine = _free()

    with pytest.raises(AttributeError):
        medicine.prescription = True  # type: ignore[misc]


# --- __str__, __repr__, __eq__ --------------------------------------------


def test_str_is_for_reader() -> None:
    """str — коротке подання з назвою, формою, терміном і ціною."""
    assert str(_free()) == "Цитрамон (таблетки) — 12 дн., 32.20 грн"


def test_repr_names_concrete_subtype() -> None:
    """repr містить назву підтипу й усі поля."""
    medicine = PrescriptionMedicine("Інсулін", "розчин", 520, 90)

    assert repr(medicine) == (
        "PrescriptionMedicine(name='Інсулін', form='розчин', price=520.0, days_left=90)"
    )


def test_duplicate_objects_are_equal() -> None:
    """Дві однакові партії рівні, але це різні об'єкти."""
    first, second = _free(), _free()

    assert first == second
    assert first is not second


def test_different_state_or_subtype_is_not_equal() -> None:
    """Інший термін або інший підтип з тим самим станом — не рівні."""
    assert _free() != _free(days=13)
    assert FreeMedicine("А", "гель", 1, 5) != PrescriptionMedicine("А", "гель", 1, 5)


def test_eq_with_foreign_type_returns_not_implemented() -> None:
    """Порівняння з чужим типом повертає NotImplemented, а == дає False."""
    medicine = _free()

    assert medicine.__eq__("Цитрамон") is NotImplemented
    assert medicine != "Цитрамон"


def test_mutable_medicine_is_unhashable() -> None:
    """Змінюваний об'єкт не може бути ключем словника."""
    with pytest.raises(TypeError):
        hash(_free())


# --- Підтипи та протокол --------------------------------------------------


def test_subtypes_have_different_scores() -> None:
    """Однаковий стан, різні формули підтипів."""
    free = FreeMedicine("А", "гель", 10.0, 90)
    rx = PrescriptionMedicine("А", "гель", 10.0, 90)

    assert free.availability_score() == pytest.approx(24.66)
    assert rx.availability_score() == pytest.approx(30.0)


@pytest.mark.parametrize(
    ("medicine", "expected"),
    [
        (FreeMedicine("А", "гель", 1, 0), 0.0),
        (FreeMedicine("А", "гель", 1, 365), 100.0),
        (FreeMedicine("А", "гель", 1, 420), 100.0),
        (PrescriptionMedicine("А", "гель", 1, 0), 0.0),
        (PrescriptionMedicine("А", "гель", 1, 180), 60.0),
        (PrescriptionMedicine("А", "гель", 1, 365), 60.0),
    ],
)
def test_score_bounds(medicine: Medicine, expected: float) -> None:
    """Нульовий термін дає 0, термін понад горизонт — максимум підтипу."""
    assert medicine.availability_score() == pytest.approx(expected)


def test_subtypes_define_prescription_flag() -> None:
    """Ознака рецепта — поведінка підтипу."""
    assert PrescriptionMedicine("А", "гель", 1, 5).prescription is True
    assert FreeMedicine("А", "гель", 1, 5).prescription is False


def test_base_class_does_not_define_score() -> None:
    """Базовий клас лише оголошує операцію."""
    with pytest.raises(NotImplementedError):
        Medicine("А", "гель", 1, 5).availability_score()


def test_super_init_validates_in_subtype() -> None:
    """Підтип успадковує перевірки базового __init__."""
    with pytest.raises(ValueError, match="price не може бути від'ємним"):
        FreeMedicine("А", "гель", -1, 5)


def test_both_subtypes_follow_protocol() -> None:
    """Обидва підтипи задовольняють runtime-перевірку протоколу Available."""
    assert isinstance(_free(), Available)
    assert isinstance(PrescriptionMedicine("А", "гель", 1, 5), Available)
    assert not isinstance("рядок", Available)
