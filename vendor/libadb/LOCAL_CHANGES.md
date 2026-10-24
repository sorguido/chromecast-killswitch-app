# Provenienza e modifiche locali

Sorgente: [LibADB Android 3.1.1](https://github.com/MuntashirAkon/libadb-android/tree/3.1.1), autore Muntashir Al-Islam. Consultazione e importazione: 24 ottobre 2026. Mantengo gli SPDX e le licenze upstream; per i file duali uso Apache-2.0, senza alterare le licenze degli altri file.

Archivio originale `https://github.com/MuntashirAkon/libadb-android/archive/refs/tags/3.1.1.tar.gz`, SHA-256 `3afbe40b278ddea4480ebb8e7b707404f827d8e41b383e12f4b1b0b82dab1d84`.

Importati `libadb/src/main` e `COPYING`/`LICENSES`; il modulo Gradle è locale. La crittografia SPAKE2, HKDF, AES-GCM e il protocollo di pairing restano upstream.

Modifiche del 24 ottobre 2026:

- `AdbConnection`: connessione TCP entro 6 secondi; letture TCP/TLS entro 15 secondi; socket chiuso se il costruttore fallisce; attesa thread in chiusura limitata a 2 secondi. Rifiuto di AUTH/CNXN privi di TLS e limite del payload negoziato alla capacità del client. Rimossi stack trace di eccezioni che potrebbero esporre endpoint.
- `PairingConnectionCtx`: socket cancellabile, connect entro 6 secondi, letture entro 12 secondi, chiusura null-safe. `destroy()` separato dalla cancellazione per evitare distruzione concorrente del contesto SPAKE2. Nessun log del PeerInfo. Esposizione del solo GUID ricevuto dopo PAKE riuscita, validazione del discriminante AOSP `ADB_DEVICE_GUID=1` e della stringa terminata da NUL. L'app deriva dal GUID il seriale secondo `GenerateDeviceGuid()` AOSP, senza leggere la proprietà protetta dalla shell.
- `SslUtils`: contesto TLS creato per la chiave corrente, senza cache statica di una precedente identità; eliminato messaggio stdout. La politica upstream di trust TLS rimane invariata: vedere il limite documentato nel [dossier trasporto](../../research/decisions/TRANSPORT.md).
- `AdbInputStream`/`AdbStream`: drenaggio dei byte già ricevuti anche dopo CLSE, EOF senza attesa indefinita su coda vuota con chiusura pendente e byte singolo senza estensione del segno.

L'app usa `AdbConnection.Builder`, `PairingConnectionCtx` e `open("reboot:")`. Non usa gli helper di connessione automatica upstream, che possono scegliere il primo dispositivo trovato.
