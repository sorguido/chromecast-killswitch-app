# Contratto operativo ADB e VPN

Questo documento possiede comandi, interpretazione dei risultati e sequenza operativa. I requisiti di prodotto e le schermate appartengono a [PRODUCT_SPEC.md](PRODUCT_SPEC.md); i gate a [ROADMAP.md](../ROADMAP.md). Le basi esterne sono nel dossier [C3](../research/corroborations/C3.md).

## Target e preflight

Prima versione: Chromecast HD, product/device `boreal`, Android 14 (API 34), utente Android `0`, Proton `ch.protonvpn.android`. Modello e versione sono una baseline di supporto, non l'identità univoca del dispositivo.

Il telefono possiede le proprie credenziali ADB. La prima associazione avviene tramite codice mostrato dalla Chromecast; il PC già autorizzato non trasferisce la propria identità. Conservare in storage privato le credenziali e il riferimento al dispositivo associato, con esclusione dai backup esportabili non protetti. Reinstallazione, cancellazione dei dati o revoca possono richiedere un nuovo pairing.

Usare `_adb-tls-pairing._tcp` per l'associazione e `_adb-tls-connect._tcp` per la connessione. IP, porta e nomi mDNS sono endpoint da risolvere: non valgono da soli come autenticazione. Non selezionare automaticamente il primo dispositivo trovato. Dopo la connessione autenticata controllare l'identità salvata e gli attributi disponibili, poi eseguire le letture di preflight:

```sh
getprop ro.product.model
getprop ro.product.device
getprop ro.build.version.release
getprop ro.build.version.sdk
am get-current-user
pm path ch.protonvpn.android
settings --user 0 get secure always_on_vpn_app
settings --user 0 get secure always_on_vpn_lockdown
settings --user 0 get secure always_on_vpn_lockdown_whitelist
```

Sono comandi della shell remota: il client Android deve aprire il canale ADB, non cercare il binario del PC o usare il suo percorso SDK. Impostare gli eventuali parametri della libreria relativi alla versione del daemon secondo il target, non automaticamente secondo l'Android del telefono.

Identità non confermata, target fuori supporto, utente diverso, Proton assente, sempre-attiva assegnata a un altro provider o eccezioni inattese devono produrre un esito esplicito prima di qualsiasi scrittura. Per la baseline concordata la whitelist è assente: non importare, sovrascrivere o cancellare configurazioni estranee senza una decisione pertinente.

## Lettura e stato

Conservare distinti stato del collegamento, valori persistenti e avanzamento dell'operazione. Accettare `null` solo da una lettura riuscita; una stringa vuota, un timeout, un errore o una risposta troncata non significano impostazione assente.

| Condizione letta per l'utente 0 | Interpretazione |
|---|---|
| App `ch.protonvpn.android`, lockdown `1`, nessuna eccezione | Configurazione di sistema ON |
| App `null`, lockdown `null` oppure `0`, whitelist assente o vuota | Configurazione di sistema OFF |
| App Proton con lockdown `0`/`null`, oppure combinazione incompleta | Configurazione parziale |
| Altro provider, eccezioni inattese, valori non supportati | Conflitto da spiegare; niente scritture automatiche |
| Lettura non riuscita, target non autenticato o irraggiungibile | Stato da verificare |

Le impostazioni non sono una misura del traffico: non derivarne un indicatore "nessuna perdita" o "tunnel connesso". La UI può mostrare configurazione letta e configurazione verificata dopo il riavvio, con l'istante dell'ultima lettura. La prova funzionale è descritta nella procedura di accettazione.

L'Auto-connect di Proton è indipendente. OFF rimuove gli automatismi Android concordati; non disconnette necessariamente il tunnel e non modifica la preferenza interna di Proton.

## Attivazione

Sul solo target confermato, con una singola operazione alla volta e dopo la conferma UI prevista da `PRODUCT_SPEC.md`:

```sh
settings --user 0 put secure always_on_vpn_app ch.protonvpn.android
settings --user 0 put secure always_on_vpn_lockdown 1
```

Rileggere le tre chiavi. Le prime due devono essere esattamente `ch.protonvpn.android` e `1`; la whitelist deve restare senza eccezioni. Un exit code favorevole senza readback non basta. Un errore o una scrittura parziale impedisce il riavvio automatico e richiede stato esplicito e azione di ripristino coerente.

## Disattivazione completa

Applicare in quest'ordine:

```sh
settings --user 0 delete secure always_on_vpn_lockdown
settings --user 0 delete secure always_on_vpn_app
settings --user 0 delete secure always_on_vpn_lockdown_whitelist
```

Rileggere tutte e tre le chiavi e richiedere `null` prima del riavvio, come nella guida concordata. La cancellazione di una chiave già assente è valida: `Deleted 0 rows` non è un errore. Non sostituire `delete` con `put ... null`, che scriverebbe una stringa.

La sequenza deve funzionare anche quando è attivo soltanto l'always-on e il kill switch è già OFF. In caso di risultati discordanti non annunciare successo e non riavviare automaticamente. Conservare il minimo stato locale utile a spiegare una modifica parziale, senza includere segreti nei log.

