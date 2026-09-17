# Plan testów rzeczywistego ruchu — STOP HAZARD

## Cel

Potwierdzić, że przyszły silnik VPN przekazuje zwykły internet i nie powoduje utraty łączności.

## Zasada bezpieczeństwa

Do czasu spełnienia wszystkich kryteriów silnik pozostaje wyłączony w `TrafficFilterEngineFactory`.

## Testy obowiązkowe

1. Uruchomienie aplikacji bez ochrony.
2. Wi-Fi: otwarcie kilku zwykłych stron HTTPS.
3. LTE/5G: otwarcie tych samych stron.
4. DNS: rozwiązywanie zwykłych domen.
5. IPv4: połączenie HTTPS.
6. IPv6: połączenie HTTPS, jeśli operator i urządzenie je obsługują.
7. TCP: zwykłe połączenie HTTPS i pobranie małego pliku.
8. UDP: test aplikacji korzystającej z UDP, jeśli silnik deklaruje obsługę UDP.
9. Włączenie ochrony: zwykłe strony nadal muszą działać.
10. Wyłączenie ochrony: internet musi wrócić bez restartu i bez odinstalowania aplikacji.
11. Restart telefonu: aplikacja nie może samoczynnie przejąć ruchu bez świadomego włączenia ochrony.
12. Błąd silnika: aplikacja ma pozostać bezpiecznie wyłączona, a internet ma działać.

## Kryterium zaliczenia

Test uznajemy za zaliczony dopiero wtedy, gdy:

- zwykłe domeny działają na Wi-Fi i LTE/5G,
- DNS działa dla dozwolonych domen,
- HTTPS działa po IPv4,
- IPv6 nie jest psute, jeśli jest dostępne,
- wyłączenie ochrony przywraca internet,
- aplikacja nie wymaga odinstalowania do odzyskania połączenia,
- domeny testowe z listy blokad są blokowane dopiero po podłączeniu kompletnego silnika.

## Wynik

Wyniki należy zapisać wraz z modelem telefonu, wersją Androida, rodzajem sieci i godziną testu. Sam pomyślny build GitHub Actions nie oznacza, że routing działa na urządzeniu.
