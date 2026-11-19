"""Предметна модель аптеки: сутність, підтипи, протокол і підсумок.

Модуль не читає файлів і не формує звіт: він лише зберігає стан препарату,
перевіряє його інваріанти та обчислює поліморфну операцію availability_score.
"""

import math
from dataclasses import dataclass
from typing import ClassVar, Protocol, runtime_checkable

#: Горизонт безрецептурного препарату: термін понад рік уважається «повним».
FREE_HORIZON_DAYS = 365

#: Горизонт рецептурного препарату: курс лікування рідко довший за пів року.
PRESCRIPTION_HORIZON_DAYS = 180

#: Найбільший індекс рецептурного препарату: потреба в рецепті знижує доступність.
PRESCRIPTION_MAX_SCORE = 60.0


@runtime_checkable
class Available(Protocol):
    """Контракт об'єктів, з якими працює analysis.

    Функції аналізу знають лише ці властивості й метод, тому будь-який об'єкт
    із таким набором (зокрема новий підтип) обробляється без змін у analysis.
    """

    @property
    def name(self) -> str:
        """Назва препарату."""
        ...

    @property
    def form(self) -> str:
        """Лікарська форма в нижньому регістрі."""
        ...

    @property
    def price(self) -> float:
        """Ціна в гривнях."""
        ...

    @property
    def days_left(self) -> int:
        """Кількість днів до кінця терміну придатності."""
        ...

    @property
    def prescription(self) -> bool:
        """Чи потрібен рецепт."""
        ...

    def availability_score(self) -> float:
        """Індекс доступності 0–100."""
        ...


def _required_text(value: str, field: str) -> str:
    """Повертає непорожній рядок без пробілів по краях."""
    if not isinstance(value, str):
        raise TypeError(f"поле {field} має бути рядком")
    text = value.strip()
    if not text:
        raise ValueError(f"поле {field} порожнє")
    return text


class Medicine:
    """Базовий препарат зі спільним перевіреним станом.

    Інваріанти: назва й форма непорожні, ціна — скінченне число не менше 0,
    days_left — ціле не менше 0. Вони перевіряються в setter-ах, тож діють і
    під час створення, і під час пізнішої зміни значення.
    """

    #: Ознака рецепта визначається підтипом, а не окремим полем стану.
    PRESCRIPTION: ClassVar[bool] = False

    def __init__(self, name: str, form: str, price: float, days_left: int) -> None:
        """Створює препарат і перевіряє всі поля через setter-и."""
        self.name = name
        self.form = form
        self.price = price
        self.days_left = days_left

    @property
    def name(self) -> str:
        """Повертає назву препарату."""
        return self._name

    @name.setter
    def name(self, value: str) -> None:
        """Встановлює непорожню назву без пробілів по краях."""
        self._name = _required_text(value, "name")

    @property
    def form(self) -> str:
        """Повертає лікарську форму в нижньому регістрі."""
        return self._form

    @form.setter
    def form(self, value: str) -> None:
        """Встановлює непорожню форму; регістр нормалізується."""
        self._form = _required_text(value, "form").lower()

    @property
    def price(self) -> float:
        """Повертає ціну в гривнях."""
        return self._price

    @price.setter
    def price(self, value: float) -> None:
        """Встановлює скінченну невід'ємну ціну."""
        # bool є підкласом int, але True не є ціною.
        if isinstance(value, bool) or not isinstance(value, int | float):
            raise TypeError("поле price має бути числом")
        if not math.isfinite(value):
            raise ValueError("поле price має бути скінченним числом")
        if value < 0:
            raise ValueError("поле price не може бути від'ємним")
        # Записується внутрішнє поле, тому setter не викликає сам себе.
        self._price = float(value)

    @property
    def days_left(self) -> int:
        """Повертає кількість днів до кінця терміну."""
        return self._days_left

    @days_left.setter
    def days_left(self, value: int) -> None:
        """Встановлює ціле невід'ємне значення днів."""
        if isinstance(value, bool) or not isinstance(value, int):
            raise TypeError("поле days_left має бути цілим числом")
        if value < 0:
            raise ValueError("поле days_left не може бути від'ємним")
        self._days_left = value

    @property
    def prescription(self) -> bool:
        """Повертає ознаку рецепта, задану підтипом."""
        return self.PRESCRIPTION

    def is_expiring(self, max_days: int) -> bool:
        """Перевіряє, чи спливає термін не пізніше ніж за max_days днів."""
        return self.days_left <= max_days

    def availability_score(self) -> float:
        """Оголошує операцію індексу доступності 0–100 для підтипів."""
        raise NotImplementedError(f"{type(self).__name__} не визначає індекс")

    def _state(self) -> tuple[str, str, float, int]:
        """Повертає стан, що бере участь у рівності та repr."""
        return self.name, self.form, self.price, self.days_left

    def __str__(self) -> str:
        """Повертає коротке подання для читача."""
        return f"{self.name} ({self.form}) — {self.days_left} дн., {self.price:.2f} грн"

    def __repr__(self) -> str:
        """Повертає діагностичне подання з назвою конкретного підтипу."""
        return (
            f"{type(self).__name__}(name={self.name!r}, form={self.form!r}, "
            f"price={self.price!r}, days_left={self.days_left!r})"
        )

    def __eq__(self, other: object) -> bool:
        """Порівнює препарати одного підтипу за повним станом."""
        if not isinstance(other, Medicine):
            return NotImplemented
        return type(self) is type(other) and self._state() == other._state()

    # Об'єкт змінюваний (є setter-и), тому він не може бути ключем словника.
    __hash__ = None  # type: ignore[assignment]


class PrescriptionMedicine(Medicine):
    """Рецептурний препарат: продаж лише за рецептом знижує доступність."""

    PRESCRIPTION = True

    def availability_score(self) -> float:
        """Обчислює індекс: до 60 балів, повний уже за 180 днів терміну.

        Препарат із нульовим терміном видати неможливо, тому індекс 0.
        """
        part = min(self.days_left, PRESCRIPTION_HORIZON_DAYS)
        return round(part / PRESCRIPTION_HORIZON_DAYS * PRESCRIPTION_MAX_SCORE, 2)


class FreeMedicine(Medicine):
    """Безрецептурний препарат: доступність визначає лише термін."""

    PRESCRIPTION = False

    def availability_score(self) -> float:
        """Обчислює індекс: до 100 балів, повний за 365 днів терміну."""
        part = min(self.days_left, FREE_HORIZON_DAYS)
        return round(part / FREE_HORIZON_DAYS * 100, 2)


@dataclass(frozen=True)
class ReportSummary:
    """Незмінні показники звіту варіанта 11.

    Усі колекції — кортежі, вже впорядковані для детермінованого звіту.
    """

    total: int
    max_days: int
    #: Пари (days_left, name) препаратів, у яких термін спливає.
    selected: tuple[tuple[int, str], ...]
    selected_price: float
    forms: tuple[str, ...]
    #: Пари (форма, кількість), відсортовані за формою.
    count_by_form: tuple[tuple[str, int], ...]
    prescription_count: int
    #: Найменший термін і назви з ним або None для порожнього входу.
    nearest: tuple[int, tuple[str, ...]] | None
    average_price: float | None
    #: Пари (name, availability_score) від найменш доступного.
    availability: tuple[tuple[str, float], ...] = ()
