# P0 — baseline di implementazione

## OBSERVED — 24 ottobre 2026

Il repository era al commit `4a5d69d` con bootstrap e nessun codice Android. Il telefono comunicato dall’utente è Motorola edge 40 neo (XT2307-1), Android 15 (`USER_REPORTED`); nessuna prova fisica è ancora attribuita all’APK.

## DECISION — percorso PRIMARY consolidato

App Kotlin nativa, Compose e Material 3, application ID `it.sorguido.chromecastkillswitch`, nome «Chromecast Kill Switch», versione iniziale `1.0.0` / code `1`. Un modulo app e un modulo del client upstream con correzioni puntuali; nessun percorso alternativo sviluppato.

| Componente | Versione fissata / esito |
| --- | --- |
| JDK eseguito | OpenJDK 25.0.4.1 dell’host, bytecode JVM17; nessuna installazione host |
| Gradle | 9.6.0, wrapper con SHA-256 ufficiale |
| Android Gradle Plugin | 9.4.0 |
| Kotlin / compiler Compose | 2.2.10 |
| SDK | min26, compile36, target36, Build Tools36.0.0 |
| Compose UI / Material3 | 1.7.8 / 1.3.2 |
| Activity / Lifecycle | 1.10.1 / 2.8.7 |
| Coroutines | 1.10.2 |
| Client ADB | sorgente LibADB3.1.1, patch locali documentate |
| TLS / crittografia | Conscrypt2.5.3, Bouncy Castle1.81, SPAKE2 Android2.2.1 |
| Test | JUnit4.13.2, AndroidX runner1.6.2, Compose test1.7.8 |

Le versioni non sono dinamiche; lock Gradle e metadati SHA-256 rendono visibile il grafo risolto. Le versioni UI selezionate sono stabili e compatibili con min26, senza richiedere l’adozione dell’ultima versione disponibile. [AGP9.4 ufficiale](https://developer.android.com/build/releases/agp-9-4-0-release-notes) prescrive Gradle9.6 e Build Tools36; [Compose compiler](https://developer.android.com/jetpack/androidx/releases/compose-kotlin) usa il plugin Kotlin dalla versione2.0.

## PROVEN — ambito locale

Build release firmata, test unitari e lint eseguiti con successo nella prima integrazione; la qualifica finale della revisione è mantenuta nel rapporto P2. Le due dipendenze crittografiche native comprendono quattro ABI con segmenti ELF16KiB; anche l’allineamento ZIP dell’APK è stato verificato. Licenze e sorgenti sono conservati: [trasporto](../../research/decisions/TRANSPORT.md), [attribuzioni](../../THIRD_PARTY_NOTICES.md).

Le credenziali ADB sono proprie del telefono, in `noBackupFilesDir`, senza backup. Pairing e connessione richiedono TLS; GUID autenticato durante PAKE e seriale letto impediscono selezione accidentale di altri dispositivi. Non si dichiara autenticazione server forte contro MITM: la limitazione upstream e l’uso su rete locale fidata sono espliciti nel dossier trasporto.

Permessi: INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE e CHANGE_WIFI_MULTICAST_STATE. Su Android15 non è previsto un popup di autorizzazione della rete locale per questi permessi. Target36 non adotta il nuovo obbligo target37; errori SecurityException restano visibili con indicazioni di recupero. [Fonte Android](https://developer.android.com/privacy-and-security/local-network-permission). Non dichiaro Android17 collaudato.

Firma RSA3072 PKCS12 generata privatamente in `.local/signing`, configurazione `signing.properties` esclusa da Git; nessuna firma debug usata per la release. La chiave deve restare disponibile per aggiornamenti, e il backup privato controllato dall’utente è descritto nella guida di build.

## Rischi e prove aperte

La compatibilità dichiarata è Android8+/API26+ nelle ABI incluse. Quella provata è limitata alle esecuzioni riportate in P2; Android8 e le quattro ABI non sono automaticamente tutte collaudate. Sul Motorola e Chromecast restano obbligatori pairing, NSD, GUID del firmware, riavvio, ON/OFF e test funzionale del blocco traffico secondo G3.

Il percorso è implementabile ed è stato compilato. L’assenza del dispositivo fisico non ha impedito implementazione, firma o QA locale. G0 è soddisfatto nell’ambito toolchain, dipendenze, licenze e rischi; non anticipa G3/G4.
