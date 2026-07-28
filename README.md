# ⚔️ Miecz Słowa Bożego

Aplikacja na Androida do nauki wersetów biblijnych na pamięć przez grywalizację — poziomy,
zagadki, ćwiczenie wymowy na głos oraz system utrwalania wiedzy oparty na „sercu”.

> **Sword of the Word of God** — a gamified Android app for memorizing Bible verses.
> English version below.

---

## ✨ Główne funkcje

- **Sekcje i poziomy** — każda sekcja to 10 wersetów z Pisma Świętego do nauczenia, każda 
  ma 12 poziomów z zagadkami oraz dwa wyzwania specjalne, ostatecznie weryfikujące znajomość
  wersetów wers -> siglum i siglum -> wers. Ukończenie tych wyzwań odblokowuje kolejną sekcję.
  Wyzwania można podjąć w dowolnym momencie dla aktualnej sekcji, co pozwala szybciej przeskoczyć 
  do nauki kolejnych wersetów; w ten sposób użytkownik nie traci czasu, jeśli jest już zaznajomiony
  z aktualnymi wersetami.
- **Różne typy zagadek** — podstawowe poziomy zawierają różne typy zagadek, m.in. quizy,
  uzupełnianie słów w wersecie, uzupełnianie sigli, rozsypanki wyrazowe.
- **Utrwalenie w sercu** — dla każdej sekcji istnieje tak zwany poziom utrwalenia wersetów w sercu,
  który codziennie spada o 5%; aby grać w kolejne poziomy, oprócz ukończenia poprzednich należy
  osiągnąć odpowiedni poziom utrwalenia - 4%, aby grać w poziom 1, 12%, aby grać w 2, 20% w 3. itd,
  poziom utrwalenia można zwiększyć poprzez
  - granie w poziomy: połącz części wersetów i połącz sigla i wersety w pary - 2% za każdy raz dziennie
  - wymawianie wersetów na głos, 1% za wymówienie pojedynczego wersetu 5/8/10 razy, raz dziennie
    (łącznie 30%)
  - granie w zwykłe poziomy - 3% za przejście poziomu po raz pierwszy.
- **Tarcze** — ograniczają liczbę prób do 5 i odnawiają się jedna co pół godziny; można je odzyskać w Powtórce.
- **Obrazki mnemoniczne** — towarzyszą zagadkom i służą lepszemu zapamiętaniu sigli, można użyć
  domyślnych obrazków wygenerowanych przez sztuczną inteligencję, narysować własny obrazek lub
  zaimportować zdjęcie.
- **Baza wersetów** — po ukończeniu 4 podstawowych sekcji, użytkownik sam wybiera po dwie pięciowersetowe
  grupy do nauki w kolejnych sekcjach; bazę wszystkich wersetów można przeglądać w dowolnym momencie.
- **Powtórka** — aktywna, jeśli użytkownik ukończył minimum 2 sekcje, są to losowe zagadki dla już
  poznanych wersetów, można w ten sposób odzyskać utracone tarcze.
- **Losowanie** — rozwiąż zagadki dla losowego cytatu.
- **Osiągnięcia** — rekordy czasowe, serie, liczba znanych i powtórzonych wersetów,
  ukończonych poziomów itp.
- **Codzienne przypomnienia** oraz **eksport do AnkiDroid** (CSV).

## 🛠️ Technologia

- **Kotlin** + **Jetpack Compose** (Material 3)
- Trwałość danych: `SharedPreferences`, obrazki w pamięci wewnętrznej
- Rozpoznawanie mowy: `android.speech.SpeechRecognizer`
- `minSdk 24`, `targetSdk 36` — AGP 9.3, Gradle 9.5, Kotlin 2.2

## 🚀 Uruchomienie

```bash
git clone git@github.com:Ptimulka/SwordOfTheWordOfGod.git
```
Otwórz projekt w Android Studio i uruchom konfigurację `app` na emulatorze lub urządzeniu
(Android 7.0+). Aplikacja jest w języku polskim.

## 🌿 Gałęzie

- `master` — stabilna wersja
- `develop` — bieżące prace

## 📜 Licencja

Kod źródłowy jest udostępniony na licencji **GNU General Public License v3.0** — zobacz plik
[LICENSE](LICENSE).

> ⚠️ **Uwaga:** licencja obejmuje wyłącznie kod źródłowy. Teksty wersetów biblijnych oraz
> domyślne obrazki mnemoniczne pozostają własnością odpowiednich podmiotów praw autorskich
> i **nie są objęte** tą licencją.

---

# ⚔️ Sword of the Word of God (English)

A gamified Android app for learning Bible verses by heart — levels, riddles, spoken practice,
and a "heart" retention system.

## ✨ Features

- **Sections & levels** — each section is a set of 10 Scripture verses to learn; it has 12 riddle
  levels plus two special challenges that ultimately verify your knowledge of the verses both ways:
  verse → reference and reference → verse. Completing these challenges unlocks the next section.
  The challenges can be taken at any time for the current section, letting you jump ahead to new
  verses faster — so you don't waste time if you already know the current ones.
- **Varied riddles** — the standard levels contain different riddle types, including quizzes,
  filling in words of a verse, filling in references (sigla), and word scrambles.
- **Heart retention** — every section has a "verse retention in the heart" level that drops by 5%
  each day. To play a level, besides finishing the previous ones you also need enough retention —
  4% for level 1, 12% for level 2, 20% for level 3, and so on. Retention is raised by:
  - playing the connect levels (connect verse parts, connect references and verses into pairs) —
    2% each, once per day;
  - saying verses aloud — 1% for repeating a single verse 5/8/10 times, once per day (up to 30%);
  - playing the standard levels — 3% for completing a level for the first time.
- **Shields** — limit attempts to 5 and regenerate one every half hour; they can be recovered in Review.
- **Mnemonic pictures** — accompany the riddles and help you remember references; use the default
  AI-generated pictures, draw your own, or import a photo.
- **Verse database** — after finishing the 4 base sections, you pick two five-verse groups yourself
  for each next section; the full verse database can be browsed at any time.
- **Review** — available once you've finished at least 2 sections; random riddles for verses you
  already know, and a way to recover lost shields.
- **Random** — solve riddles for a random verse.
- **Achievements** — best times, streaks, counts of known and repeated verses, finished levels, etc.
- **Daily reminders** and **AnkiDroid CSV export**.

## 🛠️ Tech

- **Kotlin** + **Jetpack Compose** (Material 3)
- Storage: `SharedPreferences`, pictures in internal storage
- Speech: `android.speech.SpeechRecognizer`
- `minSdk 24`, `targetSdk 36` — AGP 9.3, Gradle 9.5, Kotlin 2.2

## 🚀 Getting started

```bash
git clone git@github.com:Ptimulka/SwordOfTheWordOfGod.git
```
Open the project in Android Studio and run the `app` configuration on an emulator or device
(Android 7.0+). The app UI is in Polish.

## 🌿 Branches

- `master` — stable
- `develop` — ongoing work

## 📜 License

The source code is released under the **GNU General Public License v3.0** — see [LICENSE](LICENSE).

> ⚠️ **Note:** the license covers the source code only. Bible verse texts and the default mnemonic
> images remain the property of their respective copyright holders and are **not covered** by
> this license.
