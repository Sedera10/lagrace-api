# Guide de tarification des catalogues

Ce document décrit le comportement présent dans le code Java (entités, dépôts,
services et création des réservations). Les colonnes ci-dessous proviennent des
mappings JPA; ce n'est pas un DDL exhaustif de la base.

## Règles communes

- Les tarifs datés ont généralement `valid_from` obligatoire et `valid_to`
  facultatif (`NULL` signifie sans date de fin).
- Les requêtes d'applicabilité utilisent des bornes inclusives :
  `valid_from <= :date AND (valid_to IS NULL OR valid_to >= :date)`.
- `created_at` indique quand la ligne a été créée; ce n'est pas sa période de
  validité.
- Le prix est `BigDecimal` et les tables de tarifs utilisent `amount` en
  `numeric(12,2)`.
- Une unité (`unit_id`) n'est pas toujours une simple étiquette : elle détermine
  parfois la quantité à facturer (`Forfait`, `Par personne`, `Jour`, `Heure`).
- Lors de la réservation, les services copient généralement le tarif choisi
  dans `unit_price` et stockent la quantité sur la ligne ou la charge. Le prix
  du catalogue peut ensuite évoluer sans recalcul automatique de l'historique.

Les requêtes PostgreSQL ci-dessous utilisent `DISTINCT ON`; elles ne sont donc
pas directement portables vers tous les moteurs SQL. La priorité actuelle est
souvent : unité/dimensions, tarifs à fin bornée avant tarifs permanents, puis
`valid_from` décroissante et `id_rate` décroissant. Les comportements détaillés
par catalogue priment sur cette règle générale.

## Vue d'ensemble des schémas

| Catalogue | Table de prix | Dimensions qui distinguent les tarifs |
|---|---|---|
| Chambres | `room_rate` | type de chambre, unité |
| Espaces | `space_rate` | espace, unité, minimum et maximum de personnes |
| Banquet | `banquet_rate` | type de menu, minimum et maximum de personnes |
| Piscines | `pool_rate` | piscine, formule, unité, tranche d'âge ou repas inclus |
| Salles de réunion | `meeting_room_rate` | salle, formule, unité |
| Services additionnels | `additional_service` | périmètre, unité; prix fixe, sans période de validité |

## 1. Chambres

### Schéma et portée

La table [`room_rate`](../src/main/java/mg/lagrace/api/catalog/room/RoomRate.java)
contient :

| Colonne | Rôle |
|---|---|
| `id_rate` | identifiant du tarif |
| `type_id` | type de chambre concerné, pas une chambre individuelle |
| `unit_id` | unité du tarif |
| `amount` | montant unitaire |
| `valid_from`, `valid_to` | période d'applicabilité |
| `created_at` | date de création |

La contrainte JPA unique est `(type_id, unit_id, valid_from)`. Un tarif défini
pour un type concerne donc toutes les chambres de ce type.

### Requêtes et services

Dans [`RoomRateRepository`](../src/main/java/mg/lagrace/api/catalog/room/persistence/RoomRateRepository.java) :

- `findApplicableByType(typeId, date)` retourne au plus un tarif par unité. Sa
  requête native est :

  ```sql
  SELECT DISTINCT ON (unit_id) *
  FROM room_rate
  WHERE type_id = :typeId
    AND valid_from <= :date
    AND (valid_to IS NULL OR valid_to >= :date)
  ORDER BY unit_id, (valid_to IS NULL), valid_from DESC, id_rate DESC
  ```

- `findApplicableNightRatesByTypes(typeIds, date)` est la lecture optimisée
  pour plusieurs types. Elle ne garde que les unités dont
  `LOWER(unit.service) = 'room'` et `LOWER(unit.name) = 'nuit'`, et retourne un
  tarif par `type_id`. Sa projection fournit `typeId`, `rateId`, `amount` et
  `unitName`.
- `findByType_IdTypeAndUnit_Service` et
  `findByType_IdTypeAndUnit_IdUnitAndValidFrom` sont des requêtes dérivées de
  Spring Data.
- `existsByType_IdTypeAndUnit_IdUnitAndValidFromAndIdRateNot`,
  `existsByType_IdTypeAndUnit_IdUnitAndValidToIsNullAndIdRateNot` et
  `existsOverlappingPromo` servent aux contrôles de doublon/période.

