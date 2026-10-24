# P0 — baseline di implementazione

Artefatto principale della fase P0. Aggiornare questo documento con le decisioni dimostrate e i riferimenti alle prove; il gate e la transizione sono in `ROADMAP.md`.

## OBSERVED

- Il repository è stato creato dall'utente come `sorguido/chromecast-killswitch-app`, privato, branch predefinito `main`; prima del bootstrap conteneva solo un README vuoto.
- L'utente richiede un'app completa e graficamente curata. I test sintetici sono consentiti internamente; app dimostrative e APK parziali non sono deliverable.
- Il target Chromecast e le prove precedenti sono registrati in `evidence/current/BOOTSTRAP.md`; il modello e la versione Android del telefono non sono stati comunicati.
- C1/C2 documentano una strada client ADB wireless già esistente. L'integrazione nel prodotto deve ancora essere realizzata e verificata.

## INFERRED

Il percorso minimo plausibile è un solo modulo Android con UI nativa, gestione dello stato e un client ADB incorporato. Il telefono agisce come client: l'operazione autorizzata avviene sul target tramite ADB. La mancanza del modello del telefono non impedisce di sviluppare l'app o qualificarla sugli emulatori; limita ciò che si può dichiarare collaudato fisicamente.

## EVIDENCE_FOR / EVIDENCE_AGAINST

- A favore: documentazione AOSP del protocollo, API NSD Android, API upstream per pairing e connessione a un host remoto; comandi già usati dall'utente sulla Chromecast.
- Limiti: nessun APK esiste ancora; compatibilità TLS sul telefono, comportamento NSD e riconnessione su questa combinazione devono essere provati; la disattivazione completa dei tre parametri non ha ancora un PASS live riferito nella sessione.

## DECISION — percorso primario da concretizzare

- Kotlin, Jetpack Compose e Material 3 come scelta iniziale per un'interfaccia nativa semplice e curata; evitare framework o moduli aggiuntivi senza necessità.
- NSD del sistema per discovery, client ADB con pairing/TLS e storage privato per l'identità. `libadb-android` con il provider TLS consigliato upstream è la prima candidata, non una dipendenza già qualificata.
- Un solo APK standalone installabile, con tutte le funzionalità reali. Eventuali fixture, fake e simulatori restano nei test o nelle preview e non nei flussi release.
- `minSdk 26` come ipotesi iniziale di compatibilità. Se il telefono viene identificato, confrontare tale scelta con il dispositivo; non dichiarare compatibilità non verificata. Scegliere compile/target SDK e toolchain stabili consultando le fonti ufficiali e fissandone le versioni, senza usare versioni dinamiche.
- Nome di lavoro "Chromecast Kill Switch"; application ID proposto `it.sorguido.chromecastkillswitch`. Consolidare prima della prima firma destinata all'uso continuativo.

Queste sono scelte di implementazione modificabili con motivazione entro il prodotto concordato. Il fallback è regolato dalla roadmap; non svilupparlo ora.

## OPEN_ITEMS

1. Verificare toolchain disponibile nell'ambiente Codex; usare wrapper e dipendenze isolate al progetto. Registrare versioni effettive e comandi riproducibili.
2. Scegliere una revisione precisa della libreria e verificare dipendenze transitive, licenze, ABI native e compatibilità delle dimensioni pagina richieste dai dispositivi supportati. Evitare API nascoste quando il percorso remoto documentato non le richiede.
3. Definire gestione del ciclo di vita, timeout, framing shell e identità del peer usando API reali. Nessuna crittografia inventata per risolvere un problema di integrazione.
4. Specificare i permessi di rete richiesti dall'OS/target SDK effettivamente scelti, con percorso UI per consenso negato e revocato.
5. Predisporre build release, chiave di firma persistente privata e modalità di backup controllata dall'utente. Mai committare chiave/password o qualificare una debug build come consegna finale.
6. Scegliere e mantenere un linguaggio visivo coerente secondo `PRODUCT_SPEC.md`; verificare schermate reali in emulatori, non solo preview statiche.
7. Individuare il telefono del collaudo quando disponibile, senza interrompere le attività che non dipendono da quel dato.

## GATE

`G0`: NOT_STARTED. Nessuna versione del client o toolchain è stata qualificata nel bootstrap. Dopo aver documentato le evidenze richieste dalla roadmap, aggiornare stato e indice e passare autonomamente a P1.
