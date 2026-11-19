# kzp-lab01 — «Аптека» (варіант 11)

Лабораторні роботи № 1–6 з курсу «Кросплатформні засоби програмування».

- ЛР № 1 (`v1.0.0`) — консольний продукт, Maven, тести, SpotBugs, CI на трьох ОС.
- ЛР № 2 (`v1.1.0`) — предметна частина перебудована на клас-сутність `Medicine`,
  фабрику `fromCsv` і record `ExpiryPrescription`; зовнішній звіт не змінився.
- ЛР № 3 (`v1.2.0`) — ієрархія: абстрактний `Medicine`, підтипи `PrescriptionMedicine` і
  `FreeMedicine`, enum `MedicineKind`, поліморфна оцінка доступності; звіт не змінився.
- ЛР № 4 (`v1.3.0`) — цикли замінено на Stream API, клас запитів `MedicineQueries`,
  опція `--queries`; звіт не змінився.
- ЛР № 5 (`v1.4.0`) — параметризоване сховище `Repository<T>`, анотація `@CsvColumn`,
  універсальний рефлексивний CSV-експортер, документний CSV-парсер, власний виняток
  `DataStorageException`, опції `--export` і `--import`; звіт не змінився.
- ЛР № 6 (`v1.5.0`) — окремий Python-етап: пакет `kzp_lab06` (CSV → типізовані записи →
  текстовий звіт), pytest, ruff, wheel; див. розділ [Python-пакет (ЛР № 6)](#python-пакет-лр--6).
  Java-частина не змінювалася.

## Призначення

Консольна програма читає записи про препарати з `data/input.csv`, перевіряє їх,
обчислює чотири показники та формує звіт у консоль і текстовий файл. Хибні рядки не
зупиняють програму: для кожного виводиться номер рядка та причина пропуску.

## Формат входу

Файл у кодуванні **UTF-8**, один запис у рядку, поля розділені `;`, десяткова крапка `.`:

```text
name;form;price;daysToExpire;prescription
```

| Поле           | Тип     | Вимога                                      |
|----------------|---------|---------------------------------------------|
| `name`         | String  | не порожнє                                  |
| `form`         | String  | не порожнє (таблетки, сироп, гель…)         |
| `price`        | double  | число з крапкою, не менше 0                 |
| `daysToExpire` | int     | ціле число, не менше 0                      |
| `prescription` | boolean | `true` або `false`                          |

Файл не містить заголовка й коментарів: кожен непорожній рядок — дані. Порожній рядок,
неправильна кількість полів, порожнє текстове поле, нечислове чи від'ємне значення —
пропускаються з поясненням.

## Показники

1. кількість коректних записів;
2. середня ціна (грн);
3. найменший термін придатності (дні);
4. кількість рецептурних препаратів.

## Збірка та запуск

Потрібні JDK 21 і Git. Maven встановлювати не потрібно — використовується Maven Wrapper.

```text
./mvnw verify          # компіляція, тести, JAR, SpotBugs
./mvnw package         # лише JAR
java -jar target/lab01-1.4.0.jar --version
java -jar target/lab01-1.4.0.jar
java -jar target/lab01-1.4.0.jar --availability
java -jar target/lab01-1.4.0.jar --queries
java -jar target/lab01-1.4.0.jar --input data/lab05.csv --export out/medicines.csv
java -jar target/lab01-1.4.0.jar --import out/medicines.csv
```

На Windows замість `./mvnw` використовуйте `mvnw.cmd`.

Параметри:

| Параметр          | Значення                                          |
|-------------------|---------------------------------------------------|
| `--input <файл>`  | вхідний CSV (типово `data/input.csv`)             |
| `--output <файл>` | файл звіту (типово `out/report.txt`)              |
| `--availability`  | додатково вивести в консоль оцінку доступності     |
| `--queries`       | додатково вивести в консоль потокові запити        |
| `--export <csv>`  | зберегти коректні записи в CSV і перевірити round-trip |
| `--import <csv>`  | прочитати CSV, створений `--export`, і вивести записи |
| `--version`       | версія та номер збірки, локально `1.4.0+local`    |
| `--help`          | довідка                                           |

Коди завершення: `0` — успіх, `1` — помилка виконання (немає файла або жодного
коректного запису), `2` — неправильні аргументи.

## Приклад

Вхід (`data/input.csv`, фрагмент):

```text
Парацетамол;таблетки;45.50;420;false
Амоксицилін;капсули;132.00;300;true
Нурофен;сироп;ціна;200;false
```

Вивід для повного файла з репозиторію:

```text
Звіт: аптека
Коректних записів: 5
Середня ціна: 165.69 грн
Найменший термін придатності: 60 дн.
Рецептурних препаратів: 2
Пропущено рядків: 7
  Рядок 6: порожній рядок
  Рядок 7: ціна не є числом: 'ціна'
  Рядок 8: від'ємна ціна: -15.00
  Рядок 9: від'ємний термін придатності: -5
  Рядок 10: очікується 5 полів, знайдено 4
  Рядок 11: порожня назва препарату
  Рядок 12: ознака рецепта має бути true або false: 'можливо'
```

## Структура

```text
src/main/java/ua/lpnu/kzp/
  Main.java                 аргументи командного рядка, послідовність дій
  FileReport.java           читання/запис файлів (Path, UTF-8)
  Medicine.java             абстрактний базовий тип: спільні інваріанти, of, fromCsv,
                            equals/hashCode, абстрактні kind() і availabilityScore()
  PrescriptionMedicine.java підтип: рецептурний препарат (ціна > 0, оцінка × 0.5)
  FreeMedicine.java         підтип: безрецептурний препарат
  MedicineKind.java         enum категорії: PRESCRIPTION, FREE
  AvailabilityReport.java   поліморфний розділ «Оцінка доступності»
  MedicineQueries.java      потокові запити: filter, map, groupingBy, статистика, top-N, Optional
  QueryReport.java          консольний розділ «Потокові запити» (--queries)
  MedicineParser.java       цикл читання: fromCsv + номер рядка в помилці
  ParseResult.java          записи + повідомлення про помилки
  ExpiryPrescription.java   record: найменший термін і кількість рецептурних
  Stats.java                record: чотири показники
  ReportFormatter.java      текст звіту (Locale.ROOT, %n)
  BuildInfo.java            версія та номер збірки
  Repository.java           параметризоване сховище Repository<T>: add, all, find
  CsvColumn.java            анотація назви CSV-стовпця (RUNTIME, FIELD)
  CsvExporter.java          універсальний рефлексивний CSV-експортер
  CsvParser.java            документний CSV-парсер (машина станів)
  DataStorageException.java власний checked-виняток із cause
  MedicineRecord.java       плоский запис варіанта для CSV (LocalDate, enum)
  MedicineCsvStore.java     запис/читання MedicineRecord, перевірка заголовка
src/main/resources/version.properties   шаблон, який заповнює Maven
src/test/java/...           тести JUnit 5
data/expected-report.txt    контрольний звіт ЛР № 1 для перевірки сумісності
data/lab05.csv              вхід ЛР № 5: 6 коректних записів (один з комою й лапками) + 1 хибний
data/lab05-invalid.csv      CSV із хибним числом для демонстрації помилки
data/lab05-empty.csv        порожній CSV (0 записів)
.github/workflows/ci.yml    CI на ubuntu, windows, macos
```

## Клас-сутність `Medicine` (ЛР № 2)

`Medicine` — незмінний клас: усі поля `private final`, сеттерів немає. Створити об'єкт
можна двома способами, і обидва проходять ті самі перевірки:

- `Medicine.of(name, form, price, daysToExpire, prescription)` (з ЛР № 3; у ЛР № 2 —
  `new Medicine(...)`) — конструктор перевіряє інваріанти;
- `Medicine.fromCsv(line)` — фабрика розбирає структуру рядка й типи полів, а потім
  викликає конструктор.

| Хто перевіряє | Правило | Повідомлення |
|---------------|---------|--------------|
| `fromCsv` | рядок не порожній | `порожній рядок` |
| `fromCsv` | рівно 5 полів | `очікується 5 полів, знайдено N` |
| `fromCsv` | ціна — число з крапкою (не `NaN`, `1e3`, `45,5`) | `ціна не є числом: '…'` |
| `fromCsv` | термін — ціле число | `термін придатності не є цілим числом: '…'` |
| `fromCsv` | ознака рецепта лише `true`/`false` | `ознака рецепта має бути true або false: '…'` |
| конструктор | назва й форма не `null` і не порожні | `порожня назва препарату` / `порожня форма випуску` |
| конструктор | ціна скінченна й `>= 0` | `від'ємна ціна: …` |
| конструктор | термін `>= 0` | `від'ємний термін придатності: …` |

Номер рядка додає `MedicineParser.parseAll`: він перехоплює `IllegalArgumentException`
від `fromCsv`, тому хибний запис не потрапляє до списку й не змінює показників.

Методи читання названо так само, як компоненти record з ЛР № 1 (`name()`, `price()` …),
тому код і тести попередньої роботи не змінено.

### Відмінності від ЛР № 1

| | ЛР № 1 | ЛР № 2 |
|---|---|---|
| Запис | `record Medicine` без перевірок | `final class Medicine` з інваріантами |
| Розбір рядка | `MedicineParser.tryParse` | `Medicine.fromCsv` |
| Перевірка значень | умови в циклі розбору | конструктор `Medicine` |
| Термін і рецепт | окремі змінні в `Stats.compute` | record `ExpiryPrescription` |
| Звіт | — | без змін (тест `BehaviorCompatibilityTest`) |

## Ієрархія препаратів (ЛР № 3)

```text
              Medicine (abstract)
   name, form, price, daysToExpire + інваріанти
   kind(), availabilityScore() — abstract
            /                     \
PrescriptionMedicine          FreeMedicine
 kind() = PRESCRIPTION         kind() = FREE
 ціна > 0                      —
 оцінка = цінова × 0.5         оцінка = цінова
```

- **Спільний тип** — абстрактний клас, бо підтипи мають спільний стан (4 поля) і готову
  реалізацію (перевірки, `equals`, `hashCode`, `toString`). Обрано спадкування: рецептурний
  і безрецептурний препарат *є різновидами* препарату (is-a).
- **enum `MedicineKind`** — `PRESCRIPTION("рецептурний")`, `FREE("безрецептурний")` з
  полями `label`, `saleLabel`, `requiresPrescription`. Замінив поле-прапорець
  `prescription`: `prescription()` тепер повертає `kind().requiresPrescription()`.
- **Поліморфна операція** — `availabilityScore()` (0–100). Спільна цінова складова
  `priceAvailability() = max(0, 100 − price / 10)` міститься в базовому класі; рецептурний
  препарат множить її на 0.5, безрецептурний повертає без змін.
- **Рівність** — однаковий клас і однакові `name`, `form`, `price`, `daysToExpire`;
  `hashCode` враховує ті самі поля й клас. Рецептурний і безрецептурний препарати з
  однаковими полями не рівні.
- **Вибір підтипу** — лише у фабриці `Medicine.of` (за полем `prescription` CSV). Решта коду
  (`Stats`, `ExpiryPrescription`, `AvailabilityReport`) працює з `List<Medicine>` без
  `instanceof` і `switch`.

Приклад `--availability` для `data/input.csv` (після основного звіту):

```text
Оцінка доступності (0–100):
  Парацетамол [безрецептурний]: 95.45
  Амоксицилін [рецептурний]: 43.40
  Ібупрофен [безрецептурний]: 90.13
  Цитрамон [безрецептурний]: 96.78
  Інсулін [рецептурний]: 24.00
Середня оцінка: 69.95
```

Цей розділ виводиться лише в консоль; файл звіту без змін.

## Потокові запити (ЛР № 4)

Клас `MedicineQueries` — набір чистих запитів до `List<Medicine>`; кожен створює новий
потік і не змінює вхідний список. Форматування для консолі — у `QueryReport`.

| Метод | Конвеєр | Результат |
|-------|---------|-----------|
| `expiringSoon` | `filter(daysToExpire ≤ 30)` → `toList()` | `List<Medicine>` |
| `names` | `map(name)` → `toList()` | `List<String>` |
| `countByForm` | `groupingBy(form, TreeMap::new, counting())` | `Map<String, Long>` |
| `priceStatistics` | `summarizingDouble(price)` | `DoubleSummaryStatistics` |
| `nearestExpiry(n)` | `sorted(днів ↑, назва ↑)` → `limit(n)` → `map(name)` | `List<String>` |
| `findByName` | `filter(name)` → `findFirst()` | `Optional<Medicine>` |

Поріг — 30 днів (межа входить), N = 5; від'ємний N → `IllegalArgumentException`.
Показники основного звіту (`Stats`, `ExpiryPrescription`) теж обчислюються потоками,
результат не змінився.

Приклад `--queries` для `data/input.csv`:

```text
Потокові запити:
  Придатність до 30 днів: []
  Назви: [Парацетамол, Амоксицилін, Ібупрофен, Цитрамон, Інсулін]
  Кількість за формою: {гель=1, капсули=1, розчин=1, таблетки=2}
  Ціна: кількість=5, min=32.20, max=520.00, сума=828.45, середня=165.69
  Топ-5 за найближчою придатністю: [Цитрамон, Інсулін, Ібупрофен, Амоксицилін, Парацетамол]
```

## CSV-сховище (ЛР № 5)

Коректні препарати зберігаються в `Repository<Medicine>` (знімок `all()` незмінний,
пошук `find(Predicate<? super T>)`). Для експорту кожен препарат перетворюється на
плоский `MedicineRecord`: дні до кінця придатності → дата `expiresOn`
(сьогодні + дні), ознака рецепта → enum `MedicineKind`.

| Поле Java | Тип | CSV-стовпець (`@CsvColumn`) |
|-----------|-----|-----------------------------|
| `expiresOn` | `LocalDate` | придатність |
| `form` | `String` | форма |
| `name` | `String` | назва |
| `prescription` | `MedicineKind` | рецепт |
| `price` | `double` | ціна |

`CsvExporter` не знає класу `MedicineRecord`: рефлексією бере всі поля з `@CsvColumn`
(включно з суперкласами), сортує їх за Java-іменем і записує файл через
`Files.writeString(..., UTF_8, TRUNCATE_EXISTING)`. Значення з комою, лапкою або
перенесенням рядка береться в лапки, лапка подвоюється. Читання — `Files.readString`
з UTF-8 і `CsvParser.parseDocument` (розрізняє кому й перенесення всередині лапок,
приймає `\n` і `\r\n`). Ключ round-trip — назва + форма.

```text
> java -jar target/lab01-1.4.0.jar --input data/lab05.csv --export out/medicines.csv
...
CSV записано: out/medicines.csv (6 записів)
Round-trip: відновлено 6 записів, рівні: так
```

`out/medicines.csv` (дати відлічуються від дня запуску, тут 2026-11-18):

```text
придатність,форма,назва,рецепт,ціна
2028-01-12,таблетки,Парацетамол,FREE,45.5
2027-09-14,капсули,Амоксицилін,PRESCRIPTION,132.0
2027-05-17,гель,Ібупрофен,FREE,98.75
2027-01-17,таблетки,Цитрамон,FREE,32.2
2027-02-16,розчин,Інсулін,PRESCRIPTION,520.0
2027-11-18,сироп,"Вітаміни ""Сонечко"", дитячі",FREE,89.9
```

Помилки (`DataStorageException`, першопричина — у `getCause()`):

```text
> java -jar target/lab01-1.4.0.jar --import data/lab05-invalid.csv
Помилка: Запис 2: поле 5 (ціна): не є числом 'дорого'
  причина: java.lang.NumberFormatException: For input string: "дорого"
```

## Версія

Версія продукту (`1.4.0`) задана в `pom.xml` і позначена Git-тегом `v1.4.0` (ЛР № 1 — `v1.0.0`,
ЛР № 2 — `v1.1.0`, ЛР № 3 — `v1.2.0`, ЛР № 4 — `v1.3.0`). Номер збірки передається окремо (`-Dbuild.number=...`), у CI це номер
запуску workflow, тому `--version` виводить, наприклад, `1.4.0+42`; локально — `1.4.0+local`.

Python-пакет ЛР № 6 має версію `1.5.0` (`pyproject.toml`, `kzp_lab06.__version__`) і тег
`v1.5.0`; Java-частина в цьому релізі не змінювалася й залишається `1.4.0` у `pom.xml`.

## Python-пакет (ЛР № 6)

Пакет `kzp_lab06` читає CSV аптеки, перетворює рядки на записи з типізованими полями,
рахує показники варіанта 11 і записує детермінований текстовий звіт. Вхідний файл не
змінюється. Потрібен Python 3.11 або новіший.

### Формат CSV

Кодування UTF-8, роздільник `,`, перший рядок — заголовок рівно з такими стовпцями:

```text
name,form,price,days_left,prescription
```

| Стовпець       | Тип у записі | Правило                                              |
|----------------|--------------|------------------------------------------------------|
| `name`         | `str`        | непорожнє, пробіли по краях обрізаються              |
| `form`         | `str`        | непорожнє, зводиться до нижнього регістру            |
| `price`        | `float`      | скінченне число з крапкою, не менше 0                |
| `days_left`    | `int`        | ціле число, не менше 0                               |
| `prescription` | `bool`       | `true` або `false` (регістр не важливий)             |

Поле з комою або лапками береться в лапки, лапка всередині подвоюється:
`"Вітаміни ""Сонечко"", дитячі",сироп,89.90,365,false`.

Файл лише з заголовком — коректний порожній набір (звіт із нулями й «немає даних»).
Порожній файл, відсутній або зайвий стовпець, порожнє чи хибне значення, від'ємне число,
зайві значення в рядку, не-UTF-8 — `DataError` з номером рядка й назвою поля; звіт не
створюється.

### Модулі

```text
src/kzp_lab06/
  __init__.py   версія пакета
  errors.py     DataError
  loader.py     читання CSV (DictReader), перевірка заголовка, перетворення полів
  analysis.py   відбір, множина форм, словник кількостей, summarize(*metrics, **labels)
  reporting.py  формування тексту звіту та запис через with
  cli.py        аргументи, коди завершення, повідомлення в stderr
tests/          pytest: loader, analysis, reporting, cli
data/           lab06.csv, lab06-empty.csv, lab06-invalid.csv, lab06-expected-report.txt
```

### Встановлення та запуск

```text
python -m venv .venv
.venv\Scripts\activate            # Windows (macOS, Ubuntu: source .venv/bin/activate)
python -m pip install -e ".[dev]"

python -m kzp_lab06.cli data/lab06.csv reports/summary.txt
kzp-lab06 data/lab06.csv reports/summary.txt --max-days 30
```

Якщо PowerShell забороняє `activate`, команди запускаються напряму:
`.venv\Scripts\python.exe -m pytest`, `.venv\Scripts\ruff.exe check .`.

| Аргумент          | Значення                                              |
|-------------------|-------------------------------------------------------|
| `input_csv`       | вхідний CSV                                           |
| `output_report`   | файл звіту (каталог створюється)                      |
| `--max-days N`    | поріг відбору `days_left <= N`, типово 30             |
| `--version`       | версія пакета                                         |

Коди завершення: `0` — звіт записано; `2` — помилка даних або аргументів (повідомлення
в stderr); `1` — звіт неможливо записати.

### Приклад звіту (`data/lab06.csv`)

```text
Звіт аптеки (варіант 11)
Усього записів: 7
Рецептурних: 2
Лікарські форми (5): гель, капсули, розчин, сироп, таблетки
Кількість за формою:
  гель: 1
  капсули: 1
  розчин: 1
  сироп: 2
  таблетки: 2
Термін спливає за <= 30 дн.: 4
  Нурофен — 0 дн.
  Цитрамон — 12 дн.
  Амоксицилін — 25 дн.
  Ібупрофен — 30 дн.
Вартість препаратів із терміном, що спливає: 339.35 грн
Найближчий термін: 0 дн. (Нурофен)
Середня ціна: 142.11 грн
```

### Перевірки та wheel

```text
python -m pytest
ruff check .
ruff format --check .
python -m build
python -m venv .venv-wheel
.venv-wheel\Scripts\python -m pip install --no-deps dist\kzp_lab06-1.5.0-py3-none-any.whl
.venv-wheel\Scripts\python -m kzp_lab06.cli data\lab06.csv reports\wheel-summary.txt
```

CI (`.github/workflows/ci.yml`, job `python`) на Ubuntu, Windows і macOS запускає pytest,
ruff, збирає wheel, встановлює його в чисте середовище, звіряє звіт з еталоном, перевіряє
код 2 для пошкодженого CSV і публікує артефакт `wheel-<ОС>-<номер>`.
