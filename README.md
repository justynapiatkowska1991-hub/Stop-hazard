# STOP HAZARD

Aplikacja Android do blokowania stron hazardowych.

## Aktualny status

- Polityka domen hazardowych i odpowiedzi DNS są objęte testami jednostkowymi.
- Usługa VPN jest obecnie zabezpieczona i nie uruchamia nieprzetestowanego transportu.
- Fabryka silnika zwraca bezpieczny silnik wyłączony, aby nie odcinać całego internetu.
- Ochrona produkcyjna nie jest jeszcze aktywna.

## Następny etap

Podłączenie rzeczywistego, przetestowanego silnika transportu DNS/TCP/UDP oraz testy na urządzeniu Android:

1. zwykłe strony działają;
2. domeny hazardowe są blokowane;
3. Wi-Fi i transmisja komórkowa pozostają dostępne;
4. wyłączenie ochrony przywraca normalny ruch.

Nie należy aktywować ochrony produkcyjnej przed zakończeniem tych testów.

## Audyt bezpieczeństwa integracji

Przed aktywacją silnika należy zapoznać się z [audytem luk filtrowania ruchu](docs/NETSTACK_SECURITY_AUDIT_2026-11.md). W szczególności obecny prototyp nie zapewnia jeszcze pełnego blokowania QUIC/HTTP3, alternatywnych metod DNS ani wszystkich wariantów TLS. Silnik pozostaje domyślnie wyłączony.
