# Zewnętrzny silnik ruchu — plan integracji STOP HAZARD

## Cel

STOP HAZARD ma korzystać z dojrzałego, zewnętrznego silnika obsługi ruchu sieciowego, zamiast implementować własny transport VPN od zera.

Docelowy przepływ:

STOP HAZARD
→ Android VpnService
→ zewnętrzny silnik userspace TCP/IP
→ filtr domen hazardowych
→ bezpośrednie połączenie z Internetem

Aplikacja nie ma korzystać z zewnętrznego serwera VPN.

## Kandydat do integracji

**NetValve (IEAmir/NetValve)** został wybrany jako główny kandydat do dalszej integracji.

Publiczna dokumentacja projektu opisuje:
- Android VpnService,
- userspace network stack oparty o gVisor,
- obsługę TCP i UDP,
- IPv4 i IPv6,
- ochronę socketów upstream przez VpnService.protect(),
- produkcyjny wariant z gVisor netstack,
- licencję Apache-2.0.

To są cechy potrzebne do rozwiązania problemu, który wcześniej powodował odcięcie całego Internetu.

## Ważne ograniczenie

Nie integrujemy jeszcze silnika do działającej ochrony.

Domyślny wariant NetValve jest wariantem loopback/dev i nie zapewnia prawdziwego przekazywania ruchu. Produkcyjne przekazywanie wymaga zbudowania wariantu netstack.

Dlatego nie wolno podłączać wersji loopback do przycisku OCHRONA.

## Zasady integracji

1. Zachować obecny interfejs `TrafficFilterEngine`.
2. Dodać osobny adapter dla zewnętrznego silnika.
3. Nie uruchamiać silnika, dopóki build produkcyjnego netstack nie zostanie potwierdzony.
4. Filtr STOP HAZARD ma blokować tylko domeny z `BlockedDomains`.
5. Zwykły ruch musi być przekazywany dalej.
6. TCP, UDP, IPv4, IPv6 i DNS muszą być przetestowane.
7. Wyłączenie ochrony musi natychmiast przywracać zwykły Internet.
8. Awaria silnika nie może pozostawić telefonu bez Internetu.
9. Nie kopiować całego repozytorium NetValve; używać wyłącznie niezbędnych komponentów zgodnie z ich licencjami.
10. Przed wydaniem APK wykonać testy na rzeczywistym urządzeniu przez Wi-Fi i sieć komórkową.

## Kolejność

### Etap A — przygotowanie
- obecne testy STOP HAZARD muszą być zielone,
- adapter pozostaje domyślnie wyłączony,
- brak zmian w działającym ruchu użytkownika.

### Etap B — silnik
- przygotowanie produkcyjnego wariantu netstack,
- integracja z `TrafficFilterEngine`,
- test zwykłego HTTP/HTTPS,
- test DNS,
- test TCP/UDP,
- test IPv4/IPv6.

### Etap C — filtr
- połączenie silnika z polityką `DnsFilterPolicy`,
- blokowanie domen hazardowych,
- dozwolone domeny pozostają dostępne.

### Etap D — urządzenie
- Wi-Fi,
- LTE/5G,
- przełączanie Wi-Fi ↔ LTE,
- restart aplikacji,
- restart telefonu,
- wyłączenie ochrony,
- awaria silnika.

### Kryterium włączenia ochrony

Przycisk OCHRONA może zostać aktywowany dopiero wtedy, gdy produkcyjny silnik przejdzie testy zwykłego Internetu i blokowania hazardu na prawdziwym urządzeniu.

## Stan

**Wybrany kandydat:** NetValve  
**Integracja produkcyjna:** jeszcze nieaktywna  
**VPN STOP HAZARD:** nadal bezpiecznie wyłączony
