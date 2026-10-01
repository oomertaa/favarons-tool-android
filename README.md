# Favaron Costuri

Offline Android app for a pastry chef: ingredient costs, recipes and sales
history. Kotlin, Jetpack Compose, Material 3, Room. UI in Romanian, currency RON.

- Target device: Lenovo Tab M11 (TB330XU), Android 15 (API 35). Works in
  landscape (navigation rail) and portrait.
- `minSdk = targetSdk = compileSdk = 35`.
- **Fully offline.** No INTERNET permission, no login, no analytics, no cloud.

> **Your data lives only on the tablet.** There is no cloud sync and Android
> backup is turned off. Uninstalling the app deletes all data. **The CSV
> export is your backup**: export the sales history (and optionally the
> ingredients and recipes) regularly and copy the files somewhere safe.
> The CSV contains the **displayed (rounded) values**, exactly as on screen,
> not the full-precision values stored internally. The app has no CSV import.

---

## 1. Build requirements

- JDK 17 or newer (JDK 21 works).
- Android SDK with *Android SDK Platform 35* and *Build-Tools 35*. Either open
  the project once in Android Studio (it installs these automatically), or use
  the command-line tools:
  `sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"`.
- Tell Gradle where the SDK is, in `local.properties` (not committed):
  ```
  sdk.dir=/path/to/Android/Sdk
  ```
  (or set the `ANDROID_HOME` environment variable).

## 2. Create the signing keystore (once)

Android only installs an update over an existing app if both are signed with
the **same key**. Create the key once and keep it forever:

```bash
keytool -genkeypair -v \
  -keystore ~/favaron-keys/favaron-release.jks \
  -alias favaron \
  -keyalg RSA -keysize 4096 -validity 36500
```

`keytool` comes with the JDK. It asks for a password and a name; remember the
password.

Then create `keystore.properties` in the project root (next to
`settings.gradle.kts`). It is listed in `.gitignore` and must never be
committed:

```properties
storeFile=/home/you/favaron-keys/favaron-release.jks
storePassword=your-store-password
keyAlias=favaron
keyPassword=your-key-password
```

(`storeFile` may also be relative to the project root.)

**Where to keep the keystore:** outside the project folder and outside any git
repository (e.g. `~/favaron-keys/`). Keep at least one extra copy offline,
such as a USB stick, and store the passwords in a password manager.
If you lose the keystore you cannot update the installed app. You would have
to uninstall it, which **deletes all data**, so export CSVs first.

## 3. Build the APK

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk` (signed with your key).
Without `keystore.properties` the build produces `app-release-unsigned.apk`,
which Android will not install.

Optional check:
```bash
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## 4. Install on the tablet

**Option A, USB cable with adb:**
1. On the tablet: *Settings → About tablet*, tap *Build number* 7 times, then
   enable *Developer options → USB debugging*.
