import java.util.*;

// --- MODEL DOMENOWY ---

abstract class Napoj {
    private final String nazwa;
    private final int cenaWGroszach;

    public Napoj(String nazwa, int cenaWGroszach) {
        this.nazwa = nazwa;
        this.cenaWGroszach = cenaWGroszach;
    }

    public String getNazwa() {
        return nazwa;
    }

    public int getCenaWGroszach() {
        return cenaWGroszach;
    }
}

class Woda extends Napoj {
    public Woda() { super("Woda", 249); }
}

class Fanta extends Napoj {
    public Fanta() { super("Fanta", 370); }
}

class Cola extends Napoj {
    public Cola() { super("Cola", 415); }
}

enum Nominal {
    N_5(500), N_2(200), N_1(100),
    N_05(50), N_02(20), N_01(10),
    N_005(5), N_002(2), N_001(1);

    private final int wartosc;

    Nominal(int wartosc) {
        this.wartosc = wartosc;
    }

    public int getWartosc() {
        return wartosc;
    }
}

// --- KLASY POMOCNICZE DLA TRANSAKCJI ---

class BrakResztyException extends Exception {
    public BrakResztyException(String message) {
        super(message);
    }
}

class WynikTransakcji {
    private final Napoj napoj;
    private final Map<Nominal, Integer> resztaWMonetach;

    public WynikTransakcji(Napoj napoj, Map<Nominal, Integer> resztaWMonetach) {
        this.napoj = napoj;
        this.resztaWMonetach = resztaWMonetach;
    }

    public Napoj getNapoj() {
        return napoj;
    }

    public Map<Nominal, Integer> getResztaWMonetach() {
        return resztaWMonetach;
    }
}

// --- LOGIKA BIZNESOWA ---

class Gracz {
    private int portfelWolny;
    private final List<Napoj> plecak;

    public Gracz(int kwotaPoczatkowa) {
        this.portfelWolny = kwotaPoczatkowa;
        this.plecak = new ArrayList<>();
    }

    public int getPortfelWolny() {
        return portfelWolny;
    }

    public boolean czyStac(int kwota) {
        return portfelWolny >= kwota;
    }

    public void pobierzPieniadze(int kwota) {
        if (!czyStac(kwota)) {
            throw new IllegalArgumentException("Niewystarczające środki");
        }
        this.portfelWolny -= kwota;
    }

    public void dodajPieniadze(int kwota) {
        this.portfelWolny += kwota;
    }

    public void dodajDoPlecaka(Napoj napoj) {
        plecak.add(napoj);
    }

    public List<Napoj> getPlecak() {
        return Collections.unmodifiableList(plecak);
    }
}

class Automat {
    private final Map<Integer, Queue<Napoj>> sloty;
    private final Map<Nominal, Integer> kasa;

    public Automat() {
        this.sloty = new HashMap<>();
        this.kasa = new EnumMap<>(Nominal.class);
        for (Nominal n : Nominal.values()) {
            kasa.put(n, 0);
        }
    }

    public void zasilKase(Nominal nominal, int ilosc) {
        kasa.put(nominal, kasa.getOrDefault(nominal, 0) + ilosc);
    }

    public void zatowaruj(int numerWyboru, Napoj napoj, int ilosc) {
        sloty.putIfAbsent(numerWyboru, new LinkedList<>());
        for (int i = 0; i < ilosc; i++) {
            sloty.get(numerWyboru).add(napoj);
        }
    }

    public void zatowarujNowymi(int numerWyboru, Class<? extends Napoj> typNapoju, int ilosc) {
        sloty.putIfAbsent(numerWyboru, new LinkedList<>());
        try {
            for (int i = 0; i < ilosc; i++) {
                sloty.get(numerWyboru).add(typNapoju.getDeclaredConstructor().newInstance());
            }
        } catch (Exception e) {
            System.err.println("Nie udało się zatowarować");
        }
    }

    public boolean czyDostepny(int numerWyboru) {
        return sloty.containsKey(numerWyboru) && !sloty.get(numerWyboru).isEmpty();
    }

    public Napoj sprawdzNapoj(int numerWyboru) {
        if (!czyDostepny(numerWyboru)) return null;
        return sloty.get(numerWyboru).peek();
    }

    public Napoj wydajNapoj(int numerWyboru) {
        if (!czyDostepny(numerWyboru)) {
            throw new IllegalStateException("Brak napoju");
        }
        return sloty.get(numerWyboru).poll();
    }

