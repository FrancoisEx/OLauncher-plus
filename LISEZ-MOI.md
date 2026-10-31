# Olauncher+ — v02a_2026-10-31

## Correctif v02a (s'installe par-dessus la v02, rien d'autre à faire)

- Fenêtre d'un dossier : le **clavier ne s'ouvrait pas** en touchant le nom du
  dossier ou d'un élément. Corrigé.
- Une petite légende « Nom du dossier (toucher pour modifier) » signale que la
  première ligne se modifie.

Fichiers changés depuis la v02 : `LISEZ-MOI.md`, `app/build.gradle`,
`app/src/main/java/app/olauncher/ui/HomeFragment.kt`,
`app/src/main/res/values/strings.xml`, `app/src/main/res/values-fr/strings.xml`.

---


Version **majeure** : à lire en entier. Elle s'installe toujours **par-dessus**
la v01, v01a, v01b ou v01c, sans rien perdre (favoris, icônes, réglages).
Rien à supprimer à la main, aucune resynchronisation.

Cette archive est **complète** (`olauncher-plus-complet-v02_…`) : elle contient
tout le code, et peut aussi servir à repartir de zéro.

## Nouveauté de la v02 : les dossiers

Un favori de l'accueil peut devenir un **dossier** : un mot (par exemple
« Infos ») qui ouvre une liste d'applis et de sites, jusqu'à 10. Un dossier
**prend la place d'un favori** : il n'ajoute aucune ligne à l'accueil. Plusieurs
dossiers sont possibles.

- **Créer** : appui long sur un favori › bouton **DOSSIER** en haut, à côté de
  ▲ ▼. Si vous avez tapé un mot dans la recherche, il devient le nom du dossier
  (sinon « Dossier »). La fenêtre du dossier s'ouvre aussitôt pour le remplir.
- **Remplir** : « + Ajouter une appli ou un site ». La liste proposée comprend les
  applis et les **raccourcis de sites** : dans Firefox, menu ⋮ › « Ajouter à
  l'écran d'accueil » crée un raccourci qu'Olauncher+ range parmi les applis.
