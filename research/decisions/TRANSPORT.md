# Trasporto ADB: decisione e rischi concreti

## OBSERVED — fonti e API

Ho ispezionato il [tag 3.1.1 di LibADB Android](https://github.com/MuntashirAkon/libadb-android/tree/3.1.1), importato nel [modulo locale](../../vendor/libadb/LOCAL_CHANGES.md). `PairingConnectionCtx.start()` esegue TLS/PAKE; `AdbConnection.Builder` accetta chiave, certificato, host, porta e API remota; `open("reboot:")` apre il servizio di riavvio. La release usa queste API reali, senza binario adb del PC.

Dipendenze dichiarate: SPAKE2 Android `2.2.1`, Bouncy Castle provider e PKIX `1.81`, Conscrypt Android `2.5.3`, AndroidX annotations `1.9.1`. Il modulo sorgente mantiene licenze e copyright upstream. La qualificazione delle ABI, dei binari nativi, delle licenze transitive e dei lock della build appartiene al rapporto complessivo G2; la disponibilità di una dipendenza non costituisce prova di compatibilità del telefono.

Il [JNI AOSP del pairing](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/jni/com_android_server_adb_AdbDebuggingManager.cpp) usa `pairing_server_new_no_cert` e invia il GUID del dispositivo; non fornisce una base per presumere che il certificato TLS temporaneo del pairing coincida con quello di adbd. In [Android 14, AdbDebuggingManager](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r1/services/core/java/com/android/server/adb/AdbDebuggingManager.java), il GUID deriva da `persist.adb.wifi.guid`. Il [protocollo PeerInfo](https://android.googlesource.com/platform/packages/modules/adb/+/refs/tags/android-14.0.0_r1/pairing_connection/include/adb/pairing/pairing_connection.h) definisce `ADB_DEVICE_GUID=1`; la costante upstream inutilizzata era erroneamente zero.

## DECISION — associazione e operazioni

Conservo chiave RSA 2048 e certificato generati con Bouncy Castle in `noBackupFilesDir`, tramite `AtomicFile`; il manifest deve inoltre escludere i backup. Non uso chiavi del PC, account o API nascoste. Il certificato dura vent'anni, con controllo di validità al caricamento; dati corrotti o scaduti non rigenerano silenziosamente l'identità. Su API con backup AtomicFile ripristino anche il caso `.bak` senza file base.

Il GUID ottenuto dalla PAKE riuscita viene conservato privatamente. Un nuovo pairing non può sostituirlo con un GUID diverso. Il [generatore AOSP Android 14](https://android.googlesource.com/platform/packages/modules/adb/+/refs/tags/android-14.0.0_r1/daemon/mdns.cpp), `GenerateDeviceGuid()`, costruisce `adb-<ro.serialno>-<sei caratteri alfanumerici>`; lo stesso GUID diventa il nome del servizio mDNS connect. Estraggo rigorosamente il seriale dal GUID autenticato, mantenendo eventuali trattini interni, e ogni `connect` confronta `getprop ro.serialno` con quel seriale prima di restituire una sessione al controller. Un formato non supportato o un seriale diverso/non leggibile blocca l'accesso. Quando AOSP genera un GUID con seriale casuale perché `ro.serialno` manca, la successiva lettura non coincide e il collegamento viene rifiutato. La proprietà protetta `persist.adb.wifi.guid` non viene letta dalla shell. Il core verifica inoltre seriale, modello, API, utente e configurazione. Il codice numerico non è scritto su disco né loggato; l'array del protocollo viene azzerato al termine, mentre le stringhe UI/Java restano soggette alla normale gestione della memoria.

La discovery risolve separatamente `_adb-tls-pairing._tcp.` e `_adb-tls-connect._tcp.`, deduplica endpoint identici e non seleziona il target. Il nome servizio coincidente con il GUID salvato dà priorità alla discovery tramite `matchesPairedService()`. Un endpoint differente può essere provato solo attraverso la verifica del seriale derivato dalla PAKE; un nome mDNS non autentica un dispositivo. Anche il collegamento manuale esegue la stessa verifica prima di qualsiasi scrittura, senza dipendere dalla leggibilità di proprietà protette. Start/stop rilasciano listener e multicast lock. La risoluzione è serializzata per supportare API NSD precedenti. L'inserimento manuale accetta solo IP numerici e porte valide.

Le shell ammettono solo i comandi fissi del contratto e due letture di identità ADB. Ogni risposta ha delimitatori casuali, exit code e limite di 64 KiB; output assente/troncato o exit non zero produce un errore. La newline di stdout è conservata per distinguere una chiave vuota letta da una risposta mancante. Nessun dato UI è interpolato nella shell. Watchdog: 15 secondi per una shell, 12 per il servizio reboot, 30 per il pairing; connect TCP 6 secondi e negoziazione ADB 12. Non c'è retry automatico mutante nel trasporto.

## Rischio materiale — autenticazione del server

`EVIDENCE_FOR`: il client upstream accetta certificati TLS del server senza trust anchor; il protocollo PAKE autentica il pairing e la chiave autorizza il telefono verso adbd. GUID e seriale impediscono la selezione accidentale di altri dispositivi.

`EVIDENCE_AGAINST`: i valori letti dalla shell non equivalgono a una firma del dispositivo. Un avversario attivo sulla rete che conoscesse GUID e seriale potrebbe imitare un server TLS. Il pairing non trasferisce un certificato server persistente da usare come pin per la successiva connessione.

`ALTERNATIVE_EXPLANATIONS`: pinning del certificato temporaneo del pairing potrebbe bloccare connessioni legittime o riavvii; un nome mDNS o IP fisso non risolve l'autenticazione.

`CONFIDENCE`: alta sulla proprietà del codice upstream e sui controlli aggiunti; non è stata svolta una certificazione di sicurezza. Non dichiaro resistenza MITM né autenticazione server forte. Uso previsto: rete locale fidata, senza esporre ADB su Internet.

`MINIMAL_DISCRIMINATING_EXPERIMENT`: sul target designato, verificare che il seriale estratto dal GUID PAKE coincida con `ro.serialno` dopo pairing e riavvio; una discordanza deve bloccare ogni scrittura. La prova non dimostra resistenza contro server ostile. Per tale requisito servirebbe un'identità server autenticabile supportata dal firmware, fuori dai claim di questa implementazione.

## Verifiche e limiti

I test `ShellFrameTest` coprono null/vuoto, framing incompleto, exit fallito, delimitatori duplicati, CRLF e input endpoint. Esecuzione e risultato effettivo vengono riportati nel rapporto G2, senza dedurli dalla presenza dei test. La review incrociata con il core ha individuato e corretto la perdita della newline di una whitelist vuota e il ripristino AtomicFile `.bak`.

Pairing/TLS, corrispondenza fra seriale nel GUID PAKE e `ro.serialno`, discovery multicast e ciclo reale di riavvio sulla Chromecast restano da verificare in G3. La scelta di fallire su formato non supportato o seriale illeggibile preserva l'identità del target ma può richiedere adattamento documentato sul firmware effettivo. Non ho eseguito operazioni su dispositivi fisici.


## PROVEN — ABI e allineamento ELF degli AAR risolti

Ho estratto gli AAR effettivamente risolti dalla cache Gradle del progetto e letto tutti i segmenti `LOAD` con `readelf -lW` (24 ottobre 2026). Entrambi contengono `arm64-v8a`, `armeabi-v7a`, `x86` e `x86_64`; per ogni `.so` tutti i segmenti LOAD hanno `p_align=0x4000` (16 KiB).

| Artefatto | SHA-256 AAR |
| --- | --- |
| `spake2-android-2.2.1.aar` | `8798fb6c04b5d53a6307ed9481e9afe563227abd4e8b9e917c8514fa850071bb` |
| `conscrypt-android-2.5.3.aar` | `551ae4e301c571760d1791e647db6ed1dcb10d34dcae7aa12b67f220f2ce98d1` |

Questa prova riguarda l'allineamento ELF dei due componenti, non l'esecuzione su ogni ABI né l'allineamento ZIP dell'APK finale. Il secondo controllo va eseguito sul deliverable con `zipalign -c -P 16`.

Il POM risolto di SPAKE2 dichiara LGPL-3.0, come il [README del tag 2.2.1](https://github.com/MuntashirAkon/spake2-java/blob/2.2.1/README.md), che attribuisce la parte ED25519 a EdDSA-Java (CC0). Il POM Conscrypt dichiara Apache-2.0. Le attribuzioni delle parti BoringSSL restano nei sorgenti upstream; non vanno eliminate dalla distribuzione dei sorgenti. Il progetto conserva il build modificabile e le coordinate esatte per sostituire/ricompilare la dipendenza LGPL: la selezione Apache del client non la rende Apache.

Il test strumentale `AdbTransportInstrumentedTest` verifica persistenza della chiave e caricamento di Conscrypt. La prova PAKE/TLS contro un vero adbd di emulatore è esplicitamente opt-in (`transportTestEnabled`, `pairingPort`, `connectPort`, `pairingCode`), forza loopback e verifica hardware `ranchu`/`goldfish`; esegue solo pairing, letture e riconnessione. Per la prova senza pairing attivato il caso viene saltato, non dichiarato PASS. Nessun valore segreto deve essere incluso nei comandi pubblicati o nei report.


## Aggiornamento di integrazione — errori e sorgente distribuito

`TransportException` distingue `GUID_MISMATCH`, `PAIRING_FAILED`, `NOT_PAIRED`, `CONNECTION_FAILED` e `IDENTITY_STORAGE`, con messaggi italiani fissi e senza concatenare eccezioni upstream. La negoziazione `connect()` ha ora anche un watchdog che chiude il socket entro 12 secondi, coprendo l'invio iniziale che precede l'attesa della libreria; la costruzione TCP resta limitata a 6 secondi. La successiva lettura del seriale usa il watchdog shell di 15 secondi.

Un pairing errato non altera il GUID salvato. La revoca della chiave può essere risolta con nuovo pairing sul medesimo GUID. Dopo reset del dispositivo o corruzione dello storage, il sistema blocca le scritture: l'operatore verifica e recupera prima la configurazione sul TV, poi può cancellare esplicitamente i dati dell'app e ripetere l'associazione. Non viene cancellato automaticamente lo stato di un'operazione incerta.

[THIRD_PARTY_NOTICES](../../THIRD_PARTY_NOTICES.md) include licenze effettive, attribuzioni e istruzioni di distribuzione. [vendor/spake2-source](../../vendor/spake2-source/README.md) conserva il tag Java 2.2.1 e il submodule nativo allo SHA esatto in archivi verificati, con GPL/LGPL e una ricetta di ricompilazione separata. L'hook `spake2Aar` consente di ricombinare l'app con una libreria modificata senza cambiare il comportamento della build normale. Ho controllato `bash -n`, checksum ed estrazione completa della ricetta; non ho eseguito una build concorrente né una ricompilazione nativa alternativa. Il modulo LibADB ora abilita `lockAllConfigurations()`; il PM deve acquisire il lock nel normale ciclo di build.


## Root cause materiale — GUID non leggibile dalla shell

`OBSERVED`: nella prova con adbd reale dell'emulatore API 35, il PM ha ottenuto PAKE riuscita e GUID salvato, ma il collegamento falliva in verifica identità. La lettura `getprop persist.adb.wifi.guid` produceva solo newline sia dall'APK sia dalla shell ADB del PC sull'emulatore. Il precedente controllo dipendeva quindi da una proprietà non esposta alla shell. Non è una prova sulla Chromecast.

`EVIDENCE_FOR`: PAKE completata, GUID privato non vuoto e discriminante del test `CONNECT_AND_GUID / GUID_MISMATCH`; lettura della stessa proprietà vuota mediante un secondo client. La fonte AOSP assegna la proprietà e la usa in processi privilegiati, senza garantire accesso shell.

`EVIDENCE_AGAINST`: il dato vuoto non identifica da solo una specifica regola SELinux né dimostra che ogni firmware esponga lo stesso comportamento. Non attribuisco il problema a un errore crittografico: la PAKE era riuscita.

`ALTERNATIVE_EXPLANATIONS`: proprietà non impostata, diverso namespace o policy firmware; l'esistenza del GUID autenticato e la lettura vuota da due client rendono insufficiente la strategia di verifica tramite `getprop` di quella proprietà.

`CONFIDENCE`: alta sulla dipendenza non valida; media sull'attribuzione specifica a SELinux, non necessaria alla correzione.

`MINIMAL_DISCRIMINATING_EXPERIMENT`: mantenere PAKE e chiavi identiche, sostituire soltanto il confronto con il seriale estratto dal GUID secondo `GenerateDeviceGuid()` e riletto da `ro.serialno`. Ripetere pairing/connessione/lettura/riconnessione sul medesimo emulatore. Nessuna modifica di permessi o policy. I test puri coprono seriali con trattini, formato malformato, seriale assente o sentinella `unknown`/`null`; il PASS end-to-end deve essere raccolto dopo la build aggiornata.