    public WynikTransakcji kupNapoj(int numerWyboru, Map<Nominal, Integer> wrzuconeMonety) throws BrakResztyException {
        if (!czyDostepny(numerWyboru)) {
            throw new IllegalStateException("Napój niedostępny.");
        }

        int cena = sprawdzNapoj(numerWyboru).getCenaWGroszach();
        int sumaWrzucona = sumujMonety(wrzuconeMonety);

        if (sumaWrzucona < cena) {
            throw new IllegalArgumentException("Wrzucono za mało pieniędzy.");
        }

        int resztaDoWydania = sumaWrzucona - cena;

        // 1. Symulacja kasy automatu (obecny stan + wrzucone monety)
        Map<Nominal, Integer> tymczasowaKasa = new EnumMap<>(kasa);
        for (Map.Entry<Nominal, Integer> entry : wrzuconeMonety.entrySet()) {
            tymczasowaKasa.put(entry.getKey(), tymczasowaKasa.getOrDefault(entry.getKey(), 0) + entry.getValue());
        }

        // 2. Przygotowujemy zbiór monet, które zostaną wydane
        Map<Nominal, Integer> wydanaReszta = new EnumMap<>(Nominal.class);

        // 3. Algorytm zachłanny wydawania reszty
        for (Nominal nominal : Nominal.values()) {
            int wartosc = nominal.getWartosc();
            int dostepnychWKasie = tymczasowaKasa.getOrDefault(nominal, 0);

            if (resztaDoWydania >= wartosc && dostepnychWKasie > 0) {
                int potrzebnychMonet = resztaDoWydania / wartosc;
                int uzytychMonet = Math.min(potrzebnychMonet, dostepnychWKasie);

                wydanaReszta.put(nominal, uzytychMonet);
                resztaDoWydania -= uzytychMonet * wartosc;
                tymczasowaKasa.put(nominal, dostepnychWKasie - uzytychMonet);
            }
        }

        // 4. Błąd, jeśli nie da się wydać reszty
        if (resztaDoWydania > 0) {
            throw new BrakResztyException("Automat nie ma jak wydać resztę");
        }

        // 5. Sukces! Zatwierdzamy stan kasy i wydajemy napój
        this.kasa.putAll(tymczasowaKasa);
        Napoj wydanyNapoj = sloty.get(numerWyboru).poll();

        return new WynikTransakcji(wydanyNapoj, wydanaReszta);
    }

    private int sumujMonety(Map<Nominal, Integer> monety) {
        int suma = 0;
        for (Map.Entry<Nominal, Integer> entry : monety.entrySet()) {
            suma += entry.getKey().getWartosc() * entry.getValue();
        }
        return suma;
    }
}

// --- WARSTWA INTERFEJSU ---

class InterfejsUzytkownika {
    private final Scanner scanner;

    public InterfejsUzytkownika(Scanner scanner) {
        this.scanner = scanner;
    }

    public void uruchom(Gracz gracz, Automat automat) {
        boolean graDziala = true;

        while (graDziala) {
            System.out.println("\n=== MENU GŁÓWNE ===");
            System.out.println("1. Podejdź do automatu (Kup napój)");
            System.out.println("2. Sprawdź plecak");
            System.out.println("0. Zakończ program");
            System.out.print("Twój wybór: ");

            int akcja = czytajInt();

            switch (akcja) {
                case 1 -> obsluzAutomat(gracz, automat);
                case 2 -> wyswietlPlecak(gracz);
                case 0 -> {
                    graDziala = false;
                    System.out.println("Koniec programu");
                }
                default -> System.out.println("Błędny wybór");
            }
        }
    }

    private void wyswietlPlecak(Gracz gracz) {
        System.out.println("\n--- ZAWARTOŚĆ PLECAKA ---");
        List<Napoj> plecak = gracz.getPlecak();
        if (plecak.isEmpty()) {
            System.out.println("Nic tu nie ma");
        } else {
            for (int i = 0; i < plecak.size(); i++) {
                System.out.println((i + 1) + ". " + plecak.get(i).getNazwa());
            }
        }
        System.out.println("Gotówka w portfelu: " + gracz.getPortfelWolny() + " gr");
        System.out.println("-------------------------");
    }

