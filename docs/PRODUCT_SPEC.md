# Specifica di prodotto

Questo file possiede i requisiti funzionali e di interfaccia dell'app. Il [contratto dei comandi](COMMAND_CONTRACT.md) possiede comandi ADB, classificazione tecnica degli stati e verifiche delle operazioni; la [roadmap](../ROADMAP.md) possiede fasi e criteri di uscita. La [procedura sul dispositivo](phase-specific/DEVICE_ACCEPTANCE.md) descrive il collaudo finale dei requisiti qui identificati.

## Obiettivo e perimetro

Un'app Android completa, installata sul telefono dell’utente, comanda una sola Chromecast già scelta dall'utente tramite ADB wireless autorizzato. Permette di attivare o disattivare insieme la VPN always-on di Android e il relativo kill switch, applicando la modifica con un riavvio della Chromecast. L'interfaccia deve essere semplice, elegante e interamente in italiano.

| Voce | Destinazione del prodotto |
| --- | --- |
| Dispositivo controllato | Chromecast con Google TV HD, `boreal`, Android 14, utente Android `0` |
| Provider VPN | Proton VPN, pacchetto `ch.protonvpn.android`, già installato e configurato sulla Chromecast |
| Telefono | Motorola edge 40 neo (XT2307-1), Android 15, comunicato dall’utente il 24 ottobre 2026; ABI e compatibilità effettiva da collaudare |
| Compatibilità minima | Ipotesi e decisione possedute dalla [baseline P0](phase-specific/P0_BUILD_BASELINE.md), da convalidare sulle dipendenze e sui dispositivi; nessuna compatibilità già dimostrata |
| Toolchain | Versioni di SDK, JDK, Gradle e dipendenze da selezionare e fissare in P0 secondo la roadmap |
| Consegna | APK release completo, firmato, con tutte le funzioni descritte; nessun APK dimostrativo o provvisorio come deliverable |

L'app è un telecomando locale per questa configurazione: non diventa un client VPN e non sostituisce Proton. L'Auto-connect interno a Proton resta un'impostazione separata, gestita nell'app Proton sulla Chromecast.

## Requisiti funzionali

| ID | Requisito | Criterio verificabile |
| --- | --- | --- |
| F01 | Controllare una sola Chromecast associata e il profilo previsto. | Prima di una modifica, il target corrisponde all'associazione salvata, all'utente previsto e alla configurazione supportata. Un altro dispositivo scoperto non diventa automaticamente il target. |
| F02 | Associare il telefono come nuovo client ADB. | Il flusso usa il codice mostrato dalla Chromecast e conserva l'associazione per gli avvii successivi dell'app. Un codice errato non abilita comandi di modifica. Le credenziali del PC non sono necessarie. |
| F03 | Individuare automaticamente l'endpoint wireless corrente. | La scoperta distingue servizio di pairing e servizio di connessione; risolve nuovamente IP e porta quando cambiano e riconosce gli eventuali endpoint duplicati della stessa Chromecast. L'identità salvata non coincide con un IP o una porta fissi. |
| F04 | Consentire un collegamento manuale quando la scoperta non riesce. | Una sezione secondaria accetta l'endpoint corrente fornito dall'utente, segnala un indirizzo non valido e conserva gli stessi controlli sull'identità del target. Il collegamento manuale non aggira l'associazione. |
| F05 | Leggere lo stato reale della configurazione. | La schermata distingue configurazione ON, OFF, parziale e non verificabile secondo il contratto. Letture fallite, vecchi valori in cache o dati incompleti non producono uno stato ON/OFF attuale inventato. |
| F06 | Attivare insieme always-on Android e kill switch. | Dopo la conferma vengono impostate entrambe le funzioni per Proton, verificata la configurazione, richiesto il riavvio e verificato il risultato dopo l'avvio, secondo il contratto. Il semplice ritorno della scrittura non conclude l'operazione. |
| F07 | Disattivare completamente le due funzioni di sistema. | Dopo la conferma vengono rimosse tutte e tre le impostazioni indicate nel contratto, controllata la rimozione e applicato il riavvio. La sola disattivazione del lockdown, con Proton ancora selezionata come always-on, non soddisfa OFF. |
| F08 | Distinguere modifica salvata, riavvio e risultato verificato. | La UI descrive lo stato dell'operazione senza confonderlo con lo stato del tunnel VPN. La perdita del collegamento durante il riavvio è gestita; senza verifica successiva l'operazione resta non confermata. |
| F09 | Gestire gli errori senza dichiarare successi parziali. | Un errore di collegamento, autorizzazione, scrittura o rilettura arresta la sequenza prevista dal contratto. La UI indica cosa è stato verificato e permette una nuova lettura o un nuovo tentativo esplicito; nessun riavvio viene inviato dopo una verifica fallita. |
| F10 | Riprendere correttamente dopo uscita e riapertura dell'app. | L'app conserva l'associazione e l'eventuale operazione non conclusa. Al ritorno aggiorna lo stato; non ripete in silenzio una scrittura o un riavvio rimasti incerti. |
| F11 | Funzionare localmente, in primo piano e senza root sul telefono. | Le funzioni non richiedono account dell'app, server cloud, PC acceso, root o servizi permanenti sul telefono. L'app non richiede un proprio servizio VPN, accessibilità di sistema o un APK aggiuntivo sulla Chromecast per il percorso previsto. |
| F12 | Spiegare e gestire l'indisponibilità del Debug wireless. | Se ADB non è raggiungibile, l'app presenta le istruzioni sul televisore e le opzioni di riprova/collegamento manuale. Non dichiara di aver attivato il Debug wireless tramite un collegamento assente. Il ritorno della rete non avvia da solo un'operazione precedentemente fallita. |
| F13 | Offrire istruzioni italiane utilizzabili durante un problema di rete. | La guida integrata resta leggibile offline e copre associazione, rete locale, endpoint corrente, riavvio e riattivazione del Debug wireless dal TV. Include il richiamo al recupero USB già verificato e alla relativa procedura canonica. |
| F14 | Consegnare un'app completa con comportamento reale. | Nell'APK release tutti i percorsi richiesti sono implementati: pairing, scoperta, fallback manuale, lettura, ON, OFF, riavvio, riconnessione, gestione errori e guida. Nessun pulsante richiesto è un segnaposto; nessuno stato simulato o trasporto mock è attivo nella release. |

