# Audyt bezpieczeństwa integracji netstack — listopad 2026

## Status

Silnik NetValve pozostaje eksperymentalny. Nie włączaj `ENABLE_NETSTACK_RUNTIME` w wydaniu dla użytkowników i nie publikuj APK jako działającego blokera hazardu, dopóki poniższe luki nie zostaną zamknięte i zweryfikowane na urządzeniu.

## Poprawki wykonane

- Dodano brakującą implementację `TrafficFilterEngine.isRunning()` w adapterze NetValve.
- Nieudane utworzenie interfejsu VPN nie kończy już metody `start()` z pozostawioną flagą `running=true`.
- Deskryptor interfejsu TUN jest przechowywany i zamykany podczas sprzątania.
- Ścieżka błędu uruchomienia wywołuje wspólne `stop()`, które zamyka utworzony tunel i TUN.

## Luki blokujące wydanie

### 1. QUIC / HTTP/3

Ruch UDP jest przekazywany bez inspekcji nazwy domeny. Dotyczy to m.in. QUIC/HTTP/3 na UDP 443. Klient może więc ominąć kontrolę hosta opartą o HTTP Host i TLS SNI.

### 2. DNS poza klasycznym UDP/53

Obecny filtr DNS jest wywoływany tylko dla UDP na porcie 53. Nie obejmuje automatycznie DNS przez TCP/53, DNS-over-TLS (853), DNS-over-HTTPS ani własnych resolverów aplikacji.

### 3. Niepełna widoczność TLS

Adapter sprawdza pierwszy odczyt strumienia TCP. ClientHello może być podzielony na kilka odczytów, a TLS Encrypted Client Hello (ECH) może ukrywać nazwę serwera. Gdy parser nie rozpozna hosta, obecna logika przekazuje połączenie dalej.

### 4. Połączenia bez nazwy domenowej

Połączenia bez rozpoznanego Host/SNI, w tym bezpośrednio do adresu IP, są obecnie dozwolone. Sama lista domen nie wystarcza do ich identyfikacji.

### 5. IPv6

Trasa IPv6 jest kierowana do tunelu, ale musi zostać osobno sprawdzona pod kątem wszystkich powyższych ścieżek i wycieków. Samo dodanie trasy IPv6 nie dowodzi, że domeny są filtrowane.

## Kolejność prac przed aktywacją

1. Dodać buforowanie i składanie danych TLS przed analizą ClientHello; obsłużyć wiele rekordów/fragmentów lub bezpiecznie oznaczać nieznany host.
2. Dodać testy parsera HTTP Host i TLS SNI, w tym fragmentację i błędne pakiety.
3. Zaprojektować kontrolę QUIC/UDP 443 oraz jawnie zdecydować, czy wymuszamy fallback do TCP (z możliwym wpływem na aplikacje).
4. Zaprojektować obsługę DNS TCP i udokumentować ograniczenia DoT/DoH/ECH.
5. Dodać testy integracyjne DNS, TCP, UDP, IPv4 i IPv6.
6. Przeprowadzić testy na fizycznym telefonie przez Wi-Fi i LTE/5G: zwykłe strony, STS, Fortuna, inne domeny testowe, przełączanie sieci, wyłączenie ochrony i restart.
7. Dopiero po udokumentowaniu wyników rozważyć włączenie runtime.

## Kryterium wydania

Wydanie nie może być oznaczone jako skuteczny bloker, jeśli testy wykazują obejście przez QUIC, alternatywny DNS, IPv6 lub nieparsowalny TLS. Zielony build CI potwierdza kompilację i testy automatyczne, a nie poprawność działania routingu na telefonie.
