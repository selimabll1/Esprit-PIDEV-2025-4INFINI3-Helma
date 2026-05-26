package tn.esprit.helma.services.auth;

/**
 * Abstraction centralisée de l'utilisateur courant.
 * Permet de remplacer facilement la logique statique par JWT/session.
 */
public interface CurrentUserProvider {

    /**
     * Retourne l'identifiant de l'utilisateur courant.
     */
    Long getCurrentUserId();
}