- **Ouvrir** : toucher le nom du dossier (suivi d'un discret « › »). Son contenu
  remplace la liste des favoris, au même endroit et dans le même style, sous une
  petite ligne « ‹ Infos ». Pour le refermer : Retour, toucher la ligne
  « ‹ Infos », toucher une zone vide, ou quitter l'accueil.
- **Tenir à l'écran** : si les lignes ne tiennent pas, l'espacement se resserre,
  puis le texte rapetisse.
- **Gérer** : appui long sur le dossier (fermé ou ouvert). On y renomme le
  dossier et chaque élément (toucher le nom pour le modifier : utile pour les
  titres de sites trop longs), on réordonne (▲ ▼), on retire (✕), on ajoute.
  « Défaire ce dossier » (deux touchers) rend un favori vide ; le contenu est perdu.
- Un dossier se déplace comme un favori (▲ ▼, « Réorganiser les favoris »),
  avec son contenu.

### Fichiers changés depuis la v01c

```
LISEZ-MOI.md
app/build.gradle                         VERSION v02, versionCode 119
app/src/main/java/app/olauncher/MainViewModel.kt
app/src/main/java/app/olauncher/data/Constants.kt
app/src/main/java/app/olauncher/data/Prefs.kt
app/src/main/java/app/olauncher/ui/AppDrawerFragment.kt
app/src/main/java/app/olauncher/ui/HomeFragment.kt
app/src/main/res/layout/fragment_app_drawer.xml      (+ layout-land/)
app/src/main/res/values/strings.xml
app/src/main/res/values-fr/strings.xml
```

## Nouveautés de la v01c

- **Trait plus épais** : les pictogrammes passent en Material Symbols *Rounded*,
  graisse 500 (au lieu de *Outlined* 300) : plus lisibles, bouts arrondis.
- **Navigateur** : globe plein à la place du globe quadrillé.
- **Choisir soi-même le pictogramme** : Réglages › Écran d'accueil ›
  « Personnaliser les icônes » (anciennement « Réorganiser les icônes »).
  Chaque ligne commence par son pictogramme : le toucher ouvre la grille du pack
  (31 pictogrammes), avec « Auto » en premier pour revenir au choix automatique.
  Changer l'appli d'un emplacement remet son pictogramme en « Auto ».

### Fichiers changés depuis la v01b

```
LISEZ-MOI.md
app/build.gradle                         VERSION v01c, versionCode 118
app/src/main/java/app/olauncher/data/Prefs.kt
app/src/main/java/app/olauncher/helper/QuickIcons.kt
app/src/main/java/app/olauncher/ui/HomeFragment.kt
app/src/main/java/app/olauncher/ui/SettingsFragment.kt
app/src/main/res/drawable/ic_quick_*.xml               (32, redessinés)
app/src/main/res/values/strings.xml
app/src/main/res/values-fr/strings.xml
```

## Nouveautés de la v01b

- **Icônes rapides homogènes** : chaque icône est désormais un pictogramme au
  trait d'un même jeu (Material Symbols, licence Apache 2.0), dans la couleur du
  texte, choisi selon le rôle de l'appli : téléphone, messages, appareil photo,
  navigateur, mail, galerie, musique, météo, enregistreur, cartes, agenda…
  (31 pictogrammes). Une appli non reconnue reçoit son initiale dans un cercle,
  au même trait. Le pas vertical des icônes (48 dp) diffère volontairement de
  celui des favoris : les deux colonnes ne tombent plus sur les mêmes lignes.
- **Position verticale** : Réglages › Écran d'accueil › « Position verticale » :
  Haut, Milieu ou Bas. Elle s'applique aux favoris et aux icônes rapides. En
  position Haut, la liste commence juste sous l'heure. L'ancien choix « Bas » du
  menu Alignement est repris automatiquement, puis retiré de ce menu.
- **Nom dans les Réglages** : le titre devient « Olauncher+, un fork de
  Olauncher », pour ne pas confondre ce lanceur avec l'original.
- **Réorganiser les icônes** : même fenêtre ▲ / ▼ que pour les favoris
  (devenue « Personnaliser les icônes » en v01c).

### Fichiers changés depuis la v01a

```
LISEZ-MOI.md
app/build.gradle                         VERSION v01b, versionCode 117
app/src/main/java/app/olauncher/data/Constants.kt
app/src/main/java/app/olauncher/data/Prefs.kt
app/src/main/java/app/olauncher/helper/QuickIcons.kt   (nouveau)
app/src/main/java/app/olauncher/ui/HomeFragment.kt
app/src/main/java/app/olauncher/ui/SettingsFragment.kt
app/src/main/res/drawable/ic_quick_*.xml               (32 nouveaux)
app/src/main/res/layout/fragment_home.xml            (+ layout-land/)
app/src/main/res/layout/fragment_settings.xml        (+ layout-land/)
app/src/main/res/menu/alignment.xml
app/src/main/res/menu/vertical_position.xml            (nouveau)
app/src/main/res/values/dimens_plus.xml
app/src/main/res/values/strings.xml
app/src/main/res/values-fr/strings.xml
```

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
2. **Icônes rapides** — une colonne de 0 à 5 petites icônes au trait sur le
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

- VERSION `v02a_2026-10-31` (versionName de `app/build.gradle`) conforme à la forme
  `vnn[lettre]_AAAA-MM-JJ` ; le nom de l'archive la reprend (script
  `scripts/verifier_version.sh`, rejoué aussi par GitHub sur le nom de l'APK).
- Relecture complète du code ajouté.
- v01 compilée avec succès par GitHub et testée sur téléphone par François.
- v01a compilée par GitHub et testée sur téléphone par François.
- v01b compilée par GitHub et installée sur téléphone par François.
- v01c compilée par GitHub et installée sur téléphone par François.
- v02 compilée par GitHub et essayée sur téléphone par François.
- v02a : compilation faite par GitHub à l'envoi (l'environnement de rédaction
  n'a pas accès aux serveurs Android) ; en cas d'échec, l'onglet Actions affiche
  l'erreur.

## À savoir

- La clé de signature est dans le dépôt : pratique pour un usage personnel, mais
  quiconque y a accès pourrait signer une appli « compatible ». Garder le dépôt
  privé si cela vous gêne (le téléchargement de l'APK demandera alors d'être
  connecté à GitHub).
- Licence GPL-3.0 conservée, comme l'exige le code d'origine.
