# P2 — qualifica locale completata

`G2: PASS` nell’ambito locale, 24 ottobre 2026. G3 e G4 restano aperti: nessuna prova fisica sul Motorola o sulla Chromecast è stata inventata o sostituita da fixture.

## Artefatto qualificato

| Voce | Valore verificato |
| --- | --- |
| APK | `deliverables/1.0.0/chromecast-killswitch-1.0.0.apk` |
| Versione / package | `1.0.0` (1), `it.sorguido.chromecastkillswitch` |
| Commit sorgente | `0d1265e1d621e3251281daf79c0ecccbd7de7f99` |
| SHA-256 APK | `8eb281a2ebb21807df137513702e3b03647e0ff5692c543e59350be8783f8dba` |
| Certificato SHA-256 | `2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc` |
| API | min26, compile36, target36 |
| Esecuzione locale | Android15/API35, x86_64, AVD720×1600, density320 |
| Telefono designato | Motorola edge40neo (XT2307-1), Android15, `USER_REPORTED`; non ancora collaudato |

Le successive modifiche documentali non cambiano il commit sorgente dell’APK. I metadati Git incorporati nell’APK corrispondono al commit sopra.

## PROVEN — build, firma e riproducibilità

Eseguito `./scripts/build.sh`: test unitari, lint release e APK firmato riusciti. Toolchain e dipendenze sono fissate nella [baseline](../../docs/phase-specific/P0_BUILD_BASELINE.md), nei lock Gradle e nei metadati SHA-256. Nessuna versione dinamica risolta senza lock.

Il bundle Git consegnabile è stato clonato in una directory indipendente, configurato con SDK e firma locali, e ricompilato con `./gradlew --offline --no-build-cache clean :app:assembleRelease`. **Il file risultante è identico byte per byte al deliverable.** Anche `base.apk` estratto dall’installazione nell’emulatore ha lo stesso SHA-256. `apksigner verify` e `zipalign -c -P 16 4` sono riusciti sul file finale.

Il bundle conserva il commit necessario ai metadati VCS; il tar è disponibile per consultazione e ricombinazione dei sorgenti. Per la riproduzione esatta usare il bundle, la stessa toolchain e la medesima chiave privata. Nessuna chiave o password è inclusa nei sorgenti consegnabili.

Il manifest finale espone quattro permessi di rete e il permesso interno AndroidX con protezione signature. Nessun permesso telefonico, storage condiviso, root, VPN o accessibilità. I backup e il trasferimento dati sono esclusi; le identità sono in `noBackupFilesDir`.

## PROVEN — test e ambito

| Prova | Risultato | Ambito effettivo |
| --- | --- | --- |
| JUnit dominio | 18/18 | Target/profilo/provider estranei, stato parziale, readback, ordine OFF, errore scrittura, pending, boot e assenza replay |
| JUnit trasporto | 5/5 | Framing, output vuoto/null, exit code, troncamento, endpoint, parser GUID/seriale |
| UI strumentale | 11/11 | Conferma a ogni tap, annullamento, mutua esclusione, guida offline, ricerca, temi e font200 reale |
| Storage/TLS strumentale | 2/2 | Recupero AtomicFile `.bak`/`.new`, pending, persistenza chiave e caricamento Conscrypt |
| ADB reale nell’emulatore | 1/1 | PAKE/TLS con vero adbd, seriale vincolato, shell, riconnessione con credenziali salvate |
| Lint release | 0 errori, 19 warning esaminati | Versioni più recenti/target37, codice upstream obsoleto e trust TLS documentato |
| APK release finale | PASS locale | Installazione, avvio, riapertura, temi, onboarding, guida e identità binaria |

Totale: **23 test unitari e 14 strumentali passati**, senza contare casi saltati come PASS. [Ricevuta sanitizzata](local-checks.json).

I test strumentali usano la variante debug con lo stesso codice di produzione; fixture e Activity di test sono escluse dalla release. Dopo quei test le sole modifiche al prodotto sono state l’esclusione del blocco Play cifrato e dei permessi impliciti inutilizzati: nessuna logica applicativa o crittografica è cambiata. L’APK release finale ha verifiche di installazione/rendering e checksum proprie. Il ciclo ON/OFF sulla Chromecast richiede comunque G3 sul medesimo APK.

## OBSERVED — QA visiva e accessibilità

Ho ispezionato le [schermate release](screenshots/release/) effettive e gli [scenari UI sintetici](screenshots/evidence/), esplicitamente marcati come tali. Temi, gerarchia, spaziatura e azioni risultano leggibili; lo stato parziale e quello non verificabile non sono presentati come OFF/ON certi. Font200 è stato applicato nelle impostazioni Android dell’emulatore: popup e pulsanti restano raggiungibili scorrendo; il valore1.0 è stato ripristinato.

