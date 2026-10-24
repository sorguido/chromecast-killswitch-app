# Dominio delle operazioni

## OBSERVED — implementazione

`OperationEngine.kt` applica il [contratto operativo](../../docs/COMMAND_CONTRACT.md) su un'interfaccia sincrona che richiede output shell completo con exit code zero. Il trasporto reale controlla framing, timeout ed esito; la UI concede l'esecuzione solo dopo conferma esplicita.

L'identità persistita include seriale, modello, device, SDK e release; nessun endpoint diventa identità. Tutti i preflight rileggono utente 0 e presenza di Proton. Le scritture sono comandi fissi. Un provider diverso, una whitelist estranea, una risposta inattesa o un'identità diversa impediscono qualsiasi mutazione.

Lo stato pendente viene scritto durevolmente prima della prima mutazione e prima della richiesta `reboot:`. La cancellazione OFF richiede esattamente tre `null` prima del riavvio. Una scrittura fallita o una rilettura discordante arresta la sequenza senza riavvio e senza rollback automatico che possa ridurre una protezione preesistente.

`reconcile` esegue esclusivamente letture. Un ciclo viene verificato solo dopo una richiesta di riavvio registrata, con UUID di boot diverso, avvio completato e configurazione desiderata riletta. Un'interruzione nelle fasi `WRITING` o `SAVED` resta esplicita: la UI propone un nuovo tentativo confermato. L'app può mostrare la configurazione corrente senza qualificare come completato il vecchio ciclo.

Un nuovo tentativo esplicito può sostituire un'operazione pendente, ma solo dopo nuovo preflight completo. Un blocco atomico impedisce richieste sovrapposte sullo stesso motore; il controller mantiene inoltre un unico worker per trasporto e UI.

## INFERRED — limiti deliberati

Senza UUID di boot leggibile la lettura della configurazione resta disponibile, ma non vengono avviate nuove modifiche: mancherebbe una prova discriminante del riavvio. Il collaudo reale deve confermare la leggibilità di `/proc/sys/kernel/random/boot_id` sul firmware designato.

Il seriale letto tramite connessione ADB non è da solo una prova crittografica dell'identità del server. La qualifica del trasporto e del pairing è separata; il dominio impedisce il cambio accidentale di target. Nessun risultato sulle chiavi Android certifica lo stato del tunnel o il blocco del traffico.

## Prove sintetiche

`OperationEngineTest.kt` verifica classificazione ON/OFF/parziale/conflitto, identità errata, profilo e target incompatibili, Proton assente, whitelist estranea, risposte vuote/multilinea, errore durante le scritture, write ignorata, readback fallito, persistenza indisponibile, riavvio incerto, ripresa dopo terminazione, nessun replay, differenza di boot, avvio incompleto, risultato discordante, concorrenza e retry esplicito. Le fixture esistono esclusivamente in `src/test`.

Esito di esecuzione da integrare nel rapporto qualità dopo la build del progetto; questi test non dimostrano compatibilità del telefono o funzionamento della Chromecast.

## Revisione indipendente dell'integrazione

- `OBSERVED`: la UI invoca la mutazione soltanto dal pulsante «Conferma», mentre «Annulla» elimina la richiesta locale; il controller serializza le operazioni con un unico job. Il preflight viene ripetuto dopo la conferma.
- `OBSERVED`: la rilettura dopo il riavvio non chiama `execute` e non ripete `reboot`. Lo stato non verificato resta separato dalla configurazione riletta.
- `OBSERVED`: sono state segnalate ai proprietari del trasporto/controller tre correzioni: preservare la newline di output nel framing shell (whitelist vuota valida), recuperare i backup AtomicFile anche con file base assente e consentire un vero riavvio della ricerca dopo errori NSD. La conferma delle correzioni e le prove d'integrazione appartengono al rapporto qualità.

