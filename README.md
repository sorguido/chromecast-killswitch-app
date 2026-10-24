# Chromecast Kill Switch

Telecomando Android locale per la Chromecast con Google TV HD associata: configura insieme always-on e kill switch Android per Proton VPN, verifica le impostazioni e richiede il riavvio. Ogni operazione richiede una conferma. Non misura lo stato del tunnel VPN.

Il prodotto è implementato; la qualifica fisica sul **Motorola edge 40 neo (XT2307-1), Android 15**, e sulla Chromecast rimane obbligatoria. Le prove locali non equivalgono al collaudo finale.

## Installazione e uso

1. Usa l’APK release indicato nel [rapporto qualità](evidence/current/QUALITY_REPORT.md), verificandone SHA-256. Sul telefono apri l’APK e consenti l’installazione da quella sorgente se Android lo richiede.
2. Collega telefono e Chromecast alla rete locale fidata. Sul TV apri **Impostazioni → Sistema → Opzioni sviluppatore → Debug wireless → Associa tramite codice**.
3. Nell’app scegli **Associa la Chromecast**, seleziona il servizio o inserisci IP e porta di associazione, quindi il codice. Il telefono usa una propria chiave: non serve il PC.
4. Torna alla schermata principale Debug wireless del TV. Nell’app scegli il servizio di collegamento o inserisci la relativa porta, diversa da quella del codice.
5. Dopo l’identificazione e la lettura, scegli **Attiva e riavvia** o **Disattiva e riavvia**. Leggi il popup e conferma. L’app verifica il salvataggio e, dopo il nuovo avvio, rilegge lo stato.

Se la connessione non torna, riattiva Debug wireless dal TV e usa **Rileggi lo stato** o il collegamento manuale. L’app non ripete da sola scritture e riavvii incerti. La guida integrata funziona offline; il [contratto](docs/COMMAND_CONTRACT.md) contiene anche il recupero USB. OFF rimuove gli automatismi Android; Auto-connect di Proton rimane separato.

## Build riproducibile

Toolchain e limiti di compatibilità: [baseline](docs/phase-specific/P0_BUILD_BASELINE.md). Servono JDK compatibile con Gradle9.6, SDK36 e Build Tools36.0.0. Configura `sdk.dir` in `local.properties` oppure `ANDROID_HOME`. Il wrapper, i lock e i checksum delle dipendenze sono inclusi; cache e tool locali restano in `.local/`.

```sh
./scripts/build.sh
```

La firma release richiede `signing.properties` privato, con `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. Nella postazione di sviluppo la chiave persistente è in `.local/signing/release.p12`. Senza configurazione di firma Gradle produce un APK unsigned, da non qualificare come release installabile. Nessun fallback alla firma debug.

Conserva un backup privato della chiave e di `signing.properties` su un supporto controllato: sono necessari per aggiornare l’app già installata. Non inserirli in Git, email, log o allegati pubblici. Chi ricompila può usare una propria chiave; in tal caso Android richiede la disinstallazione dell’APK precedente, perdendo associazione e pending. Verifica prima lo stato della Chromecast.

Test UI su un emulatore dedicato:

```sh
GRADLE_USER_HOME="$PWD/.local/gradle" ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5580 shell am instrument -w it.sorguido.chromecastkillswitch.test/androidx.test.runner.AndroidJUnitRunner
```

Seleziona sempre il seriale dell’emulatore effettivamente dedicato ai test. Il caso di pairing reale è opt-in e accetta soltanto il loopback dell’emulatore; senza parametri viene saltato. Gli scenari UI sintetici esistono soltanto nei test e non sono inclusi nella release.

## Evidenze e sorgenti

- [Requisiti](docs/PRODUCT_SPEC.md), [contratto](docs/COMMAND_CONTRACT.md), [roadmap](ROADMAP.md).
- [Implementazione](research/decisions/P1_IMPLEMENTATION.md), [qualità locale](evidence/current/QUALITY_REPORT.md), [procedura reale](docs/phase-specific/DEVICE_ACCEPTANCE.md).
- [Attribuzioni e licenze](THIRD_PARTY_NOTICES.md); client ADB modificato in `vendor/libadb`, sorgente SPAKE2 e procedura di ricombinazione in `vendor/spake2-source`.

Il client upstream non offre autenticazione forte del server TLS contro un avversario attivo sulla LAN: usa soltanto una rete locale fidata e non esporre ADB su Internet. I controlli GUID/seriale bloccano target estranei, senza trasformare il prodotto in una certificazione di sicurezza della rete.

Per riprendere lo sviluppo leggi [START.md](START.md); l’obiettivo resta aperto finché G4 non è provato.