    private void obsluzAutomat(Gracz gracz, Automat automat) {
        System.out.println("\n--- AUTOMAT Z NAPOJAMI ---");
        System.out.println("Wybierz napój: 1. Woda, 2. Fanta, 3. Cola");
        int wyborNapoju = czytajInt();

        if (!automat.czyDostepny(wyborNapoju)) {
            System.out.println("Wybranego napoju nie ma");
            return;
        }

        Napoj wybranyNapoj = automat.sprawdzNapoj(wyborNapoju);
        int cena = wybranyNapoj.getCenaWGroszach();

        System.out.println("Wybrałeś: " + wybranyNapoj.getNazwa() + " (Cena: " + cena + " gr)");
        System.out.println("Masz w portfelu: " + gracz.getPortfelWolny() + " gr");

        Map<Nominal, Integer> wrzuconeMonety = new EnumMap<>(Nominal.class);
        int wrzuconaKwota = 0;
        Nominal[] dostepneNominaly = Nominal.values();

        while (wrzuconaKwota < cena) {
            System.out.println("\nAktualnie wrzucona kwota: " + wrzuconaKwota + " gr / " + cena + " gr");
            System.out.println("Wybierz nominał do wrzucenia:");

            for (int i = 0; i < dostepneNominaly.length; i++) {
                System.out.println((i + 1) + ". Wrzuć " + dostepneNominaly[i].getWartosc() + " gr");
            }
            System.out.println("0. ANULUJ TRANSAKCJĘ");

            int decyzja = czytajInt();

            if (decyzja == 0) {
                System.out.println("Anulowano. Zwracam " + wrzuconaKwota + " gr.");
                gracz.dodajPieniadze(wrzuconaKwota);
                return;
            } else if (decyzja > 0 && decyzja <= dostepneNominaly.length) {
                Nominal wybranyNominal = dostepneNominaly[decyzja - 1];
                int wartosc = wybranyNominal.getWartosc();

                if (gracz.czyStac(wartosc)) {
                    gracz.pobierzPieniadze(wartosc);
                    wrzuconaKwota += wartosc;
                    // Dodanie monety do naszego wiaderka
                    wrzuconeMonety.put(wybranyNominal, wrzuconeMonety.getOrDefault(wybranyNominal, 0) + 1);
                } else {
                    System.out.println("Nie masz takiej monety w portfelu!");
                }
            } else {
                System.out.println("Nieprawidłowy wybór monety.");
            }
        }

        System.out.println("\nPrzetwarzam transakcję...");

        try {
            WynikTransakcji wynik = automat.kupNapoj(wyborNapoju, wrzuconeMonety);

            gracz.dodajDoPlecaka(wynik.getNapoj());
            System.out.println("Pomyślnie kupiono: " + wynik.getNapoj().getNazwa());

            Map<Nominal, Integer> wydanaReszta = wynik.getResztaWMonetach();
            int sumaReszty = 0;
            if (!wydanaReszta.isEmpty()) {
                System.out.println("Automat wydaje resztę w monetach:");
                for (Map.Entry<Nominal, Integer> entry : wydanaReszta.entrySet()) {
                    System.out.println(" -> " + entry.getValue() + "x " + entry.getKey().getWartosc() + " gr");
                    sumaReszty += entry.getKey().getWartosc() * entry.getValue();
                }
            }
            gracz.dodajPieniadze(sumaReszty);

        } catch (BrakResztyException e) {
            System.out.println("KOMUNIKAT: " + e.getMessage());
            System.out.println("Transakcja anulowana. Automat zwraca wrzucone monety (" + wrzuconaKwota + " gr).");
            gracz.dodajPieniadze(wrzuconaKwota);
        } catch (Exception e) {
            System.out.println("Wystąpił błąd: " + e.getMessage());
            gracz.dodajPieniadze(wrzuconaKwota);
        }
    }

    private int czytajInt() {
        while (!scanner.hasNextInt()) {
            System.out.println("Błąd: Proszę podać liczbę całkowitą.");
            scanner.next(); // czyszczenie bufora
        }
        return scanner.nextInt();
    }
}

// --- MAIN ---

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Gracz gracz = new Gracz(3000);
        Automat automat = new Automat();

        // Automat zatowarowany tylko napojami (kasa z pieniędzmi jest pusta)
        automat.zatowarujNowymi(1, Woda.class, 3);
        automat.zatowarujNowymi(2, Fanta.class, 3);
        automat.zatowarujNowymi(3, Cola.class, 3);

        InterfejsUzytkownika ui = new InterfejsUzytkownika(scanner);
        ui.uruchom(gracz, automat);

        scanner.close();
    }
}