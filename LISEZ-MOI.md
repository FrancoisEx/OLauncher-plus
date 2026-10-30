# Olauncher+ — v01a_2026-10-30

Ce fichier accompagne aussi bien l'archive **complète** (installation depuis rien)
que l'archive **différentielle** `olauncher-plus-diff-v01a_…` : celle-ci contient
seulement les fichiers changés depuis la v01, et s'installe par-dessus la v01.

## Nouveautés de la v01a (installation par-dessus la v01, réglages conservés)

- **Réorganiser les favoris** : Réglages › Écran d'accueil › « Réorganiser les
  favoris ». Toute la liste s'affiche ; ▲ / ▼ déplacent un favori sans fermer la
  fenêtre. Les ▲ / ▼ de l'appui long restent disponibles.
- **Points de notification** : Réglages › Écran d'accueil › « Points de
  notification ». Un petit point apparaît à côté des favoris, et sur les icônes
  rapides, des applis qui ont une notification non lue (les notifications
  permanentes — musique, recharge — sont ignorées). Désactivé par défaut ;
  l'activer demande l'**accès aux notifications** (fenêtre d'Android). Si
  l'interrupteur est grisé : Paramètres › Applications › Olauncher+ › ⋮ ›
  « Autoriser les paramètres restreints ». Seul le nom de l'appli émettrice est
  utilisé, en mémoire ; le contenu des notifications n'est jamais lu.

Rien à supprimer à la main, aucune resynchronisation.

Olauncher+ est une version personnelle d'Olauncher (logiciel libre, licence GPL-3.0,
auteur d'origine : Tanuj M., https://github.com/tanujnotes/Olauncher), basée sur
Olauncher v6.10.27. Elle s'installe **à côté** d'Olauncher (identifiant
`app.olauncher.plus`) et apparaît sous le nom « Olauncher+ ».

## Ce qui change par rapport à Olauncher

1. **Réordonner les favoris** — appui long sur un favori de l'écran d'accueil :
   en haut de la liste qui s'ouvre, deux boutons **▲** et **▼** le font monter ou
   descendre d'un cran. (Le renommage et le changement d'appli restent possibles
   comme avant.)
2. **Icônes rapides** — une colonne de 0 à 5 petites icônes (noir et blanc) sur le
   bord opposé aux favoris : à droite si les favoris sont à gauche ou centrés, à
   gauche s'ils sont à droite.
   - Au premier lancement : Téléphone, Messages, Appareil photo.
   - Toucher : ouvre l'appli. Appui long : choisir une autre appli.
   - Nombre d'icônes : Réglages (appui long sur l'accueil) › Écran d'accueil ›
     « Icônes rapides » (0 = désactivé).
3. La version installée s'affiche dans les Réglages, à la place de « À propos
   d'Olauncher ».

Le fond d'écran quotidien d'Olauncher est conservé (Réglages › Fond d'écran du jour).

## Obtenir l'APK

L'APK se construit automatiquement sur GitHub (gratuit) grâce au fichier
`.github/workflows/olauncher-plus.yml` :

1. Créer un compte sur https://github.com et un dépôt nommé `olauncher-plus`
   (public : le lien de téléchargement fonctionnera sans connexion).
2. Y envoyer le contenu de cette archive (dossier caché `.github` compris), sur
   la branche principale `main`.
3. Onglet **Actions** : la construction démarre seule (environ 5 minutes).
4. Section **Releases** du dépôt : télécharger le fichier `olauncher-plus-v…apk` le plus récent
   depuis le téléphone, l'ouvrir, autoriser l'installation, puis choisir
   Olauncher+ comme lanceur par défaut.

Les versions suivantes porteront la même signature : elles s'installeront
par-dessus sans perdre vos réglages.

## Fichiers changés depuis la v01 (contenu de l'archive différentielle v01a)

```
LISEZ-MOI.md
app/build.gradle                         VERSION v01a, versionCode 116
app/src/main/AndroidManifest.xml         service des points de notification
app/src/main/java/app/olauncher/data/Prefs.kt
app/src/main/java/app/olauncher/helper/DotDrawable.kt            (nouveau)
app/src/main/java/app/olauncher/helper/NotificationDotService.kt (nouveau)
app/src/main/java/app/olauncher/ui/HomeFragment.kt
app/src/main/java/app/olauncher/ui/SettingsFragment.kt
app/src/main/res/layout/fragment_settings.xml        (+ layout-land/)
app/src/main/res/values/strings.xml
app/src/main/res/values-fr/strings.xml
```

## Fichiers modifiés ou ajoutés par rapport à Olauncher v6.10.27 (v01)

```
app/build.gradle                         identifiant, VERSION, signature
app/olauncher-plus.keystore              (nouveau) clé de signature du fork
app/src/main/java/app/olauncher/data/Constants.kt
app/src/main/java/app/olauncher/data/Prefs.kt
app/src/main/java/app/olauncher/MainViewModel.kt
app/src/main/java/app/olauncher/ui/AppDrawerFragment.kt
app/src/main/java/app/olauncher/ui/HomeFragment.kt
app/src/main/java/app/olauncher/ui/SettingsFragment.kt
app/src/main/res/layout/fragment_app_drawer.xml      (+ layout-land/)
app/src/main/res/layout/fragment_home.xml            (+ layout-land/)
app/src/main/res/layout/fragment_settings.xml        (+ layout-land/)
app/src/main/res/values/strings.xml
app/src/main/res/values-fr/strings.xml
app/src/main/res/values/dimens_plus.xml  (nouveau)
scripts/verifier_version.sh              (nouveau) contrôle de version
.github/workflows/olauncher-plus.yml     (nouveau) construction de l'APK
LISEZ-MOI.md                             (nouveau) ce fichier
```

Aucun fichier à supprimer à la main. Aucune resynchronisation.

## Contrôles passés avant envoi

- VERSION `v01a_2026-10-30` (versionName de `app/build.gradle`) conforme à la forme
  `vnn[lettre]_AAAA-MM-JJ` ; le nom de l'archive la reprend (script
  `scripts/verifier_version.sh`, rejoué aussi par GitHub sur le nom de l'APK).
- Relecture complète du code ajouté.
- v01 compilée avec succès par GitHub et testée sur téléphone par François.
- v01a : compilation faite par GitHub à l'envoi (l'environnement de rédaction
  n'a pas accès aux serveurs Android) ; en cas d'échec, l'onglet Actions affiche
  l'erreur.

## À savoir

- La clé de signature est dans le dépôt : pratique pour un usage personnel, mais
  quiconque y a accès pourrait signer une appli « compatible ». Garder le dépôt
  privé si cela vous gêne (le téléchargement de l'APK demandera alors d'être
  connecté à GitHub).
- Licence GPL-3.0 conservée, comme l'exige le code d'origine.