Contrasto statico minimo dei colori testo normali:6,42:1. Bersagli minimi48dp, principali56dp. Semantica con intestazioni, etichette e annunci di stato. La prova TalkBack sul Motorola resta D13, senza attribuirle un PASS da sola ispezione del codice.

## Correzioni e root cause materiali

Recupero AtomicFile `.bak`, conservazione della newline shell, retry NSD e nuova lettura dopo cambio endpoint sono stati corretti e rivisti indipendentemente. Il binding GUID/seriale e la proprietà protetta sono descritti con esperimento discriminante nel [dossier trasporto](../../research/decisions/TRANSPORT.md). Un AVD riutilizzato dopo il cambio motore aveva GUID del vecchio seriale: l’app lo ha respinto correttamente. PAKE/TLS è poi passata su un AVD nuovo e coerente.

**Emulatore.** `EVIDENCE_FOR`:37.1.11 e37.2.12 terminavano con SIGSEGV/SwiftShader;37.2.12 con `-gpu host -feature -Vulkan` completa boot e test. `EVIDENCE_AGAINST`: nessun debug nativo completo; il messaggio «cannot unmap» compare anche nel controllo positivo. `ALTERNATIVE_EXPLANATIONS`: renderer software, interazione host/driver o AVD. `CONFIDENCE`: alta sul confronto empirico, media sulla causa interna. `MINIMAL_DISCRIMINATING_EXPERIMENT`: stesso AVD/motore con GPU host, boot riuscito. Nessun driver o impostazione permanente host modificato.

**Acquisizioni.** `EVIDENCE_FOR`: idle Compose seguito da screenshot restituiva frame bianchi o precedenti; LocalDensity non cambiava il font dei dialoghi. `EVIDENCE_AGAINST`: controlli semantici passavano. `ALTERNATIVE_EXPLANATIONS`: compositor asincrono e contesto separato della finestra. `CONFIDENCE`: alta. `MINIMAL_DISCRIMINATING_EXPERIMENT`: attesa di frame reali, rifiuto di immagini uniformi e font_scale OS2.0; nuove catture confermano rendering e ingrandimento. Le immagini errate sono state sostituite.

**Packaging.** `EVIDENCE_FOR`: due APK con tutte le entry ZIP e firma v2 identiche differivano solo nel blocco `0x504b4453`. [Android documenta](https://developer.android.com/build/dependencies#dependency-info-play-console) i metadati dipendenze cifrati per Play. `EVIDENCE_AGAINST`: nessuna differenza nel codice o nelle risorse. `ALTERNATIVE_EXPLANATIONS`: nondeterminismo del compilatore o della firma, escluso dal confronto dei blocchi. `CONFIDENCE`: alta. `MINIMAL_DISCRIMINATING_EXPERIMENT`: disabilitare `dependenciesInfo` nell’APK standalone e ricompilare da checkout indipendente: confronto binario identico.

**Permessi impliciti.** `EVIDENCE_FOR`: il manifest merger attribuiva tre permessi telefonici/storage al target legacy del manifest SPAKE2. `EVIDENCE_AGAINST`: nessuna API dell’app li usa, né viene richiesto il consenso runtime. `ALTERNATIVE_EXPLANATIONS`: dichiarazioni dell’app, escluse dall’origine registrata nel merger. `CONFIDENCE`: alta. `MINIMAL_DISCRIMINATING_EXPERIMENT`: marker `tools:node="remove"`, nuova build e ispezione AAPT dell’APK finale: permessi assenti.

## Tracciamento verso il collaudo fisico

| Requisiti | Evidenza locale | Prova reale ancora necessaria |
| --- | --- | --- |
| F01–F04 | Binding PAKE/seriale, parser, endpoint validati, NSD e review | D02, D03, D12: Motorola/Chromecast, discovery e porta cambiata |
| F05–F09 | Test dominio, framing e scenari errore/parziale | D05–D11: scritture, riavvio, traffico e recupero OFF |
| F10 | Pending durevole, AtomicFile e riconciliazione senza replay | D03, D05: riapertura durante uso reale |
| F11–F14 | Release installata, trasporto reale, guida offline, manifest | D01, D11, D12: uso locale sul telefono designato |
| U01–U07 | Test conferma/cancel/busy, onboarding e recupero renderizzati | D01–D05, D12 |
| U08–U10 | Temi, contrasto, font200 OS, italiano e distinzione tunnel | D13 e osservazioni funzionali D06–D11 |

Nessuna misura locale certifica blocco universale del traffico o tunnel attivo. Android8/API26 e tutte le ABI sono compatibilità dichiarata dai componenti, non esecuzioni già collaudate. Il limite di autenticazione server TLS upstream contro MITM è documentato: uso su LAN fidata. G3/G4 restano aperti nella [ricevuta fisica](DEVICE_ACCEPTANCE.md).
