# Audyt bezpieczeństwa integracji netstack — listopad 2026

## Status

Silnik NetValve pozostaje eksperymentalny. Nie włączaj `ENABLE_NETSTACK_RUNTIME` w wydaniu dla użytkowników i nie publikuj APK jako działającego blokera hazardu, dopóki poniższe luki nie zostaną zamknięte i zweryfikowane na urządzeniu.

## Poprawki wykonane

- TCP: odczyt początkowy korzysta z limitu czasu dodanego do przypiętego bindingu gomobile; parser TLS składa fragmenty ClientHello i rekordy TLS przed przekazaniem pierwszych bajtów do serwera.
- HTTP: żądanie i nagłówki są buforowane do końca nagłówka przed przekazaniem dalej, aby umożliwić odczyt pola Host.
- UDP: wysyłanie datagramów do serwera i odbieranie odpowiedzi odbywają się niezależnie. Brak odpowiedzi na pojedynczy datagram nie powinien już blokować całego skojarzenia UDP. Zmiana wymaga jeszcze testu na urządzeniu.

- Dodano brakującą implementację `TrafficFilterEngine.isRunning()` w adapterze NetValve.
- Nieudane utworzenie interfejsu VPN nie kończy już metody `start()` z pozostawioną flagą `running=true`.
- Deskryptor interfejsu TUN jest przechowywany i zamykany podczas sprzątania.
- Ścieżka błędu uruchomienia wywołuje wspólne `stop()`, które zamyka utworzony tunel i TUN.

## Luki blokujące wydanie

### 1. QUIC / HTTP/3

Ruch UDP jest przekazywany bez inspekcji nazwy domeny. Dotyczy to m.in. QUIC/HTTP/3 na UDP 443. Klient może więc ominąć kontrolę hosta opartą o HTTP Host i TLS SNI.

### Decyzja projektowa: QUIC / UDP 443

Nie wdrażamy globalnego blokowania UDP/443 jako pierwszej poprawki. Taki skrót może wymusić fallback przeglądarki do TCP, ale może też pogorszyć działanie lub zerwać połączenia aplikacji, które używają QUIC bez poprawnego fallbacku. Nie wolno zakładać, że wszystkie aplikacje zachowają się jak przeglądarka.

Rozważane warianty:

- **Odrzucanie całego UDP/443** — proste, ale szerokie w skutkach; wymaga jawnego testu popularnych aplikacji i pomiaru czasu fallbacku. Nie jest domyślnie zaakceptowane.
- **Inspekcja QUIC Initial i odczyt SNI** — bardziej selektywna, ale wymaga poprawnej obsługi QUIC v1/v2, ochrony pakietów Initial, fragmentacji, retransmisji i wersji protokołu. Samo wyszukanie tekstu domeny w datagramie nie jest bezpiecznym parserem.
- **Brak zmian / przepuszczanie UDP/443** — zachowuje kompatybilność, ale pozostawia obejście blokady przez HTTP/3.

**Kierunek:** najpierw przygotować osobny, wyłączony domyślnie tryb eksperymentalny dla UDP/443, a następnie porównać selektywną inspekcję QUIC z kontrolowanym odrzucaniem UDP/443 na emulatorze i urządzeniu. Nie zmieniać zachowania zwykłego wydania, dopóki testy nie potwierdzą fallbacku oraz działania aplikacji korzystających z UDP. Każdy wariant musi mieć łatwy, sprawdzony mechanizm wyłączenia ochrony.

Minimalna macierz testów przed wyborem:
1. Chrome/Android: strona obsługująca HTTP/3 oraz strona działająca wyłącznie po TCP.
2. YouTube i aplikacje bankowe: logowanie, odtwarzanie/odświeżanie i powrót do aplikacji.
3. STS, Fortuna i domeny testowe: sprawdzenie, czy blokada nie zależy wyłącznie od DNS.
4. Wi-Fi → LTE/5G → Wi-Fi, utrata sieci, ponowne połączenie i zatrzymanie VPN.
5. Porównanie czasu ładowania i błędów z ochroną wyłączoną oraz w każdym wariancie eksperymentalnym.

### 2. DNS poza klasycznym UDP/53

Obecny filtr DNS dla UDP działa na porcie 53. Dodano kodek ramek DNS-over-TCP, inkrementalny dekoder strumienia oraz podłączono eksperymentalną ścieżkę portu 53/TCP do przekaźnika. Dekoder obsługuje fragmentację prefiksu i treści, wiele wiadomości w jednym odczycie oraz niepełną końcówkę. Ta ścieżka nie ma jeszcze testów end-to-end ani potwierdzenia na urządzeniu. Nie obejmuje DNS-over-TLS (853), DNS-over-HTTPS ani własnych resolverów aplikacji.

### 3. Niepełna widoczność TLS

Parser zbiera fragmenty ClientHello i rekordy TLS z kolejnych odczytów TCP przed przekazaniem danych dalej. Nie rozwiązuje to jednak problemu TLS Encrypted Client Hello (ECH), braku SNI ani wszystkich nietypowych wariantów protokołu. Gdy parser nie rozpozna nazwy hosta, obecna logika przepuszcza połączenie.

### 4. Połączenia bez nazwy domenowej

Połączenia bez rozpoznanego Host/SNI, w tym bezpośrednio do adresu IP, są obecnie dozwolone. Sama lista domen nie wystarcza do ich identyfikacji.

### 5. IPv6

Trasa IPv6 jest kierowana do tunelu, ale musi zostać osobno sprawdzona pod kątem wszystkich powyższych ścieżek i wycieków. Samo dodanie trasy IPv6 nie dowodzi, że domeny są filtrowane.

## Kolejność prac przed aktywacją

1. [x] Dodać składanie fragmentów ClientHello i rekordów TLS przed przekazaniem pierwszych bajtów do serwera.
2. [x] Dodać testy parsera HTTP Host i TLS SNI, w tym fragmentację i błędne pakiety.
3. [ ] Zaprojektować kontrolę QUIC/UDP 443 oraz jawnie zdecydować, czy wymuszamy fallback do TCP (z możliwym wpływem na aplikacje).
4. [x] Podłączyć kodek i dekoder DNS-over-TCP do eksperymentalnego przekaźnika portu 53/TCP; [ ] dodać testy end-to-end i sprawdzić wiele zapytań, fragmentację oraz odpowiedzi na urządzeniu; udokumentować ograniczenia DoT/DoH/ECH.
5. [ ] Dodać testy integracyjne DNS, TCP, UDP, IPv4 i IPv6; obecne testy jednostkowe nie zastępują testów pełnego przepływu.
6. [ ] Przeprowadzić testy na fizycznym telefonie przez Wi-Fi i LTE/5G: zwykłe strony, STS, Fortuna, inne domeny testowe, przełączanie sieci, wyłączenie ochrony i restart.
7. [ ] Dopiero po udokumentowaniu wyników rozważyć włączenie runtime.

## Bieżąca weryfikacja

Ostatnia zmiana UDP rozdziela nadawanie i odbiór datagramów, ale jest na razie potwierdzona wyłącznie kompilacją/testami CI. Nie ma jeszcze wyniku rzeczywistego testu Wi-Fi i sieci komórkowej na telefonie Redmi 15C z Androidem 15. Nie oznaczaj tego wariantu jako gotowego do użycia.

## Kryterium wydania

Wydanie nie może być oznaczone jako skuteczny bloker, jeśli testy wykazują obejście przez QUIC, alternatywny DNS, IPv6 lub nieparsowalny TLS. Zielony build CI potwierdza kompilację i testy automatyczne, a nie poprawność działania routingu na telefonie.
