# Organisation par modules

Le code est progressivement organisé autour des domaines métier. Chaque module
peut contenir les sous-dossiers suivants :

- `web` : contrôleurs HTTP ;
- `application` : services et cas d'usage ;
- `persistence` : repositories ;
- `dto` : objets d'entrée et de sortie de l'API.

Les entités restent à la racine de leur module ou dans un sous-domaine lorsque
cela apporte de la clarté (par exemple `booking/room` et `catalog/room`).

## Modules déjà déplacés

- `auth`
- `billing`
- `booking`
- `catalog/room`
- `catalog/space`
- `catalog/pool`
- `catalog/meetingroom`
- `customer`

## Règle de migration

Chaque fichier déplacé porte le package correspondant à son emplacement réel,
et tous ses imports internes sont mis à jour dans la même modification. Les
anciens fichiers sont supprimés : il ne doit pas y avoir de copie dans les
anciens dossiers techniques.
