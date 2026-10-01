package mg.lagrace.api.models.enums;

/**
 * Condition d'applicabilité d'un service additionnel par rapport au
 * traiteur, utilisée uniquement pour le scope SPACE.
 *
 * null (pas de valeur) = le service s'applique dans tous les cas.
 * WITHOUT_CATERER      = uniquement si le client n'a pas de traiteur.
 * WITH_CATERER         = réservé pour un futur service propre au mode
 *                        "avec traiteur" (aucun cas actuellement).
 */
public enum CateringCondition {
    WITH_CATERER,
    WITHOUT_CATERER
}