2. Connect the tablet and run:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```

**Option B, copy the file:**
1. Copy `app-release.apk` to the tablet (USB file transfer, SD card…).
2. Open it with the *Files* app. When asked, allow *Install unknown apps* for
   *Files*, then tap *Install*.

### Updating to a new version

1. In `app/build.gradle.kts` increase `versionCode` (e.g. `1` → `2`) and
   optionally `versionName`.
2. `./gradlew assembleRelease` with the **same keystore**.
3. Install as above (`adb install -r …` or open the file). The app is
   replaced in place and **all data is kept**.

Do not uninstall the app before updating. Uninstalling deletes the data.

## 5. Using the app

| Page | What it does |
|---|---|
| **Ingrediente** | Ingredients and consumables: name, unit (g / ml / buc), package quantity and price, *Consumabil* flag. Shows the price per g / ml / buc. Search and A→Z / Z→A sort. Deleting an ingredient that is used in recipes shows the affected recipes. Deletion is allowed, and those recipe lines are marked **lipsă**. |
| **Rețete** | Recipes: name, yield (pieces per batch), optional selling price per piece, lines with quantities for the whole batch. Consumable lines can have *Cantitate fixă*. Shows line costs, batch cost, cost per piece and margin, all computed from current prices. *Adaugă ingredient* opens the same ingredient dialog and selects the new item. **Scalează** (on each recipe card and in the editor) changes the number of pieces **and** multiplies the line quantities, so the costs follow. Ingredients scale exactly, consumables are rounded up to whole pieces, and *Cantitate fixă* lines stay unchanged. Typing a new yield directly only corrects the number and leaves the quantities as they are. A recipe with **lipsă** lines cannot be sold until every such line is replaced or removed. |
| **Vânzări** | Sales history. *Comandă nouă* builds an order from valid recipes (pieces, price override, quantity adjustments, customer, note, date). Saved orders are **frozen snapshots**: later changes to ingredients or recipes never affect them. Each order can be expanded, edited manually (*Editează / Salvează / Anulează*, marked *modificat manual*), rescaled per product (*Rescalează automat*) or deleted. The bottom panel shows totals and per-product totals for the selected date range. |

Quantity scaling when selling P pieces of a recipe with yield Y:
- ingredient: `batch quantity / Y × P` (exact);
- consumable: `CEIL(batch quantity / Y × P)` (whole pieces);
- consumable with *Cantitate fixă*: the batch quantity, unchanged.

## 6. CSV export

Each page has an **Export CSV** button. Android's *Save as* screen opens, where
you pick the folder and file name (Storage Access Framework, so no storage
permission is needed).

- Format for Romanian Excel: UTF-8 **with BOM**, `;` as separator, decimal
  **comma**, no thousands separator, no "RON" or units inside numeric cells
  (units have their own column). Text containing `;`, quotes or line breaks
  is quoted.
- The values are **exactly what the screen shows**: the export uses the same
  formatter as the UI (prices and line costs with 2 to 4 decimals, totals with
  2, trimmed quantities, dates as `dd.MM.yyyy`). Full-precision internal
  values are never written.
- Sales export (`vanzari_YYYY-MM-DD.csv`): one row per ingredient line per
  order with the columns *Data; Client; Produs; Bucăți; Ingredient; Unitate;
  Consumabil; Cantitate; Preț unitar; Cost linie; Preț vânzare/buc*. It is
  read only from the history (manual edits included) and covers the orders
  currently listed, so the date filter applies.
- Ingredient and recipe exports are also available on their pages.

## 7. Numbers and precision

- All money and quantities are `BigDecimal`, stored in SQLite as exact text.
  Nothing is rounded on save.
- Unit price = package price / package quantity, scale 10, HALF_UP.
- Line costs, batch cost, cost per piece, totals, profit and margins are computed
  at full precision. Rounding happens **only for display and export**, so a
  displayed total can differ by 1 ban from the sum of the displayed lines.
  This is expected.
- The only stored value that gets rounded is a scaled consumable quantity
  (rounded up to a whole number).
- Inputs accept a decimal comma or a dot. Prices allow at most 2 decimals.
  Negative values, zero package quantity and yield < 1 are rejected.

## 8. Tests

```bash
./gradlew testDebugUnitTest
```

Everything runs on the JVM. Room and UI tests use Robolectric with an in-memory
database, so no device is needed.

- `CalcTest`: unit price precision (12,50 RON / 1000 g = 0,0125; 40 g = 0,5;
  1 RON / 1000 g → 2 g = 0,002 stored unrounded), batch cost and cost per piece,
  no rounding drift over many small lines, scaling, CEIL.
- `FmtTest`: the shared display formatter (12.5 → "12,50", 0.0125 → "0,0125",
  0.001 → "0,001", tiny values never shown as 0), parsing and validation.
- `OrderLogicTest`: scaling by pieces, CEIL for consumables, fixed consumables,
  price override, manual cost override, pieces edits change revenue only,
  rescale 10 → 12 → 10 returning the original values, rescale overwriting
  manual edits, adding and deleting manual lines.
- `CsvTest`: BOM, semicolons, quoting, and every numeric cell equal to the
  string the history screen shows.
- `SnapshotIndependenceTest` (Room): after saving an order, changing an
  ingredient's price, package quantity or name, deleting it, changing a
  recipe's lines, yield or price, or deleting the recipe leaves every stored
  value, the summaries and the CSV unchanged. Also covers manual edits
  persisting without touching live data, recalculation using only stored
  values, and missing-ingredient handling.
- `RecipeScalingTest`, plus Room tests: *Scalează* multiplies quantities and
  costs, a plain yield edit does not, and scaling never changes history.
- `UiSmokeTest`: drives the real UI end to end (ingredient → recipe → scale →
  order → edit) in landscape, plus the narrow portrait layout.

## 9. Font

All text uses an Arial Narrow–style font, at the standard Material 3 sizes.
Arial Narrow itself is a commercial font, so the app bundles **Liberation Sans
Narrow** instead. It is free and metric-compatible with Arial Narrow (same
character widths), and covers all Romanian diacritics. Its license is in
`third_party/liberation-sans-narrow/`.

If you own Arial Narrow (it comes with Microsoft Office) and want the original,
copy `ARIALN.TTF` and `ARIALNB.TTF` over
`app/src/main/res/font/app_font_regular.ttf` and `app_font_bold.ttf` (keep
these names), then rebuild.

## 10. Project layout

```
app/src/main/java/ro/favaron/costuri/
  data/      Room entities, DAOs, database, repositories
             LIVE domain: ingredients, recipes, recipe_lines
             HISTORY domain: orders, order_products, order_ingredient_lines
             (denormalized, no foreign keys, no references to live data)
  domain/    Calc (math), Fmt (the shared formatter), Csv, OrderFactory
             (live recipe → snapshot), OrderEditor (manual edits/rescale),
             Summaries, validation
  ui/        Compose screens + ViewModels (MVVM, single Activity, Navigation)
app/schemas/ exported Room schema (for future migrations)
```

For future developers: the database has no destructive-migration fallback.
When you change the schema, bump the version in `AppDatabase` and add a
`Migration` so existing data survives the update.

About permissions: the merged manifest contains only
`ro.favaron.costuri.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. AndroidX adds
it automatically. It is a private permission that only this app can hold,
and it does not give network access.
