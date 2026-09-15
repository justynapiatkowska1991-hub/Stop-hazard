# Bezpieczny filtr stron hazardowych

## Cel

Blokować wyłącznie domeny hazardowe i pozostawić zwykły internet działający.

## Zasady bezpieczeństwa

1. Nie kierować całego ruchu przez niesprawdzony SOCKS5/tun2socks.
2. Ochrona ma działać w trybie allow-by-default: niezablokowana domena jest przepuszczana.
3. Przy błędzie filtra VPN musi zostać zamknięty, aby nie odcinać internetu.
4. Nie włączać ochrony, dopóki nie przejdą testy Wi‑Fi, LTE, DNS, HTTPS i IPv6.
5. IPv6 nie może być pozostawione jako niekontrolowane obejście.

## Kolejność implementacji

1. Dodać osobny moduł rozpoznawania domen i testy jednostkowe.
2. Przygotować lokalny filtr DNS z odpowiedzią blokującą wyłącznie dla domen z `BlockedDomains`.
3. Dodać kontrolowane uruchamianie VPN oraz natychmiastowe wycofanie konfiguracji przy błędzie.
4. Dodać testy urządzeniowe: zwykłe strony, domeny blokowane, Wi‑Fi, LTE, przełączanie sieci i wyłączenie ochrony.
5. Dopiero po pozytywnych testach zbudować APK.

## Ważne

Obecny tryb awaryjny celowo nie uruchamia VPN. Blokowanie jest chwilowo wyłączone, ponieważ wcześniejszy tunel przejmował cały ruch i powodował utratę internetu.
