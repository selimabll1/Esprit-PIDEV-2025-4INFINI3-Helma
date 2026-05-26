package tn.esprit.helma.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.helma.entities.BankAccount;
import tn.esprit.helma.entities.User;
import tn.esprit.helma.enums.AccountStatus;
import tn.esprit.helma.enums.AccountType;
import tn.esprit.helma.repositories.BankAccountRepository;
import tn.esprit.helma.repositories.UserRepository;
import tn.esprit.helma.services.IBankAccountService;
import tn.esprit.helma.services.auth.CurrentUserProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des comptes bancaires.
 * Gère les opérations métier relatives aux comptes: création, modification, solde, statut, etc.
 */
@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class BankAccountServiceImpl implements IBankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Crée un nouveau compte bancaire avec validation
     */
    @Override
    public BankAccount createAccount(String rib, AccountType accountType, String currency) {
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("Création d'un nouveau compte pour l'utilisateur: {}", userId);

        // Charger l'entité User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        // Vérifier que le RIB n'existe pas déjà
        if (bankAccountRepository.existsByRib(rib)) {
            log.warn("Le RIB {} existe déjà", rib);
            throw new IllegalArgumentException("Ce RIB existe déjà");
        }

        // Créer le compte
        BankAccount account = BankAccount.builder()
                .user(user)
                .rib(rib)
                .balance(BigDecimal.ZERO)
                .currency(currency != null ? currency : "TND")
                .accountType(accountType)
                .status(AccountStatus.ACTIVE)
                .build();

        BankAccount savedAccount = bankAccountRepository.save(account);
        log.info("Compte créé avec succès: ID={}, RIB={}", savedAccount.getId(), savedAccount.getRib());

        return savedAccount;
    }

    @Override
    public Optional<BankAccount> getAccountById(Long accountId) {
        log.debug("Récupération du compte: {}", accountId);
        return bankAccountRepository.findById(accountId);
    }

    @Override
    public Optional<BankAccount> getAccountByRib(String rib) {
        log.debug("Recherche du compte par RIB: {}", rib);
        return bankAccountRepository.findByRib(rib);
    }

    @Override
    public List<BankAccount> getUserAccounts(Long userId) {
        log.debug("Récupération des comptes de l'utilisateur: {}", userId);
        return bankAccountRepository.findByUser_Id(userId);
    }

    @Override
    public List<BankAccount> getCurrentUserAccounts() {
        return getUserAccounts(currentUserProvider.getCurrentUserId());
    }

    @Override
    public List<BankAccount> getActiveUserAccounts(Long userId) {
        log.debug("Récupération des comptes actifs de l'utilisateur: {}", userId);
        return bankAccountRepository.findActiveAccountsByUserId(userId, AccountStatus.ACTIVE);
    }

    @Override
    public List<BankAccount> getCurrentUserActiveAccounts() {
        return getActiveUserAccounts(currentUserProvider.getCurrentUserId());
    }

    /**
     * Met à jour le solde du compte
     */
    @Override
    public BankAccount updateBalance(Long accountId, BigDecimal newBalance) {
        log.info("Mise à jour du solde du compte: {}, nouveau solde: {}", accountId, newBalance);

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte non trouvé"));

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le solde ne peut pas être négatif");
        }

        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());

        return bankAccountRepository.save(account);
    }

    /**
     * Crédite un compte (ajoute au solde)
     */
    @Override
    public BankAccount creditAccount(Long accountId, BigDecimal amount) {
        log.info("Crédit du compte: {}, montant: {}", accountId, amount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte non trouvé"));

        account.setBalance(account.getBalance().add(amount));
        account.setUpdatedAt(LocalDateTime.now());

        return bankAccountRepository.save(account);
    }

    /**
     * Débite un compte (soustrait du solde)
     */
    @Override
    public BankAccount debitAccount(Long accountId, BigDecimal amount) {
        log.info("Débit du compte: {}, montant: {}", accountId, amount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif");
        }

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte non trouvé"));

        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Solde insuffisant pour le compte: {}", accountId);
            throw new IllegalArgumentException("Solde insuffisant");
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setUpdatedAt(LocalDateTime.now());

        return bankAccountRepository.save(account);
    }

    /**
     * Change le statut du compte
     */
    @Override
    public BankAccount updateAccountStatus(Long accountId, AccountStatus newStatus) {
        log.info("Changement du statut du compte: {} -> {}", accountId, newStatus);

        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte non trouvé"));

        account.setStatus(newStatus);
        account.setUpdatedAt(LocalDateTime.now());

        return bankAccountRepository.save(account);
    }

    /**
     * Gèle un compte temporairement
     */
    @Override
    public BankAccount freezeAccount(Long accountId) {
        log.warn("Gel du compte: {}", accountId);
        return updateAccountStatus(accountId, AccountStatus.FROZEN);
    }

    /**
     * Dégoèle un compte
     */
    @Override
    public BankAccount unfreezeAccount(Long accountId) {
        log.info("Dégel du compte: {}", accountId);
        return updateAccountStatus(accountId, AccountStatus.ACTIVE);
    }

    /**
     * Ferme définitivement un compte
     */
    @Override
    public BankAccount closeAccount(Long accountId) {
        log.warn("Fermeture du compte: {}", accountId);
        return updateAccountStatus(accountId, AccountStatus.CLOSED);
    }

    @Override
    public void deleteAccount(Long accountId) {
        log.warn("Suppression du compte: {}", accountId);
        bankAccountRepository.deleteById(accountId);
    }

    @Override
    public BigDecimal getBalance(Long accountId) {
        BankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Compte non trouvé"));
        return account.getBalance();
    }

    @Override
    public boolean isAccountActive(Long accountId) {
        Optional<BankAccount> account = bankAccountRepository.findById(accountId);
        return account.isPresent() && account.get().getStatus() == AccountStatus.ACTIVE;
    }

    @Override
    public boolean isRibAvailable(String rib) {
        return !bankAccountRepository.existsByRib(rib);
    }
}
