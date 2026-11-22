# AFBG — All (you need) For BoardGaming

Application Android (Kotlin + Jetpack Compose) qui regroupe les besoins récurrents autour des jeux de société.

| Onglet | Fonction |
|---|---|
| **Premier joueur** | Façon *Chwazi* : chacun pose un doigt, au bout de 2,5 s l'appli désigne un premier joueur. Modes : *Premier joueur*, *Ordre* (classement de tous les doigts), *Équipes* (2 à 5). |
| **Scores** | Façon *+10 compteur* : un joueur par carte colorée, boutons −/+ avec pas réglable (1, 5, 10, personnalisé), historique et annulation, joueur en tête. Sauvegardé automatiquement. « ✓ » enregistre la partie dans l'historique. |
| **Ludothèque** | Liste avec recherche, ajout par recherche BoardGameGeek ou manuel, import de sa collection BGG, fiche détaillée (description, note, notes perso, actualisation BGG). |
| **Parties** | Historique des parties (jeu, date, durée, lieu, joueurs, scores, vainqueurs). |
| **Prêts** | Jeux prêtés (à qui, depuis quand, nombre de jours), rendu en un appui, historique. |

## Source de données : BoardGameGeek

L'API XML2 de BGG exige désormais un **jeton d'application** :
1. créer une application sur <https://boardgamegeek.com/applications> ;
2. coller le jeton dans *Ludothèque → ⚙ Réglages*.

Tric Trac et Myludo n'exposent pas d'API publique documentée ; l'accès BGG est isolé dans `data/bgg/`, une autre source pourra s'y ajouter.

## Compiler

Prérequis : JDK 17, Android SDK (API 35).

```bash
./gradlew testDebugUnitTest   # tests unitaires (parseur BGG)
./gradlew assembleDebug       # APK dans app/build/outputs/apk/debug/
```

La CI GitHub Actions (`.github/workflows/android.yml`) construit l'APK debug à chaque push et le publie comme artefact.

## Architecture

- `data/db` : Room (jeux, parties, joueurs de partie, prêts)
- `data/bgg` : client OkHttp + parseur XML (testable sans Android)
- `data/Repository.kt` : point d'accès unique aux données
- `ui/*` : un package par écran (Compose + ViewModel), navigation dans `ui/AppRoot.kt`