Dans [`RoomTypeService`](../src/main/java/mg/lagrace/api/catalog/room/application/RoomTypeService.java) :

- `listTarifs(typeId, date)` liste tout l'historique du type. La réponse
  [`RoomRateResponse`](../src/main/java/mg/lagrace/api/catalog/room/dto/room/rate/RoomRateResponse.java)
  contient notamment `unitName`, `amount`, les dates et `applicable`.
- `getApplicableNightRate` et `getApplicableNightRates` servent aux appels
  unitaires ou groupés pour les tarifs « Nuit ».
- `createRate` / `updateRate` valident le type, l'unité de service `ROOM`, les
  dates et certaines collisions; `deleteRate` supprime le tarif.

La réservation d'une chambre appelle
`BookingRoomService.findRoomNightRate(room, checkIn)` et utilise le tarif «
Nuit » applicable à la date d'arrivée. C'est ce prix qui est copié dans
`unit_price`; le calcul n'effectue pas une nouvelle sélection de tarif pour
chaque nuit du séjour.

API de gestion : `/api/room-types/{typeId}/tarifs`, avec `date` facultative en
lecture.

## 2. Espaces

### Schéma et portée

[`space_rate`](../src/main/java/mg/lagrace/api/catalog/space/SpaceRate.java)
contient `space_id`, `unit_id`, `min_persons`, `max_persons`, `amount`,
`valid_from`, `valid_to` et `created_at`. Les bornes de personnes sont
facultatives; `NULL` signifie qu'elles ne limitent pas ce côté du palier.

### Requêtes et services

[`SpaceRateRepository.findApplicableBySpace`](../src/main/java/mg/lagrace/api/catalog/space/SpaceRateRepository.java)
retourne un tarif par unité et palier :

```sql
SELECT DISTINCT ON (unit_id, min_persons, max_persons) *
FROM space_rate
WHERE space_id = :spaceId
  AND valid_from <= :date
  AND (valid_to IS NULL OR valid_to >= :date)
ORDER BY unit_id, min_persons, max_persons,
         (valid_to IS NULL), valid_from DESC, id_rate DESC
```

Dans [`SpaceService`](../src/main/java/mg/lagrace/api/catalog/space/application/SpaceService.java) :

- `getTarifs(spaceId)` retourne tous les tarifs de l'espace.
- `currentRates(spaceId, date)` applique la date donnée, ou `LocalDate.now()`
  si elle est absente.
- `othersRates(spaceId, date)` retourne les tarifs qui ne sont pas sélectionnés
  comme applicables à cette date.
- `addTarif` / `updateTarif` contrôlent notamment le montant strictement
  positif, les dates et `minPerson <= maxPerson`.

À la création ou modification d'une réservation, la méthode
`BookingSpaceService.findSpaceRate(space, eventDate, guestsCount)` filtre aussi
les paliers selon le nombre d'invités. Si plusieurs paliers correspondent, elle
préfère un tarif à fin bornée, puis le `valid_from` et l'identifiant les plus
récents. `quantityForSpaceRate` accepte actuellement :

- `Forfait` : quantité `1`;
- `Par personne` : quantité égale au nombre d'invités;
- toute autre unité : erreur.

La ligne de réservation conserve le tarif unitaire et la quantité retenus.

API exposée : `/api/spaces/{idSpace}/rates` pour la liste, et
`POST|PUT|DELETE /api/spaces/rates...` pour la gestion. Le service possède
aussi `currentRates` et `othersRates`, mais le contrôleur actuel n'expose pas
de routes dédiées à ces deux méthodes.

## 3. Banquet

### Schéma et portée

[`banquet_rate`](../src/main/java/mg/lagrace/api/catalog/space/BanquetRate.java)
contient `menu_type_id`, `min_persons`, `max_persons`, `amount`, `valid_from`,
`valid_to` et `created_at`. Il n'y a pas de `unit_id`; le calcul de réservation
utilise le nombre de personnes comme quantité.

### Requêtes et services

[`BanquetRateRepository.findApplicableByMenuType`](../src/main/java/mg/lagrace/api/catalog/space/persistence/BanquetRateRepository.java)
retourne au plus une ligne par paire de seuils :