Per il rischio di risultato falsamente completato dopo interruzione: `EVIDENCE_FOR` = la fase `WRITING` può precedere un riavvio esterno; `EVIDENCE_AGAINST` = la configurazione desiderata e il nuovo boot sarebbero comunque osservati; `ALTERNATIVE_EXPLANATIONS` = riavvio manuale o completamento da altra sessione; `CONFIDENCE` = alta sul rischio di attribuzione del ciclo; `MINIMAL_DISCRIMINATING_EXPERIMENT` = fixture pendente `WRITING`/`SAVED`, boot cambiato e configurazione corretta. La decisione implementata mantiene `REQUIRES_EXPLICIT_RETRY`, senza attribuire all'app un ciclo non registrato.

## Revisione finale dell'integrazione — 24 ottobre 2026

`PROVEN` nell'ambito dei test sintetici: ho letto il risultato JUnit di `OperationEngineTest` prodotto dalla build del progetto: 18 test, 0 failure, 0 errori e 0 skip. Questo risultato riguarda il dominio e non il pairing o la Chromecast fisica.

`OBSERVED` nel codice aggiornato: il parser shell conserva la newline, `PrivateStore` apre AtomicFile prima di concludere che il dato sia assente, gli errori di avvio NSD fermano i listener e il controller consente una nuova ricerca. Il vecchio endpoint viene provato prima dei candidati generici, evitando che servizi estranei lo escludano dal limite di tentativi.

`OBSERVED` nel nuovo binding del trasporto: il GUID ricevuto nello scambio PAKE riuscito viene persistito; il formato AOSP `adb-<seriale>-<sei caratteri>` viene validato rigorosamente e `ro.serialno` deve corrispondere prima che la sessione sia restituita al controller. Non viene più richiesta la leggibilità della proprietà `persist.adb.wifi.guid`. La successiva verifica del dominio conserva tutti gli attributi del target. Il limite di autenticazione TLS upstream, già documentato nel dossier trasporto, rimane esplicito: il controllo protegge la selezione del dispositivo e non dimostra resistenza a un server ostile.

Ho segnalato un ulteriore caso di ripresa: all'apertura la discovery riparte vuota e la prima lettura può fotografare soltanto l'endpoint precedente prima della risoluzione NSD. Occorre un tentativo di sola lettura limitato quando arriva il servizio associato, oppure una breve attesa della discovery; nessuna scrittura o richiesta di riavvio deve essere ripetuta. La conferma dell'integrazione di questa correzione è riportata nel seguito della review.

Il dominio non presenta ulteriori difetti bloccanti individuati dalla review: preflight fresco, conferma delegata alla UI, readback prima del reboot, persistenza prima delle mutazioni e riconciliazione senza replay restano coerenti. La chiusura G1 può basarsi sull'implementazione integrata e sulla review dopo la correzione della ripresa NSD; G2/G3 mantengono rispettivamente qualifica dell'APK e prove fisiche distinte.

`OBSERVED` dopo la correzione della ripresa: `refresh` conserva i candidati già tentati e, dopo un primo errore, attende al massimo 12 secondi un nuovo endpoint pubblicizzato con il GUID associato; esegue quindi un solo nuovo tentativo di lettura. `candidates` dà precedenza ai servizi corrispondenti al GUID, poi all'ultimo endpoint e infine agli altri servizi. La corrispondenza mDNS è soltanto un criterio di ordine: ogni connessione resta vincolata al controllo del seriale e al preflight. Il timeout non genera mutazioni e la cancellazione non viene reinterpretata come un nuovo tentativo.

`OBSERVED`: il modulo trasporto rifiuta inoltre ADB non TLS prima di accettare `CNXN`; la via manuale non abilita implicitamente il vecchio ADB TCP non cifrato.

Conclusione della review G1: nessun difetto bloccante residuo individuato nel dominio, nel controller o nel collegamento dell'identità dopo le correzioni esaminate. Pairing reale aggiornato, build/lint, QA e qualifica fisica restano evidenze da attribuire ai rispettivi risultati; questa conclusione riguarda implementazione e review e non dichiara G2/G3/G4 conclusi.
