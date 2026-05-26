# Symulator Automatu z Napojami (Java)

Konsolowa aplikacja w języku Java symulująca działanie klasycznego automatu z napojami. Projekt demonstruje podstawy programowania obiektowego (OOP), obsługę wyjątków, operacje na kolekcjach oraz prostą logikę biznesową związaną z transakcjami i wydawaniem reszty.

## Funkcjonalności

* **Wybór napojów:** Możliwość zakupu różnych napojów (Woda, Fanta, Cola) o ustalonych cenach.
* **System monetarny:** Obsługa wrzucania konkretnych nominałów (od 1 gr do 5 zł) przy użyciu typu wyliczeniowego `Enum`.
* **Zarządzanie portfelem i plecakiem:** Gracz posiada określoną kwotę startową oraz ekwipunek (plecak), do którego trafiają zakupione napoje.
* **Inteligentne wydawanie reszty:** Automat przelicza wrzucone monety i wydaje resztę na podstawie aktualnego stanu swojej kasy. 
* **Obsługa błędów transakcji:** Zabezpieczenia przed wrzuceniem niewystarczającej kwoty, brakiem napoju w slocie oraz sytuacją, w której automat nie ma jak wydać reszty (wtedy cofa transakcję i oddaje wrzucone monety).

## Struktura Projektu

Kod został logicznie podzielony na kilka głównych sekcji:

1.  **Model Domenowy:** Klasy reprezentujące byty fizyczne, takie jak `Napoj` (klasa abstrakcyjna) i jego dziedziczące warianty (`Woda`, `Fanta`, `Cola`) oraz `Nominal` (enum reprezentujący polskie monety w groszach).
2.  **Klasy Pomocnicze dla Transakcji:** Obiekty przechowujące wynik operacji (`WynikTransakcji`) oraz niestandardowe wyjątki (`BrakResztyException`).
3.  **Logika Biznesowa:** * `Gracz` – przechowuje stan portfela gracza oraz listę zakupionych napojów.
    * `Automat` – zarządza slotami z towarem, kasetką z pieniędzmi oraz logiką procesowania zakupu i wydawania reszty.
4.  **Warstwa Interfejsu:** Klasa `InterfejsUzytkownika` odpowiadająca za wyświetlanie menu w konsoli, interakcję z użytkownikiem i walidację wprowadzanych danych.
5.  **Main:** Punkt wejścia do programu, inicjalizujący obiekty i "zatowarowujący" automat przed rozpoczęciem gry.

## Jak uruchomić projekt?

### Wymagania
* Zainstalowane środowisko **Java Development Kit (JDK)** w wersji 8 lub nowszej.

### Instrukcja krok po kroku

1. Sklonuj repozytorium na swój dysk lokalny:
   ```bash
   git clone [https://github.com/TwojaNazwaUzytkownika/nazwa-repozytorium.git](https://github.com/TwojaNazwaUzytkownika/nazwa-repozytorium.git)
