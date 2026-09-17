# Prototyp silnika ruchu — STOP HAZARD

## Cel

Przygotować osobny, mały prototyp silnika ruchu przed podłączeniem go do aplikacji produkcyjnej.

## Zakres pierwszej wersji

- Android `VpnService` uruchamiany wyłącznie po świadomej zgodzie użytkownika.
- Przekazywanie zwykłego ruchu bez filtrowania jako test transportu.
- Obsługa DNS i TCP/HTTPS jako minimum.
- Brak automatycznego startu po restarcie telefonu.
- Natychmiastowe zamknięcie tunelu po wyłączeniu ochrony.
- Bez `0.0.0.0/0` w produkcyjnej aplikacji, dopóki transport nie przejdzie testów.
- Brak integracji z `TrafficFilterEngineFactory` na etapie prototypu.

## Zakaz

Nie wolno podłączać prototypu do głównej aplikacji, dopóki nie zostaną wykonane testy na rzeczywistym telefonie:

1. Wi-Fi — zwykłe HTTPS działa.
2. LTE/5G — zwykłe HTTPS działa.
3. DNS — dozwolone domeny rozwiązują się poprawnie.
4. Wyłączenie ochrony przywraca internet bez restartu.
5. Błąd silnika nie odcina internetu.
6. Nie ma wycieku ani blokady całego ruchu.

## Kryterium zakończenia etapu

Etap uznajemy za zakończony dopiero po zapisaniu wyników testów z modelem telefonu, wersją Androida, rodzajem sieci i logami błędów.

Do tego czasu `TrafficFilterEngineFactory` musi nadal zwracać `DisabledTrafficFilterEngine`.
