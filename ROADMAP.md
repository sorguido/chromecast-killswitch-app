# State machine

L'obiettivo terminale è un'app Android completa, installabile e verificata per il prodotto definito in `docs/PRODUCT_SPEC.md`. Le fasi sono checkpoint interni dello stesso Goal; il loro superamento conduce alla fase successiva, senza consegne di app dimostrative o richieste di continuazione.

## Percorso

`PRIMARY`: app Android nativa, client ADB wireless autorizzato, discovery locale e comandi documentati. La base candidata e le decisioni ancora da validare sono in `docs/phase-specific/P0_BUILD_BASELINE.md`.

Se emerge un problema, adattare il percorso primario e svolgere l'esperimento discriminante pertinente. Soltanto quando l'impedimento è dimostrato e le correzioni ragionevoli sono esaurite, valutare `FALLBACK`: un'altra implementazione client ADB mantenendo autenticazione, comportamento e prodotto concordati. Non sviluppare i due percorsi in parallelo. Un cambiamento di prodotto o autorizzazioni segue `AGENTS.md`.

| PHASE | PURPOSE | PRIMARY_ARTIFACT | EXIT_GATE | NEXT |
|---|---|---|---|---|
| P0 | Consolidare baseline, toolchain e scelta del client nel progetto finale | `docs/phase-specific/P0_BUILD_BASELINE.md` | `G0`: toolchain e dipendenze fissate; compatibilità e licenze valutate; percorso implementabile, rischi e prove necessarie espliciti | P1 |
| P1 | Realizzare tutta l'app integrata | `research/decisions/P1_IMPLEMENTATION.md` | `G1`: requisiti funzionali/UX implementati con trasporto reale; nessun flusso principale fittizio o lasciato da completare; review del codice svolta | P2 |
| P2 | Qualificare il prodotto e preparare il medesimo APK da collaudare | `evidence/current/QUALITY_REPORT.md` | `G2`: build riproducibile, test e lint pertinenti superati; QA visiva e accessibilità documentate; APK release firmato, checksum e procedura di installazione pronti | P3 |
| P3 | Verificare l'uso reale sui dispositivi designati | `evidence/current/DEVICE_ACCEPTANCE.md` | `G3`: procedura `docs/phase-specific/DEVICE_ACCEPTANCE.md` eseguita con esiti effettivi e attribuiti; problemi corretti e ricontrollati; nessuna prova obbligatoria sostituita da mock | P4 oppure P1/P2 per correzioni |
| P4 | Consegnare il prodotto verificato | `evidence/current/DELIVERY.md` | `G4`: tutti i requisiti obbligatori tracciati a evidenze; APK finale e istruzioni fruibili; sorgente/commit, versione, firma e checksum coerenti con il collaudo; limiti residui dichiarati e non bloccanti | `MILESTONE_COMPLETE` |

I percorsi degli artefatti futuri sono intenzionali: crearli quando la relativa fase produce risultati, non come file vuoti. L'indice distingue gli artefatti disponibili da quelli previsti.

## Applicazione dei gate

Solo `G4` soddisfa il Goal. Un APK che compila, un insieme di test sintetici, una UI con dati simulati o un rapporto di fattibilità non soddisfano `G3`/`G4`.

Se manca l'intervento umano indispensabile a `G3`, preparare prima app completa, APK, QA eseguibile e procedura minima; registrare `HUMAN_REQUIRED` secondo `AGENTS.md`, mantenendo `G3` aperto. Proseguire ogni attività indipendente. Non inventare accesso al telefono o risultati della Chromecast.

Dopo ogni modifica che incide sul prodotto, tornare al controllo pertinente e preservare la corrispondenza tra sorgente, APK e prove. Non qualificare una build differente da quella consegnata; una modifica richiede rivalutazione motivata della copertura.
