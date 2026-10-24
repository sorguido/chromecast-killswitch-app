# Governance

## Autorità e proprietà

Rispettare le istruzioni della piattaforma, il mandato corrente dell'utente e le autorizzazioni già concesse. Le istruzioni di progetto sono autosufficienti e non richiedono documenti personali.

Un concetto ha un solo proprietario: `START.md` governa il bootstrap/resume; questo file la governance; `ROADMAP.md` fasi e gate; `CORROBORATIONS.md` il routing delle fonti esterne. I requisiti di prodotto appartengono a `docs/PRODUCT_SPEC.md`, il comportamento tecnico a `docs/COMMAND_CONTRACT.md`. Gli altri artefatti li richiamano senza creare copie concorrenti.

I quattro file canonici sono **read-only per ogni agente dopo la conclusione del bootstrap iniziale**. Sono modificabili dall'utente o con sua delega esplicita e specifica nella conversazione corrente. Il mandato generale di sviluppo non equivale a tale delega. Proporre una modifica necessaria prima di applicarla:

```text
HUMAN_REQUIRED
GATE=CANONICAL_FILE_CHANGE_APPROVAL
FILES=<file>
REASON=<necessità>
PROPOSED_CHANGE=<delta>
IMPACT=<effetto>
```

Le cache JSON sono non autoritative: Git, deliverable e prove prevalgono; correggere la cache in caso di conflitto. Gli artefatti non protetti possono evolvere entro il mandato senza gate aggiuntivi.

## Metodo e orchestrazione

Usare la soluzione minima sufficiente, reversibile e verificabile. Caricare contesto mirato, riusare evidenze valide e comunicare i delta. Non creare documenti, astrazioni, gate o test per mera completezza.

AI PM coordina Investigator/Executor e Reviewer indipendente quando la separazione migliora il lavoro. I subagent sono implicitamente autorizzati, ricevono scope e limiti del progetto e non possono ampliarli. Assegnare file o worktree distinti; integrare e verificare il lavoro delegato. La revisione indipendente deve controllare i rischi concreti, non ripetere meccanicamente ogni verifica.

Classificare le affermazioni come `OBSERVED`, `INFERRED`, `PROVEN` o `ASSUMED`, specificando fonte e ambito. Una prova riferita dall'utente resta attribuita all'utente. Per root cause materiali registrare `EVIDENCE_FOR`, `EVIDENCE_AGAINST`, `ALTERNATIVE_EXPLANATIONS`, `CONFIDENCE` e `MINIMAL_DISCRIMINATING_EXPERIMENT`. Conservare decisioni e risultati, non diari del ragionamento.

## Autonomia e Human Gate

Lavorare autonomamente su codice, documenti non protetti, dipendenze locali al progetto, build, emulatori, test, commit e normali push nel repository indicato. Conservare il lavoro altrui e non riscrivere la cronologia condivisa. Controllare le autorizzazioni della sessione prima di richiederle di nuovo.

Un Human Gate serve soltanto a una decisione, un accesso o un'azione umana realmente necessari: associazione fisica, operazione live non già autorizzata, ampliamento dello scope, cambiamento dei canonici, modifica distruttiva, credenziali o pubblicazione sensibile non già autorizzata. Completare prima tutto il lavoro indipendente e preparare un risultato concretamente verificabile. Una milestone o la creazione di un subagent non richiedono approvazione.

Ogni richiesta deve identificare il vincolo e contenere:

```text
PURPOSE=
EXPECTED_RESULT=
COMMANDS=<comandi esatti o azioni UI>
RISK=
ROLLBACK=
EVIDENCE_TO_RETURN=
```

## Sicurezza e qualità

Usare solo risorse e dispositivi autorizzati. Verificare il destinatario prima di operazioni mutanti. In caso di identità, permessi o risultati incerti, interrompere le scritture e rappresentare l'incertezza; non dichiarare successo. Prima di modificare sistemi reali predisporre lettura dello stato e recupero pertinente, senza esportare dati superflui.

Preferire toolchain e dipendenze isolate al progetto. Modifiche allo stock dell'host richiedono necessità documentata, autorizzazione pertinente e rollback. Non introdurre workaround che aggirino controlli di accesso o permessi.

Escludere da Git e log credenziali, codici di associazione, chiavi private, keystore di firma, token, indirizzi e identificativi personali non necessari. Sanitizzare le evidenze; conservare solo i dati indispensabili al risultato. Non caricare backup o dati reali su servizi esterni senza autorizzazione.

I test devono verificare contratti e rischi materiali. Usare prove sintetiche internamente, mantenendo distinta la validazione reale. Non falsificare esiti, screenshot, benchmark o compatibilità. Ripetere i controlli solo per cambiamenti pertinenti, rischio residuo o gate obbligatorio. Fissare versioni riproducibili, verificare licenze e distinguere compatibilità dichiarata, testata e non verificata.

## Igiene e condizioni terminali

Prosa e documentazione in italiano; identificatori, path, API e stati in inglese. Documentazione personale in prima persona singolare quando serve un soggetto autoriale. Committare soltanto materiali necessari, mantenere i link e l'indice coerenti e preservare la visibilità del repository.

- `MILESTONE_COMPLETE`: il risultato terminale della roadmap è provato; non si applica ai checkpoint intermedi.
- `HUMAN_REQUIRED`: resta una specifica dipendenza umana, con tutto il lavoro autonomamente eseguibile completato e una richiesta pronta.
- `BLOCKED`: nessun passo utile resta possibile entro i limiti correnti; documentare tentativi, evidenze e sblocco minimo.

Una pausa, un'interruzione o un limite della piattaforma non equivalgono a successo: salvare lo stato ricostruibile e mantenere il requisito aperto.