## Requisiti di interfaccia

| ID | Requisito | Criterio verificabile |
| --- | --- | --- |
| U01 | Una schermata principale essenziale. | Sono immediatamente riconoscibili Chromecast associata, raggiungibilità, configurazione rilevata e le due operazioni principali. Pairing, dettagli tecnici e aiuto stanno in sezioni secondarie; nessun terminale è necessario nell'uso ordinario. |
| U02 | Operazioni dal significato esplicito. | I controlli principali sono «Attiva e riavvia» e «Disattiva e riavvia», con indicazione che agiscono su entrambe le funzioni Android. Non si usa un interruttore che suggerisca un'applicazione istantanea. |
| U03 | Popup obbligatorio a ogni tap operativo. | Ogni tap abilitato su ON o OFF apre una finestra che espone precondizioni di rete/Debug wireless e riavvio. I pulsanti sono «Annulla» e «Conferma». Non esiste «Non mostrare più». Nessuna scrittura o richiesta di riavvio precede la conferma. |
| U04 | Annullamento effettivo e operazioni non sovrapposte. | «Annulla» chiude il popup senza modificare la Chromecast. Durante una sequenza già avviata i controlli di modifica non permettono doppi invii o un'operazione opposta concorrente. |
| U05 | Onboarding breve e guidato. | Il primo uso mostra come aprire l'associazione sul televisore, scegliere la Chromecast e inserire il codice. Distingue chiaramente l'associazione del telefono da quella già concessa al PC, senza chiedere il Debug wireless sul telefono per farlo agire da client. |
| U06 | Avanzamento e risultato comprensibili. | I messaggi separano salvataggio, riavvio, attesa e verifica. Uno stato non raggiungibile o non verificato è espresso a parole, non solo con un colore. Gli errori propongono un'azione pertinente e non un successo generico. |
| U07 | Guida di recupero nel punto in cui serve. | Il messaggio per ADB irraggiungibile spiega che l'app non può riaccendere da sola il Debug wireless e consente di aprire la guida, riprovare o usare l'endpoint manuale. Le credenziali di pairing non compaiono nei messaggi diagnostici. |
| U08 | Aspetto Material 3 sobrio, chiaro e scuro. | Entrambi i temi sono completi e seguono la preferenza di sistema. Spaziatura, tipografia e gerarchia rendono le due azioni leggibili senza schermate affollate, decorazioni invasive o controlli tecnici fuori contesto. |
| U09 | Accessibilità su un telefono reale. | I bersagli tattili misurano almeno 48 dp; testo al 200% non taglia istruzioni o azioni essenziali e può scorrere quando necessario. TalkBack annuncia stato e scopo dei controlli; il contrasto del testo normale è almeno 4,5:1. |
| U10 | Italiano coerente e limiti esposti con precisione. | Schermate, errori, onboarding e guida sono in italiano. «Configurazione ON» non viene presentato come prova di VPN connessa o di assenza di perdite. La guida spiega che Auto-connect di Proton è separato dalle due funzioni di sistema. |

### Contenuto minimo dei popup ON/OFF

Titolo coerente con l'azione: «Attivare e riavviare?» oppure «Disattivare e riavviare?».

Il testo deve comunicare, in forma breve:

> Il telefono deve poter raggiungere la Chromecast sulla rete locale e il Debug wireless deve essere attivo. La modifica richiede il riavvio della Chromecast e interrompe la riproduzione. Se il Debug wireless è spento, va riattivato dal televisore.

Nel popup OFF precisare che vengono disattivati always-on e kill switch **di Android**; l'Auto-connect di Proton resta separato. «Conferma» avvia solo l'operazione selezionata dopo una nuova verifica delle precondizioni. Un popup di recupero informa e guida: non concede accessi e non rende raggiungibile un dispositivo offline.

## Riscontri da acquisire sul prodotto completo

La compatibilità del telefono, il client ADB/TLS scelto, la scoperta mDNS dal telefono e la riconnessione dopo il riavvio devono essere dimostrati sul prodotto integrato. Le prove storiche dei comandi da PC non costituiscono un collaudo dell'APK. Il modello del telefono e le scelte di implementazione restano quelli registrati nelle evidenze effettive di progetto.

Il riferimento operativo è il [contratto dei comandi](COMMAND_CONTRACT.md), che conserva i dettagli di implementazione e le verifiche tecniche.