```sql
SELECT DISTINCT ON (min_persons, max_persons) *
FROM banquet_rate
WHERE menu_type_id = CAST(:menuTypeId AS integer)
  AND valid_from <= :date
  AND (valid_to IS NULL OR valid_to >= :date)
ORDER BY min_persons, max_persons,
         (valid_to IS NULL), valid_from DESC, id_rate DESC
```

[`BanquetService`](../src/main/java/mg/lagrace/api/catalog/space/application/BanquetService.java)
offre `getRates`, `getCurrentRates(date)` et `getOtherRates(date)`. La lecture
des tarifs courants parcourt les types de menu et appelle la requête pour
chacun. Les réponses
[`BanquetRateResponse`](../src/main/java/mg/lagrace/api/catalog/space/dto/banquet/BanquetRateResponse.java)
incluent le type de menu, les bornes, le montant et la période.

`BookingSpaceService.resolveBanquetRates` filtre par nombre de personnes et
exige exactement un palier. Zéro ou plusieurs correspondances provoquent une
erreur `NO_TARIFF`. Lors de la création d'une charge banquet, la quantité
devient le nombre de personnes et le `unit_price` est le montant du palier.

La validation de création exige `minPersons >= 1`, `maxPersons >= minPersons`
si le maximum existe et un montant positif ou nul.

API : `/api/banquets/rates`, `/api/banquets/rates/current?date=...` et
`/api/banquets/rates/others?date=...`.

## 4. Piscines

### Schéma et portée

[`pool_rate`](../src/main/java/mg/lagrace/api/catalog/pool/PoolRate.java)
contient `pool_id`, `service_id`, `unit_id`, `meal_included`, `min_age`,
`max_age`, `amount`, `valid_from`, `valid_to` et `created_at`.

Un tarif décrit soit une formule par âge, soit une formule avec/sans repas.
`PoolManagmentService.validateRequest` rejette une requête qui mélange ces deux
modèles ou qui ne renseigne aucun des deux.

### Requêtes et services

[`PoolRateRepository.findApplicableByPoolAndService`](../src/main/java/mg/lagrace/api/catalog/pool/persistence/PoolRateRepository.java)
sélectionne un tarif par unité et dimensions (repas, âge) :

```sql
SELECT DISTINCT ON (unit_id, meal_included, min_age, max_age) *
FROM pool_rate
WHERE pool_id = :poolId
  AND service_id = :serviceId
  AND valid_from <= :date
  AND (valid_to IS NULL OR valid_to >= :date)
ORDER BY unit_id, meal_included, min_age, max_age,
         (valid_to IS NULL), valid_from DESC, id_rate DESC
```

Dans [`PoolManagmentService`](../src/main/java/mg/lagrace/api/catalog/pool/application/PoolManagmentService.java) :

- `allRates(poolId)` retourne l'historique.
- `currentRates(poolId, date)` parcourt les formules de la piscine et agrège
  leurs tarifs applicables. La date par défaut utilise l'horloge réglée sur
  `Indian/Antananarivo`.
- `applicableRates(poolId, serviceId, date)` lit une formule déterminée.
- `otherRates` compare les identifiants courants avec l'historique.

La réponse
[`PoolRateResponse`](../src/main/java/mg/lagrace/api/catalog/pool/dto/pool/PoolRateResponse.java)
retourne les identifiants piscine/formule/unité, les dimensions âge/repas, des
libellés dérivés, le montant et la période.

Pour une réservation, `BookingPoolService.resolveRate` vérifie le `rateId`
fourni : il doit appartenir à la piscine et à la formule demandées et faire
partie des tarifs applicables à la date. La quantité est calculée selon l'unité
et le code de formule :

- formule `ENTRY` : quantité demandée;
- `Forfait` : `1`;
- `Par personne` : nombre d'invités;
- `Heure` : durée arrondie à l'heure supérieure, seulement pour une
  privatisation avec une heure de fin.

API de lecture : `/api/pools/{idPool}/rates?serviceId=...&date=...`,
`/api/pools/{idPool}/rates/currents` et
`/api/pools/{idPool}/rates/others`.

## 5. Salles de réunion

### Schéma et portée

[`meeting_room_rate`](../src/main/java/mg/lagrace/api/catalog/meetingroom/MeetingRoomRate.java)
contient `meeting_room_id`, `service_id`, `unit_id`, `amount`, `valid_from`,
`valid_to` et `created_at`.

### Requêtes et services

