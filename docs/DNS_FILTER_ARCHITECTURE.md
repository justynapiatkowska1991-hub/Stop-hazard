# Stop Hazard — architektura bezpiecznego blokowania

## Cel

Blokować wyłącznie domeny hazardowe bez przejmowania całego ruchu internetowego przez `tun2socks`.

## Zasady

1. Nie używać trasy `0.0.0.0/0`.
2. Nie przekierowywać całego TCP/UDP przez własny proxy.
3. Najpierw wdrożyć lokalny filtr DNS i testy na prawdziwym telefonie.
4. Awaria filtra nie może wyłączać internetu.
5. Dopiero po stabilnym DNS dodać ochronę przed DoH/DoT i obejściami.

## Etapy

### Etap 1 — stabilny DNS

- parser zapytań DNS UDP/TCP;
- rozpoznawanie domen przez `DomainFilter`;
- odpowiedź blokująca dla domen hazardowych;
- przekazywanie domen dozwolonych do upstream DNS;
- IPv4 i IPv6;
- timeout i bezpieczny fallback;
- testy: `sts.pl` i `fortuna.pl` blokowane, `google.com` dozwolone.

### Etap 2 — odporność

- ochrona przed DNS-over-TLS;
- ograniczenie DNS-over-HTTPS;
- kontrola QUIC/IPv6;
- ponowne uruchamianie po restarcie telefonu;
- testy Wi-Fi i LTE/5G.

### Etap 3 — zabezpieczenia użytkownika

- trwałe ustawienia ochrony;
- ochrona przed przypadkowym wyłączeniem;
- czytelny ekran blokady;
- aktualizacja listy domen;
- dziennik ostatnich blokad bez zapisywania pełnej historii przeglądania.

## Kryterium gotowości

APK nie może być uznane za gotowe, dopóki zwykłe strony, bankowość, Wi-Fi i dane komórkowe nie działają równocześnie z blokadą domen testowych.
