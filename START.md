# Avvio e ripresa

Leggi integralmente, nell'ordine, questo file, `AGENTS.md`, `ROADMAP.md` e `CORROBORATIONS.md`. Poi leggi `PROJECT_STATE.json` e `ARTIFACT_INDEX.json`.

Ricostruisci il punto di lavoro prima di modificare il progetto:

1. Controlla repository e remote, branch corrente, HEAD, stato del worktree e commit recenti. Identifica modifiche dell'utente o di altri agenti già presenti.
2. Confronta i deliverable realmente esistenti con le evidenze indicizzate e con i gate della roadmap. Una cache aggiornata non sostituisce questa verifica.
3. Determina la fase effettiva, il primo requisito non soddisfatto e le dipendenze che impediscono di affrontarlo.
4. Carica soltanto gli artefatti e i dossier pertinenti, usando l'indice. I percorsi dichiarati futuri non sono documenti mancanti da ricreare automaticamente.
5. Riprendi dal primo lavoro utile: implementazione, verifica, correzione o preparazione dell'azione che sblocca il passo successivo.
6. Prosegui attraverso le fasi fino a una condizione terminale definita dai documenti canonici. Le normali transizioni sono checkpoint del medesimo obiettivo.

Prima di interrompere il lavoro, aggiorna le cache ricostruibili e l'artefatto principale della fase con risultati, riferimenti alle prove, questioni aperte e prossima azione. Mantieni distinguibili ciò che è implementato, ciò che è stato verificato e ciò che richiede ancora una prova.

Per un nuovo avvio in modalità Goal, il mandato è in `docs/AI_PM_GOAL.md`; per una ripresa, conserva il mandato già attivo e ricostruisci lo stato corrente.
