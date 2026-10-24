# P1 — implementazione integrata

## OBSERVED — 24 ottobre 2026

Baseline Git `4a5d69d`, worktree inizialmente pulito. Sono implementati dominio, client ADB reale, NSD, storage privato atomico e UI Material 3 in italiano. Il controllo operativo usa una singola attività in primo piano; ViewModel conserva il worker durante rotazione e l’operazione pendente è scritta su disco prima delle mutazioni. Dopo riapertura vengono eseguite solo letture.

Il target è vincolato all’identità letta sulla connessione associata, verificata prima delle scritture. Le impostazioni estranee impediscono modifiche. Il readback precede il servizio ADB `reboot:`; il completamento richiede un nuovo boot UUID e stato desiderato. Un timeout o una perdita del collegamento non equivalgono a successo.

## Architettura

- `core/OperationEngine.kt`: contratto, preflight, sequenze, riconciliazione e test sintetici; [dettagli e review](CORE.md).
- `transport/`: pairing SPAKE2, TLS, stream ADB, framing shell e discovery NSD; fork minimale del candidato originale in `vendor/libadb`; [provenienza e limiti](TRANSPORT.md).
- `PrivateStore.kt`: associazione, endpoint e pending nei file privati esclusi dai backup.
- `MainViewModel.kt`: connessione, mutua esclusione, retry limitati delle sole letture, stato attuale invalidato quando la lettura fallisce.
- `ui/KillSwitchApp.kt`: schermata principale, popup obbligatori, associazione, endpoint manuale e guida offline. Test UI separati dall’APK release.

## Review indipendente e correzioni

Revisione del dominio e controller da agente distinto dall’implementatore: corretto recupero AtomicFile dopo crash, mantenuto newline del framing per distinguere whitelist vuota da risposta assente, priorità all’ultimo endpoint noto, ricerca nuovamente avviabile anche durante monitoraggio. Le fasi interrotte prima della richiesta di riavvio restano da riconciliare esplicitamente.

## Stato delle prove

Build release firmata, lint senza errori e 22 test unitari passati nella prima integrazione. I test UI sono eseguiti sull’emulatore; la qualifica finale resta P2. Nessuna operazione eseguita sulla Chromecast fisica. G0 e G1 consolidati: review indipendente completata; pairing PAKE/TLS, seriale, shell e riconnessione passati contro il vero adbd di un AVD coerente. Nessun falso target Chromecast introdotto. L’emulatore serve alle prove locali; non sostituisce G3.
