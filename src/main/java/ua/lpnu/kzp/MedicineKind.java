package ua.lpnu.kzp;

/**
 * Категорія препарату за умовами відпуску (варіант 11 «Аптека»).
 *
 * <p>Набір категорій фіксований, тому замість довільного тексту або окремого прапорця
 * використовується enum. Значення порівнюються через {@code ==}: кожна константа існує
 * в одному екземплярі.
 */
public enum MedicineKind {
    /** Препарат відпускається лише за рецептом лікаря. */
    PRESCRIPTION("рецептурний", "за рецептом", true),
    /** Препарат вільного продажу. */
    FREE("безрецептурний", "без рецепта", false);

    private final String label;
    private final String saleLabel;
    private final boolean requiresPrescription;

    /* Конструктор enum викликається JVM один раз для кожної константи. */
    MedicineKind(String label, String saleLabel, boolean requiresPrescription) {
        this.label = label;
        this.saleLabel = saleLabel;
        this.requiresPrescription = requiresPrescription;
    }

    /**
     * Повертає назву категорії для звіту.
     *
     * @return назва категорії, наприклад «рецептурний»
     */
    public String label() {
        return label;
    }

    /**
     * Повертає умову відпуску у формі, яку використовує {@link Medicine#toString()}.
     *
     * @return «за рецептом» або «без рецепта»
     */
    public String saleLabel() {
        return saleLabel;
    }

    /**
     * Повідомляє, чи потрібен рецепт для препаратів цієї категорії.
     *
     * @return {@code true} для {@link #PRESCRIPTION}
     */
    public boolean requiresPrescription() {
        return requiresPrescription;
    }

    /**
     * Повертає категорію за ознакою рецепта з CSV-рядка.
     *
     * @param prescription значення поля {@code prescription}
     * @return {@link #PRESCRIPTION} для {@code true}, інакше {@link #FREE}
     */
    public static MedicineKind of(boolean prescription) {
        return prescription ? PRESCRIPTION : FREE;
    }
}