## Riavvio e riconnessione

Dopo il readback favorevole richiedere il riavvio attraverso il servizio ADB appropriato, equivalente a `adb reboot`. Verificare l'API concreta della libreria: non assumere che una generica stringa nella shell equivalga al servizio ADB di riavvio.

La perdita della connessione è attesa, ma non dimostra da sola il riavvio riuscito. Registrare l'operazione pendente, ritrovare il dispositivo tramite discovery, verificarne nuovamente l'identità e attendere l'avvio con segnali disponibili, ad esempio `getprop sys.boot_completed` e un indicatore di avvio confrontabile se leggibile. Implementare timeout finiti e retry limitati; non inviare ciclicamente comandi mutanti o reboot per una risposta persa.

Dopo l'avvio:

- ON: verificare app Proton, lockdown `1`, nessuna eccezione.
- OFF: verificare app `null`, lockdown `null` oppure `0`, whitelist assente o vuota.

Le verifiche di applicazione runtime disponibili sul firmware possono integrare la rilettura, documentandone limiti e formato. Le sole chiavi non certificano il blocco di ogni flusso. La qualifica funzionale sul target resta necessaria.

Se l'app va in background, viene ruotata o viene terminata, non deve perdere silenziosamente il significato dell'operazione: alla riapertura rilegge lo stato e riconcilia l'eventuale operazione pendente. Nessun bisogno di un servizio residente permanente.

## Errori e comportamento sicuro

Distinguere almeno: pairing rifiutato/scaduto, credenziali revocate, discovery vuota, endpoint cambiato, rete locale non accessibile, permesso di rete negato, target incompatibile, errore shell, configurazione parziale e riavvio non verificato. Le risposte shell devono avere framing e controlli affidabili; non interpretare una semplice occorrenza di `1` in un output come successo.

Usare comandi fissi e argomenti validati; non interpolare codice di pairing, input UI o indirizzi nella shell remota. Bloccare i doppi tap concorrenti. Un errore durante ON non deve attivare automaticamente una procedura OFF che riduca una protezione preesistente. Preparare un recupero esplicito e idempotente.

Il popup è guida e conferma: non abilita adbd quando è spento. In tale caso mostrare i passi dal telecomando e una nuova ricerca; l'IP manuale aiuta solo se esiste un endpoint accessibile.

## Configurazione intermedia per il collaudo

Per il caso D09 della procedura di accettazione, l'operatore può preparare sul target già identificato questo stato dalla shell ADB autorizzata:

```sh
settings --user 0 put secure always_on_vpn_app ch.protonvpn.android
settings --user 0 put secure always_on_vpn_lockdown 0
settings --user 0 get secure always_on_vpn_app
settings --user 0 get secure always_on_vpn_lockdown
```

Richiedere readback Proton/`0`, quindi applicare il riavvio con il servizio ADB già descritto e rileggere. Questa fixture serve esclusivamente al collaudo: non introduce un terzo pulsante o un comportamento alternativo nell'app. La successiva operazione OFF completa è il ripristino previsto; le precondizioni sul target e sulla whitelist restano valide.

## Recupero da PC via USB

Il collegamento USB dati e l'alimentazione adeguata sono già stati confermati dall'utente su questa Chromecast. Il PC deve essere autorizzato e il Debug USB abilitato. `-d` seleziona il trasporto USB ed evita ambiguità con le connessioni wireless dello stesso dispositivo.

Il blocco ripristina le tre chiavi alla baseline assente e riavvia solo dopo il readback. Il percorso SDK è quello comunicato dall'utente su Fedora; non è un percorso richiesto dall'app Android.

```bash
CC_ADB="$HOME/Android/Sdk/platform-tools/adb"

if "$CC_ADB" -d shell settings --user 0 delete secure always_on_vpn_lockdown &&
   "$CC_ADB" -d shell settings --user 0 delete secure always_on_vpn_app &&
   "$CC_ADB" -d shell settings --user 0 delete secure always_on_vpn_lockdown_whitelist &&
   [ "$("$CC_ADB" -d shell settings --user 0 get secure always_on_vpn_app | tr -d '\r\n')" = "null" ] &&
   [ "$("$CC_ADB" -d shell settings --user 0 get secure always_on_vpn_lockdown | tr -d '\r\n')" = "null" ] &&
   [ "$("$CC_ADB" -d shell settings --user 0 get secure always_on_vpn_lockdown_whitelist | tr -d '\r\n')" = "null" ]; then
    echo "Configurazione di sistema rimossa. Riavvio della Chromecast..."
    "$CC_ADB" -d reboot
else
    echo "Rimozione non confermata. Controlla cavo, Debug USB e autorizzazione del PC."
fi
```

Dopo il riavvio rileggere app e lockdown con il medesimo selettore `-d`; valgono le condizioni OFF indicate sopra. Questa procedura è recupero tecnico e non il percorso ordinario richiesto all'utente dell'app.
