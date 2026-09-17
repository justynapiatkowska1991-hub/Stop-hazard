# Ocena kandydata na silnik ruchu — STOP HAZARD

## Cel

Wybrać silnik, który pozwoli blokować wyłącznie domeny hazardowe bez odcinania zwykłego internetu.

## Warunki obowiązkowe

- Android `VpnService` z poprawnym przekazywaniem ruchu.
- Obsługa zwykłego TCP/HTTPS.
- Obsługa DNS z możliwością zastosowania `DnsFilterPolicy`.
- Brak przejmowania ruchu przed świadomym włączeniem ochrony.
- Bezpieczne zatrzymanie i natychmiastowy powrót internetu.
- Brak wymagania roota.
- Licencja pozwalająca na użycie w projekcie STOP HAZARD.
- Możliwość testów na rzeczywistym telefonie przez Wi-Fi i LTE/5G.

## Kandydaci do dalszego audytu

### 1. NetValve

Do sprawdzenia:

- rzeczywisty moduł przekazywania ruchu,
- obsługa TCP/UDP/DNS,
- zależności i ich licencje,
- możliwość wydzielenia komponentów bez kopiowania całego projektu,
- zachowanie przy błędzie i zatrzymaniu usługi.

### 2. Pure Kotlin userspace TCP/IP stack

Do sprawdzenia:

- kompletność obsługi TCP,
- stabilność HTTPS,
- obsługa DNS i UDP,
- wydajność oraz zużycie baterii,
- zgodność z Androidem bez NDK.

### 3. Tun2socks

Nie może zostać użyty samodzielnie. Wymaga kompletnego, przetestowanego transportu oraz prawidłowego routingu. Samo podłączenie tun2socks nie jest dowodem, że internet będzie działał.

## Zasada wdrożenia

Żaden kandydat nie zostanie podłączony do `TrafficFilterEngineFactory`, dopóki nie przejdzie testów rzeczywistego ruchu opisanych w `REAL_TRAFFIC_TEST_PLAN.md`.

## Status

- Kandydat produkcyjny: **nie wybrano**.
- Ochrona produkcyjna: **wyłączona**.
- Następny krok: audyt jednego konkretnego silnika i przygotowanie minimalnego prototypu poza główną ścieżką aplikacji.
