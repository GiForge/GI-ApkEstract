# Android APK Extractor & Manager

<p align="center">
  <img width="334" height="732" alt="App Screenshot" src="https://github.com/user-attachments/assets/5928bab7-7f4d-42d8-94df-c052f1064d34" />
</p>

---

## Badge di Progetto

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg?logo=kotlin&logoColor=white)]()
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg?logo=jetpackcompose&logoColor=white)]()
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24%2B-blue.svg)]()
[![Target SDK](https://img.shields.io/badge/Target%20SDK-36-orange.svg)]()

---

## Descrizione Generale

Un'applicazione Android moderna e veloce creata per scansionare, analizzare ed estrarre i file APK delle applicazioni installate sul dispositivo (sia di sistema che utente). L'app è progettata con l'interfaccia **Material 3** e garantisce un'esperienza d'uso fluida grazie alla gestione asincrona dello stato dei dati.

---

## Funzionalità Dettagliate

* **Scansione ed Elenco App:** 
  * Ricerca rapida in tempo reale.
  * Filtri avanzati per distinguere app Utente, di Sistema o tutte le applicazioni.
  * Ordinamento personalizzabile (per nome, dimensione o data di aggiornamento).
* **Estrazione Singola e Multipla (Batch):** 
  * Estrazione ad alte prestazioni dei file APK.
  * Supporto all'estrazione simultanea (Batch) con indicatore di progresso in tempo reale.
* **Condivisione Avanzata:** 
  * Invio diretto via **Bluetooth**.
  * Integrazione con il foglio di condivisione nativo di sistema Android.
* **Gestore File Integrato:** 
  * Scheda dedicata **"APK Estratti"** per visualizzare, rinominare, condividere o eliminare gli APK archiviati nella memoria locale.
* **Scheda Dettagli Tecnici:** 
  * Visualizzazione approfondita dei dettagli del pacchetto, versione, codice di build, permessi dichiarati e percorsi di sistema del file sorgente.

---

## Tech Stack & Architettura

L'applicazione segue i principi dell'Android Modern Development (MAD):

* **Linguaggio:** Kotlin 2.2
* **Interfaccia Grafica:** Jetpack Compose (Material 3)
* **Architettura:** MVVM (Model-View-ViewModel)
* **Concorrenza & Reattività:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Condivisione File:** `FileProvider` di AndroidX per un trasferimento sicuro senza esporre URI `file://` grezzi.

---

## Permessi di Sistema

| Permesso | Motivazione |
| :--- | :--- |
| `QUERY_ALL_PACKAGES` | Necessario per rilevare ed elencare le applicazioni installate sul dispositivo (Android 11+). |
| `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` | Utilizzato per il salvataggio e la lettura degli APK estratti sul disco locale *(limitato o gestito tramite Scoped Storage sulle versioni recenti)*. |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` | Richiesto per l'invio e il trasferimento diretto dei file APK tramite dispositivi Bluetooth associati. |

---

## Istruzioni di Compilazione ed Esecuzione

### Requisiti
* **Android Studio** (versione recente supportante Kotlin 2.2 e Gradle aggiornato)
* **JDK 17** o superiore

### Comandi Gradle

Per compilare la versione di debug ed eseguire i test unitari, utilizzare i seguenti comandi dalla root del progetto:

* **Compilazione APK Debug:**
  ```bash
  ./gradlew assembleDebug
  ```

* **Esecuzione Test Unitari:**
  ```bash
  ./gradlew testDebugUnitTest
  ```
