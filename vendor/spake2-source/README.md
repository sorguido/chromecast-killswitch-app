# Sorgente corrispondente SPAKE2

Conservo due archivi upstream non modificati, per complessivi circa 124 KiB:

- [SPAKE2-Java, tag 2.2.1](https://github.com/MuntashirAkon/spake2-java/tree/2.2.1): `spake2-java-2.2.1.tar.gz`.
- [Submodule SPAKE2-C, commit 0d15933e5ba3e662cb01245a7ac0dc9fca3eac31](https://github.com/MuntashirAkon/spake2-c/tree/0d15933e5ba3e662cb01245a7ac0dc9fca3eac31): `spake2-c-0d15933.tar.gz`. Questo SHA è il gitlink del tag Java; il solo archivio GitHub Java non include i sorgenti del submodule.

[SHA256SUMS](SHA256SUMS) verifica i byte degli archivi. Ogni archivio include le attribuzioni nei sorgenti e la licenza originale. Le copie leggibili della [LGPL-3.0](COPYING.LESSER) e della [GPL-3.0](COPYING) accompagnano la distribuzione. Copyright 2021 Muntashir Al-Islam; le parti derivate mantengono i rispettivi autori, incluso il codice BoringSSL/Google presente nel submodule e i file EdDSA/CC0 della variante Java.

Il tag richiesto è `2.2.1`, anche se la variabile `version` nel file Gradle upstream reca ancora `2.2.0`. Non la uso per identificare l'artefatto risolto: coordinate JitPack, tag, SHA del submodule e hash dell'AAR sono registrati nel [dossier trasporto](../../research/decisions/TRANSPORT.md).

## Estrarre, modificare e ricompilare

`bash vendor/spake2-source/rebuild.sh --prepare-only` verifica i checksum ed estrae entrambi gli archivi in `.local/spake2-rebuild`. La copia di lavoro include tutti i sorgenti e può essere modificata. Richiamare lo script non sovrascrive una copia già preparata.

Lo script sostituisce esclusivamente la configurazione Gradle nella copia di lavoro: usa Gradle dell'app, AGP 9.4.0, Java 17, compile SDK/build tools 36, min SDK 26, NDK `27.2.12479018`, CMake `3.22.1` e allineamento massimo pagina 16384. La crittografia resta nei sorgenti estratti. Occorrono SDK, NDK e CMake indicati; `ANDROID_HOME` punta allo SDK scelto. La sintassi e l'estrazione sono verificate; la compilazione nativa di questa ricetta alternativa resta non eseguita finché manca la toolchain NDK/CMake. L'AAR usato e qualificato nella build normale resta quello originale JitPack 2.2.1.

```bash
bash vendor/spake2-source/rebuild.sh
./gradlew :app:assembleRelease \
  -Pspake2Aar="$PWD/.local/spake2-rebuild/android/build/outputs/aar/android-release.aar" \
  --write-locks
```

Il secondo comando ricombina l'app con la libreria modificata; `--write-locks` aggiorna la copia locale dei lock per la sostituzione intenzionale. Configurare una propria firma secondo la guida di build del progetto. Le chiavi private della release originale non fanno parte del sorgente necessario per installare una propria build: si può disinstallare la precedente app e installare l'APK firmato personalmente. Questo cancella associazione e stato locale; verificare prima sul TV la configurazione e completare eventuali recuperi pendenti, quindi effettuare una nuova associazione. Non occorre una modifica al firmware della Chromecast.

La sostituzione mantiene l'interfaccia Java/JNI prevista da LibADB. Una libreria modificata richiede nuovi test e non eredita le prove della release originale.