[`MeetingRoomRateRepository`](../src/main/java/mg/lagrace/api/catalog/meetingroom/persistence/MeetingRoomRateRepository.java)
propose trois lectures utiles :

- `findApplicableForBooking(roomId, serviceId, date)` : un tarif par unité,
  pour une salle et une formule précises;
- `findApplicableByService(serviceId, date)` : un tarif par salle et unité,
  pour alimenter les listes d'une formule;
- `findAllByOrderByValidFromDesc()` : historique global.

Les deux requêtes d'applicabilité filtrent par période inclusive et départagent
par date de fin, `valid_from` et `id_rate`.

[`MeetingRoomManagementService`](../src/main/java/mg/lagrace/api/catalog/meetingroom/application/MeetingRoomManagementService.java)
construit les réponses
[`MeetingRoomRateResponse`](../src/main/java/mg/lagrace/api/catalog/meetingroom/dto/meeting/MeetingRoomRateResponse.java).
`currentRates(serviceId, date)` parcourt les formules; `otherRates(date)` retire
de l'historique les identifiants courants.

La réservation choisit d'abord une unité `Forfait`, puis `Jour`. Si aucune
n'est présente, elle accepte le tarif seulement si une seule ligne reste.
`Forfait` et `Jour` ont une quantité de `1`; `Par personne` utilise le nombre
d'invités; les autres unités retombent actuellement à `1`.

API : `/api/meeting-rooms/rates`, `/api/meeting-rooms/rates/current` et
`/api/meeting-rooms/rates/others`. Pour
`GET /api/meeting-rooms/rates`, `serviceId` et `date` doivent être tous deux
présents ou tous deux absents.

## 6. Services additionnels

### Schéma et modèle de prix

[`additional_service`](../src/main/java/mg/lagrace/api/catalog/additionalservice/AdditionalService.java)
est différent des tables précédentes : le prix est directement sur le service,
sans historique ni dates :

| Colonne | Rôle |
|---|---|
| `id_service`, `name` | identifiant et nom |
| `scope_id` | catalogue d'application (`ROOM`, `SPACE`, `POOL`, etc.) |
| `unit_id`, `price` | unité et montant fixe |
| `is_deposit` | indique une caution |
| `catering_condition` | règle traiteur pour les services d'espace |
| `free_with_caterer` | charge incluse si l'espace a un traiteur |
| `is_active` | disponibilité à la vente |
| `created_at` | date de création |

Le DTO de gestion est
[`AdditionalServiceRequest`](../src/main/java/mg/lagrace/api/catalog/additionalservice/dto/service/AdditionalServiceRequest.java).
`ServiceConfigService` exige le périmètre, l'unité, le nom et un prix positif ou
nul; `active` vaut vrai si le champ n'est pas fourni.

### Requêtes et utilisation

Il n'existe pas de méthode `findRateByIdService` dans le code actuel.
[`AdditionalServiceRepository`](../src/main/java/mg/lagrace/api/catalog/additionalservice/persistence/AdditionalServiceRepository.java)
hérite de `findById(id)` et déclare seulement :

```jpql
SELECT service
FROM AdditionalService service
WHERE service.scope.code = 'SPACE'
  AND service.deposit = true
  AND service.active = true
```

Cette méthode s'appelle `findActiveSpaceDeposits`; elle sélectionne la
configuration de caution, pas un tarif daté. Les autres prix sont lus via
`findById` puis `AdditionalService.getPrice()`.

Les appels de réservation copient ce prix dans `BookingCharge.unit_price`.
`BookingChargeService` et `BookingExtraService` vérifient notamment que le
service est actif, qu'il appartient au bon `scope`, et qu'une caution n'est pas
ajoutée comme une charge ordinaire. Les services d'espace vérifient aussi
`catering_condition`; `free_with_caterer` marque la charge comme incluse quand
un traiteur est présent.

Pour les cautions d'espace, la configuration actuelle autorise un seul service
actif du scope `SPACE`, impose `100000 Ar`, et
`BookingPaymentService` l'ajoute au dossier au moment de l'encaissement. La
migration
[`20261109_space_booking_rules.sql`](../src/main/resources/db/migration/20261109_space_booking_rules.sql)
ajoute `free_with_caterer` et une fonction/trigger de contrôle pour cette
caution.

API de configuration : `/api/service-configs/additional-services` (liste,
création, mise à jour, suppression). Les services de banquet exposent aussi
`/api/banquets/services`; les réservations de chaque catalogue filtrent les
services additionnels par scope.

