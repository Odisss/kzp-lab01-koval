"""Аптека (варіант 11): об'єктна модель препаратів, аналіз і текстовий звіт."""

from kzp_lab07.models import (
    Available,
    FreeMedicine,
    Medicine,
    PrescriptionMedicine,
    ReportSummary,
)

__version__ = "1.6.0"

__all__ = [
    "Available",
    "FreeMedicine",
    "Medicine",
    "PrescriptionMedicine",
    "ReportSummary",
    "__version__",
]
