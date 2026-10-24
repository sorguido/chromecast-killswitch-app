# Componenti di terze parti

Questa app incorpora i componenti sotto elencati. Le licenze originali, i copyright e le esclusioni di garanzia restano applicabili ai rispettivi componenti; il repository privato e la selezione Apache di LibADB non trasformano SPAKE2 in codice proprietario o Apache.

## Componenti inclusi nell'APK

| Componente e versione | Autori / titolari indicati upstream | Licenza e materiale allegato |
| --- | --- | --- |
| LibADB Android 3.1.1, con modifiche locali | Muntashir Al-Islam; autori identificati negli header dei singoli file | Scelta Apache-2.0 per i file duali GPL-3.0-or-later / Apache-2.0; ulteriori header BSD/MIT conservati. [COPYING](vendor/libadb/COPYING), [licenze](vendor/libadb/LICENSES/Apache-2.0), [modifiche](vendor/libadb/LOCAL_CHANGES.md), sorgenti completi nel modulo. |
| SPAKE2 Android 2.2.1 | Copyright 2021 Muntashir Al-Islam | [LGPL-3.0](vendor/spake2-source/COPYING.LESSER), con [GPL-3.0](vendor/spake2-source/COPYING) incorporata. [Sorgenti esatti, submodule e ricompilazione](vendor/spake2-source/README.md). |
| SPAKE2-C, commit `0d15933e5ba3e662cb01245a7ac0dc9fca3eac31` | Google Inc., Muntashir Al-Islam, Free Software Foundation e autori negli header | Licenza del progetto LGPL-3.0; `spake2.c` conserva una licenza permissiva ISC, `sha512.c`/header derivati da glibc dichiarano LGPL-2.1-or-later, utilizzabile con versione successiva. Testi e attribuzioni originali sono nell'archivio del submodule allegato. |
| Conscrypt Android 2.5.3 | The Android Open Source Project e contributori Conscrypt; porzioni Apache Harmony e Netty | [Apache-2.0](vendor/licenses/Conscrypt-LICENSE) e [NOTICE originale](vendor/licenses/Conscrypt-NOTICE). Non è una libreria sottoposta a una nuova licenza da questa app. |
| BoringSSL incorporato da Conscrypt, commit `49f0329110a1d93a5febc2bceceedc655d995420` | OpenSSL Project, Eric Young, Google e autori specifici | [Testo completo](vendor/licenses/BoringSSL-LICENSE): OpenSSL/SSLeay, ISC e MIT per le porzioni indicate. Revisione rilevata da `org/conscrypt/conscrypt.properties` nell'AAR effettivamente risolto. |
| Bouncy Castle `bcprov`, `bcpkix`, `bcutil` jdk15to18, 1.81 | The Legion of the Bouncy Castle Inc. | [Licenza permissiva Bouncy Castle](vendor/licenses/BouncyCastle-LICENSE), estratta dall'artefatto `bcprov` risolto. |
| AndroidX, Compose e Material 3 | The Android Open Source Project e contributori AndroidX | Apache-2.0 secondo i POM risolti. [Testo completo](vendor/libadb/LICENSES/Apache-2.0). Le versioni di ogni modulo sono nel [lock dell'app](app/gradle.lockfile). Principali: Activity 1.10.1, Lifecycle 2.8.7, Compose UI 1.7.8, Material 3 1.3.2. |
| Kotlin stdlib 2.2.10, kotlinx-coroutines 1.10.2, JetBrains annotations 23.0.0 | JetBrains e contributori | Apache-2.0 secondo i POM risolti; [testo](vendor/libadb/LICENSES/Apache-2.0). |
| Guava ListenableFuture 1.0 | Google e contributori Guava | Apache-2.0; [sorgente originale](https://github.com/google/guava), [testo](vendor/libadb/LICENSES/Apache-2.0). |

L'archivio completo SPAKE2-Java conserva anche la variante Java non impiegata dal backend Android nativo, con attribuzioni EdDSA-Java/CC0. La presenza di quei sorgenti non significa che tutte le varianti siano collegate nell'APK. Dipendenze di test come JUnit ed Espresso e strumenti di build restano separati dal runtime della release; il lock distingue le configurazioni.

## Distribuzione e possibilità di modifica

SPAKE2 è una libreria LGPL inclusa nell'app. Accompagno l'APK con questo avviso, i testi LGPL/GPL, gli archivi dei sorgenti corrispondenti, il sorgente dell'app e gli script necessari a ricombinare l'app con una versione modificata della libreria. Non impongo restrizioni alla modifica delle porzioni LGPL né al reverse engineering necessario a verificarne le modifiche. L'app non controlla una firma obbligatoria per bloccare una libreria sostitutiva.

La procedura concreta è in [vendor/spake2-source/README.md](vendor/spake2-source/README.md). Il flag `-Pspake2Aar=/percorso/libreria.aar` sostituisce intenzionalmente la dipendenza Maven nel modulo LibADB; senza flag, la build usa ancora l'AAR originale 2.2.1. I lock della copia modificata devono essere aggiornati. Una propria firma consente l'installazione dopo aver disinstallato la precedente build; l'associazione va ripetuta e la configurazione sul TV va verificata prima di cancellare dati locali. Le chiavi di firma e ADB originali non devono essere distribuite.

Se l'APK viene trasferito a un altro destinatario, trasferire anche questa documentazione e l'archivio sorgente completo della medesima revisione, comprese `vendor/libadb`, `vendor/spake2-source` e `vendor/licenses`. Non sostituire il sorgente corrispondente con un collegamento generico a `master`; preservare checksum, copyright e licenze. Conservare le attribuzioni e i testi delle licenze permissive anche nella distribuzione binaria.

La sintassi e l'estrazione della ricetta alternativa SPAKE2 sono state controllate; non attribuisco alla libreria ricompilata le prove del binario upstream. L'esito della build normale e la corrispondenza dell'APK restano nel rapporto di qualità della release.

## Fonti verificate

Consultazione del 24 ottobre 2026: [LibADB 3.1.1](https://github.com/MuntashirAkon/libadb-android/tree/3.1.1), [SPAKE2-Java 2.2.1](https://github.com/MuntashirAkon/spake2-java/tree/2.2.1), [SPAKE2-C alla revisione indicata](https://github.com/MuntashirAkon/spake2-c/tree/0d15933e5ba3e662cb01245a7ac0dc9fca3eac31), [Conscrypt 2.5.3](https://github.com/google/conscrypt/tree/2.5.3) e [BoringSSL alla revisione dell'AAR](https://boringssl.googlesource.com/boringssl/+/49f0329110a1d93a5febc2bceceedc655d995420/LICENSE). I POM e gli artefatti effettivamente risolti integrano queste fonti per le dipendenze transitive; hash e ABI dei componenti crittografici sono nel [dossier trasporto](research/decisions/TRANSPORT.md).
