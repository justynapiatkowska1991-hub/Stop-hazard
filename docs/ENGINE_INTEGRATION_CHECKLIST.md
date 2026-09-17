# Checklista integracji rzeczywistego silnika — STOP HAZARD

## Cel

Podłączyć silnik przekazywania ruchu dopiero wtedy, gdy spełnia wymagania bezpieczeństwa. Sama obecność `VpnService` nie oznacza, że internet będzie działał.

## Warunki przed integracją

- [ ] Silnik ma jasno opisaną licencję.
- [ ] Silnik obsługuje przekazywanie zwykłego ruchu, a nie tylko przechwytywanie pakietów.
- [ ] Udokumentowana obsługa TCP.
- [ ] Udokumentowana obsługa UDP albo jawne ograniczenie zakresu.
- [ ] Obsługa DNS i możliwość zastosowania `DnsFilterPolicy`.
- [ ] Brak wymogu roota.
- [ ] Brak użycia nieutrzymywanego lub nieznanego binarium.
- [ ] Testy na Wi-Fi i LTE/5G.
- [ ] Test wyłączenia ochrony przywraca internet bez restartu i odinstalowania.

## Zasada podłączenia

1. Najpierw przygotować osobny adapter silnika.
2. Nie zmieniać `TrafficFilterEngineFactory`, dopóki adapter nie przejdzie testów.
3. Nie uruchamiać tunelu z trasą `0.0.0.0/0`, jeśli nie ma działającego transportu przekazującego ruch.
4. Przy błędzie startu silnik musi pozostać wyłączony.
5. Ochrona ma blokować wyłącznie domeny z listy hazardowej.
6. Zwykłe strony, aplikacje i transmisja danych muszą nadal działać.

## Testy akceptacyjne

- [ ] Zwykła domena HTTPS działa przed ochroną.
- [ ] Zwykła domena HTTPS działa po włączeniu ochrony.
- [ ] Domena hazardowa jest blokowana po włączeniu ochrony.
- [ ] Domena hazardowa nie jest blokowana przed włączeniem ochrony.
- [ ] Wi-Fi działa.
- [ ] LTE/5G działa.
- [ ] IPv4 działa.
- [ ] IPv6 nie jest psute, jeśli jest dostępne.
- [ ] Wyłączenie ochrony przywraca internet.
- [ ] Ponowne uruchomienie telefonu nie włącza ochrony bez zgody użytkownika.
- [ ] Awaria silnika nie odcina internetu.

## Decyzja

Do czasu odhaczenia wszystkich krytycznych punktów fabryka pozostaje przy `DisabledTrafficFilterEngine`.