## Comment les tarifs sont consommés dans une réservation

| Réservation | Date de sélection | Clé/dimensions | Ce qui est enregistré |
|---|---|---|---|
| Chambre | date d'arrivée | type + unité `Nuit` | prix par nuit et nombre de nuits |
| Espace | date de l'événement | espace + invités + unité | prix, unité et quantité calculée |
| Banquet | date de l'événement | type de menu + personnes | référence du tarif, prix/personne et quantité |
| Piscine | date du service | `rateId` + piscine + formule | prix, quantité selon unité/formule |
| Salle de réunion | date de service | salle + formule + unités | prix et quantité selon unité |
| Service additionnel | pas de date tarifaire | service + scope | prix fixe copié sur la charge |

À la lecture ultérieure d'un dossier, il faut donc prendre le prix enregistré
sur sa ligne/charge, et non supposer que le tarif catalogue actuel est celui
facturé à l'origine.

## Différences et points à uniformiser

1. **Sélection des chevauchements.** Les requêtes `DISTINCT ON` n'ont pas toutes
   exactement la même clé ou priorité. Par exemple, la chambre sélectionne par
   unité, l'espace par unité et bornes de personnes, et la piscine ajoute âge
   et repas. Banquet exige ensuite exactement un palier dans le service, tandis
   que l'espace choisit le maximum selon un comparateur.
2. **Contrôle des périodes.** La chambre a des contrôles de début dupliqué,
   tarif permanent et promotions; espace, banquet, piscine et salle de réunion
   n'ont pas dans leurs services de contrôle équivalent des chevauchements.
   Une ligne peut donc être valide à la date tout en laissant plusieurs tarifs
   possibles, selon le catalogue.
3. **Unités textuelles.** `Unit` n'a que `name` et `service`; la logique compare
   des libellés (`Nuit`, `Forfait`, `Par personne`, `Jour`, `Heure`) et parfois
   des codes de formule (`ENTRY`). Renommer un libellé peut modifier le calcul.
4. **Dates par défaut.** Les catalogues n'utilisent pas tous la même source
   d'heure/date : `LocalDate.now()` dans plusieurs services, horloge configurée
   pour piscine et salle de réunion. Pour unifier, injecter une même `Clock` et
   un fuseau métier explicite.
5. **Prix fixe ou historique.** `additional_service.price` ne permet pas de
   programmer une évolution. Si une date d'effet est requise, il faudra une
   table de tarifs séparée plutôt que modifier le prix en place.
6. **Nommage des méthodes.** `findRateByIdService` n'est pas présent. Le dépôt
   de service additionnel utilise `findById`; les méthodes spécialisées sont
   `findApplicableBy...` pour les autres catalogues.

## Modèle conseillé pour un catalogue similaire

Pour ajouter un catalogue dont le prix varie dans le temps, reprendre le modèle
daté existant tout en normalisant les règles :

1. Créer une table tarif liée à l'élément du catalogue, à l'unité, au montant
   et à `valid_from`/`valid_to`. Ajouter les dimensions métier réellement
   nécessaires en colonnes explicites (par exemple tranche de personnes).
2. Définir la clé logique d'un tarif (élément + service/formule + unité +
   dimensions) et empêcher en base les doublons/chevauchements incompatibles.
3. Exposer dans le dépôt une méthode `findApplicable...(..., date)` qui
   applique les mêmes bornes inclusives et une priorité documentée. Ne pas
   retourner silencieusement plusieurs candidats lorsqu'un seul est requis.
4. Centraliser dans le service le défaut de date, la validation des dimensions,
   la sélection, la quantité et le total. Retourner un objet de type
   `TariffQuote` avec au minimum `rateId`, `amount`, `unit`, `quantity` et la
   période retenue.
5. À la réservation, enregistrer le tarif choisi quand c'est utile à l'audit,
   ainsi qu'une copie de `unit_price` et `quantity` pour préserver l'historique.
6. Tester les limites de dates, `valid_to = NULL`, les chevauchements, les
   paliers de personnes/âge et chaque unité de calcul.

Pour uniformiser les catalogues actuels sans imposer une table unique, le
meilleur premier pas est de partager **le contrat de sélection et les règles
de date**, tout en gardant les dimensions métier propres à chaque table.
