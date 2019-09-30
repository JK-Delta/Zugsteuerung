# Zugsteuerung

Mit der Zugsteuerung lassen sich Lego Züge, die das Powered Up System benutzen, steuern.
Die Zugsteuerung besteht aus 2 Teilen:
- Ein Server der sich über Bluetooth mit dem Zug verbindet und eine API zur Steuerung bereitstellt.
- Ein Webinterface über das der Server gesteuert werden kann ([Link](https://github.com/JK-Delta/ZugsteuerungFrontend)).

Dieses Projekt enthält den Server für die Zugsteuerung. Das Webinterface befindet sich [hier](https://github.com/JK-Delta/ZugsteuerungFrontend).
Der Server verbindet sich über Bluetooth mit dem Zug und stellt eine API zur Steuerung bereit.
Die Zugsteuerung wurde dafür entworfen um auf einem Raspberry Pi ausgeführt zu werden.
Die Benutzung der Zugsteuerung hat folgende Vorteile gegenüber der offiziellen App von Lego:
- Mehr als 1 Zug gleichzeitig.
- Steuerung der LED am Zug für jeden Zug einzeln
- Anstatt des Smartphones verbindet sich der Raspberry Pi mit dem Zug. Dadurch wird der Akku des Smartphones
weniger belastet.
- Außerdem fahren die Züge weiter auch wenn der Bildschirm des Smartphones ausgeschaltet wird.
- Noch dazu muss nur der Raspberry Pi in der Nähe des Zugs sein, nicht aber das Gerät von dem aus der Zug
gesteuert wird.
- Da die Steuerung über ein Webinterface läuft, lassen sich die Züge auch vom PC oder Tablet aus steuern.

## Vorraussetzungen
- Raspberry Pi mit Bluetooth Low Energy Unterstützung (z.b. den Typ 3B)
- Linux-basiertes Betriebssystem (z.B. Raspbian Stretch Lite)
- Java 8 oder höher
- Einfacher Webserver (z.B. Lighttpd)

## Installation

Im Folgenden wird erklärt, wie sich der Server über ein Terminal auf einem Raspberry Pi installieren lässt.

Lade zuerst die neuste Version von der Release Seite herunter.
```
wget https://github.com/JK-Delta/Zugsteuerung/releases/download/v1.0.0/Zugsteuerung-1.0.0.jar
```

Damit der Server eine Konfigurationsdatei anlegen kann, sollte er in ein Verzeinis gelegt werden, in dem der
Benutzer Schreibrechte besitzt (z.B. das 'home' Verzeichnis).

Damit der Server funktioniert muss der Benutzer die notwendigen Rechte besitzen um Bluetooth zu benutzen.
Mit dem folgenden Befehl wird der Standardbenutzer *pi* zur Bluetooth Gruppe hinzugefügt:
```
sudo usermod -a -G bluetooth pi
```
Falls ein anderes System als ein Raspberry Pi mit Raspbian Stretch benutzt wird, sind eventuell weitere
Schritte (je nach System) notwendig um Bluetooth korrekt einzurichten.

Danach sollte der Raspberry Pi neugestartet werden:
```
sudo shutdown -r
```
Damit ist die Installation abgeschlossen.

## Benutzung

Der Server wird mit dem folgenden Befehl gestartet:

```
java -jar Zugsteuerung-1.0.0.jar --address=<Serveradresse>
```
Der Parameter *--address* ist unbedingt erforderlich um Anfragen aus dem Frontend zuzulassen (aufgrund von 'CORS').
Hierbei gilt, dass der übergebene Parameter dem entsprechen muss, was als Server Adresse im Webbrowser
angegeben wird.

Beispiel: Wenn das Webinterface mit *http://raspberrypi.local/* aufgerufen wird, dann muss als Parameter
```
--address=raspberrypi.local
``` 
übergeben werden (ohne *http://* und ohne */* am Ende).
Wird der Webserver über einen anderen Port aufgerufen, so muss dieser Port auch im Parameter übergeben werden.

Neben dem Server muss auch das Webinterface installiert werden ->[hier](https://github.com/JK-Delta/ZugsteuerungFrontend).

## Lizenz
Dieses Projekt steht unter der [Apache License 2.0](https://spdx.org/licenses/Apache-2.0.html)-Lizenz zur Verfügung.
