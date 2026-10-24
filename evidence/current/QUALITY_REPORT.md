# P2 — qualifica locale

## Stato

`IN_PROGRESS`, 24 ottobre 2026. Le prove riportate sono locali; nessun caso G3 sulla Chromecast e sul Motorola è qui dichiarato superato. La versione candidata è `1.0.0` (1).

## PROVEN — build e test eseguiti

Toolchain fissata nella [baseline](../../docs/phase-specific/P0_BUILD_BASELINE.md); cache isolata in `.local/gradle`, wrapper Gradle con checksum e dipendenze con lock/SHA-256. Eseguiti build release firmata, lint release e test unitari. Nell’integrazione con binding seriale sono passati 23 test unitari: 18 del dominio e 5 del framing/endpoint/identità. I test sintetici coprono target estraneo, provider e whitelist estranei, output incompleto, errori di scrittura/readback, ordine OFF completo, riavvio non ripetuto, pending e nuovo boot.

Sono passati 11 test UI sull’emulatore Android15 x86_64: conferma ripetuta, annullamento senza callback operativo, blocco durante operazione, distinzione OFF/Auto-connect, guida offline, ricerca ripetibile e acquisizioni. Il test font200 è stato ripetuto con la configurazione OS reale: istruzioni e pulsanti risultano raggiungibili scorrendo. Il valore iniziale1.0 è stato ripristinato. Le immagini precedenti con override locale insufficiente sono state sostituite.

L’APK release è firmato con certificato SHA-256 `2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`; `apksigner verify` e `zipalign -c -P 16 4` sono riusciti. L’identità definitiva di APK e sorgente sarà registrata dopo l’ultima verifica della medesima build.

## OBSERVED — QA visiva

Le [schermate](screenshots/evidence/) sono catture dell’emulatore, con fixture esplicitamente marcate `synthetic`. Home chiara/scura, ON, parziale, lettura vecchia dopo errore e recupero risultano leggibili; le azioni e la guida scorrono. Il contrasto calcolato sui colori espliciti è almeno6,42:1 per le coppie di testo normali; bersagli48dp, principali56dp. TalkBack fisico resta D13.

Le prime catture home erano bianche perché anticipavano il compositor nonostante l’idle di Compose. La procedura ora attende due frame reali e rifiuta catture uniformi; le immagini bianche sono state sostituite, senza usarle come PASS.

## Difetti materiali individuati e corretti

- Recupero AtomicFile anche con solo `.bak` dopo un crash.
- Conservazione della newline shell per distinguere whitelist vuota da output mancante.
- Ricerca riavviabile, precedenza al GUID associato e lettura al ritorno dopo risoluzione di un nuovo endpoint.
- Proprietà `persist.adb.wifi.guid` non leggibile dalla shell: uso del seriale contenuto nel GUID autenticato dalla PAKE secondo AOSP14. [Root cause completa](../../research/decisions/TRANSPORT.md).

## Root cause — emulatore locale

`EVIDENCE_FOR`: i motori37.1.11 e37.2.12 terminavano con SIGSEGV usando SwiftShader, prima del boot. Con37.2.12 e `-gpu host -feature -Vulkan` l’avvio è completato e i test UI sono eseguibili.

`EVIDENCE_AGAINST`: non è stato eseguito un debug nativo completo; il messaggio «cannot unmap» compare anche nell’avvio riuscito e non prova la causa.

`ALTERNATIVE_EXPLANATIONS`: difetto del renderer software, interazione driver/host o configurazione AVD. La memoria ridotta da sola non ha risolto.

`CONFIDENCE`: alta sul confronto empirico dei renderer; media sulla causa interna SwiftShader.

`MINIMAL_DISCRIMINATING_EXPERIMENT`: stesso AVD/motore, passaggio a GPU host: boot completato. Nessun driver o impostazione permanente host modificati.

Il cambio motore ha inoltre cambiato il seriale virtuale mentre il GUID persistente derivava dal vecchio seriale. L’app ha bloccato correttamente il mismatch. La prova pairing è passata su un nuovo AVD coerente: PAKE/TLS, seriale, shell e riconnessione in0,74s. Il controllo non è stato indebolito per adattarlo alla fixture incoerente.

## Limiti e prossima prova

Passati inoltre due test strumentali su recupero AtomicFile/pending e chiave persistente/Conscrypt. Il riepilogo sanitizzato è in [local-checks.json](local-checks.json). Restano installazione e QA della release definitiva, riproduzione binaria e poi tutti i casi della [procedura fisica](../../docs/phase-specific/DEVICE_ACCEPTANCE.md). Nessun successo del tunnel o blocco traffico è dedotto dalle chiavi di configurazione.
