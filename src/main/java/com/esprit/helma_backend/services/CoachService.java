package com.esprit.helma_backend.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esprit.helma_backend.dto.CoachDto;
import com.esprit.helma_backend.entities.CoachMessage;
import com.esprit.helma_backend.entities.CoachSession;
import com.esprit.helma_backend.entities.MessageRole;
import com.esprit.helma_backend.entities.User;
import com.esprit.helma_backend.repositories.CoachMessageRepository;
import com.esprit.helma_backend.repositories.CoachSessionRepository;
import com.esprit.helma_backend.repositories.UserRepository;

@Service
@Transactional
public class CoachService {

    // ─────────────────────────────────────────────────────────────────────────
    // SYSTEM PROMPT — FinCoach Helma
    // To update: change the text between the triple-quotes.
    // The {CONTEXT_SNAPSHOT} placeholder is replaced at runtime with live data.
    // ─────────────────────────────────────────────────────────────────────────
    private static final String SYSTEM_PROMPT = """
Tu es FinCoach, le conseiller financier personnel intégré dans Helma — une fintech tunisienne\s
pour jeunes entrepreneurs et jeunes actifs entre 15 et 30 ans.

═══════════════════════════════════════
TON IDENTITÉ
═══════════════════════════════════════
Tu es un expert en finance opérationnelle et gestion de trésorerie d'entreprise.
Tu connais les chiffres exacts de ton client — tu ne devines pas, tu analyses.
Tu es direct, bienveillant, et jamais condescendant.
Tu tutoies toujours ton client (ton/ta/tes — jamais vous/votre).

═══════════════════════════════════════
RÈGLES ABSOLUES
═══════════════════════════════════════
1. Tu réponds TOUJOURS en anglais
2. Tu cites TOUJOURS les chiffres exacts du contexte — jamais de généralités
3. Si une donnée est "Non disponible" → demande d'abord d'ajouter des transactions
4. Tu ne promets JAMAIS de rendements garantis
5. Tu travailles uniquement avec les données du contexte fourni

═══════════════════════════════════════
CE QUE TU CALCULES SYSTÉMATIQUEMENT
═══════════════════════════════════════
Dans chaque réponse, selon la pertinence, tu calcules et cites :

→ Taux d'épargne     = (revenus - dépenses) / revenus × 100
→ Runway             = solde cumulé / burn rate (en mois)
→ Budget adherence   = dépenses / plafond × 100
→ Capacité de remboursement mensuelle = 30% du revenu moyen
→ Objectif épargne hebdo manquant = (cible - actuel) / semaines restantes

Ne saute JAMAIS ces calculs quand les données sont disponibles.
Montre le calcul intermédiaire si c'est pédagogique.

═══════════════════════════════════════
COMPORTEMENT SELON LE STATUT RUNWAY
═══════════════════════════════════════

🔴 CRITIQUE (< 2 mois) :
→ Commence par : "ALERTE TRÉSORERIE — ton runway est de X mois."
→ Calcule combien réduire le burn rate pour atteindre 3 mois de runway
→ Liste 3 dépenses à couper immédiatement avec les montants exacts
→ Propose le micro-prêt Helma si badge ≥ TRUSTED

🟡 ATTENTION (2-4 mois) :
→ Signale le risque avec le chiffre exact
→ Propose un plan de réduction de 15-20% du burn rate
→ Calcule l'impact concret sur le runway (+X mois si économie de Y TND)

🟢 SAIN (≥ 4 mois) :
→ Félicite brièvement
→ Suggère d'optimiser (savings goals, investissement, badge upgrade)
→ Calcule combien mettre de côté pour atteindre 6 mois de runway

═══════════════════════════════════════
COMPORTEMENT SELON LE TRUST BADGE
═══════════════════════════════════════

UNVERIFIED → explique comment débloquer BUILDING (1 mois de transactions)
BUILDING   → donne un plan chiffré pour atteindre TRUSTED en 3 mois :
             - 0 RiskCase pendant 3 mois
             - Runway ≥ 3 mois
             - Budget respecté chaque mois
TRUSTED    → rappelle la capacité de micro-prêt disponible (montant exact)
ELITE      → encourage proactivement le crowdfunding Helma

═══════════════════════════════════════
FORMAT DE RÉPONSE
═══════════════════════════════════════

Question simple     → 2-4 phrases max, 1-2 chiffres clés
Analyse demandée    → Structure : 📊 Situation | ⚠️ Points d'attention | ✅ Actions
Plan détaillé       → Étapes numérotées avec délais et montants
Rapport complet     → Toutes les sections, max 500 mots

Termine TOUJOURS par une question de suivi pertinente pour approfondir.
Exemple : "Veux-tu que je te calcule combien économiser par semaine pour atteindre cet objectif ?"

═══════════════════════════════════════
CONTEXTE FINANCIER EN TEMPS RÉEL
═══════════════════════════════════════
{CONTEXT_SNAPSHOT}
""";

    private final CoachSessionRepository coachSessionRepo;
    private final CoachMessageRepository coachMessageRepo;
    private final ContextSnapshotBuilder contextSnapshotBuilder;
    private final GroqApiClient          groqApiClient;
    private final UserRepository         userRepository;

    public CoachService(CoachSessionRepository coachSessionRepo,
                        CoachMessageRepository coachMessageRepo,
                        ContextSnapshotBuilder contextSnapshotBuilder,
                        GroqApiClient groqApiClient,
                        UserRepository userRepository) {
        this.coachSessionRepo       = coachSessionRepo;
        this.coachMessageRepo       = coachMessageRepo;
        this.contextSnapshotBuilder = contextSnapshotBuilder;
        this.groqApiClient          = groqApiClient;
        this.userRepository         = userRepository;
    }

    public CoachDto.Response sendMessage(Long userId, String userMessage) {
        if (userMessage == null || userMessage.isBlank())
            throw new IllegalArgumentException("Message vide");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Active session = less than 24h old
        Instant activeSince = Instant.now().minus(24, ChronoUnit.HOURS);
        CoachSession session = coachSessionRepo
                .findByUserIdAndLastActivityAtAfter(userId, activeSince)
                .orElseGet(() -> coachSessionRepo.save(
                        CoachSession.builder().user(user).build()));


        String contextSnapshot = contextSnapshotBuilder.build(userId);


        List<CoachMessage> fullHistory = coachMessageRepo.findBySessionOrderByCreatedAtAsc(session);
        List<CoachMessage> history = fullHistory.size() <= 10
                ? fullHistory
                : fullHistory.subList(fullHistory.size() - 10, fullHistory.size());


        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role",    "system",
                "content", SYSTEM_PROMPT.replace("{CONTEXT_SNAPSHOT}", contextSnapshot)
        ));
        for (CoachMessage msg : history) {
            messages.add(Map.of("role", mapRole(msg.getRole()), "content", msg.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        String aiResponse = groqApiClient.chat(messages);


        coachMessageRepo.save(CoachMessage.builder()
                .session(session).role(MessageRole.USER).content(userMessage).build());
        CoachMessage saved = coachMessageRepo.save(CoachMessage.builder()
                .session(session).role(MessageRole.ASSISTANT).content(aiResponse).build());

        session.setLastActivityAt(Instant.now());
        coachSessionRepo.save(session);

        return new CoachDto.Response(saved.getId(), session.getId(), aiResponse, saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<CoachDto.MessageResponse> getHistory(Long userId) {
        Instant activeSince = Instant.now().minus(24, ChronoUnit.HOURS);
        return coachSessionRepo
                .findByUserIdAndLastActivityAtAfter(userId, activeSince)
                .map(session -> coachMessageRepo
                        .findBySessionOrderByCreatedAtAsc(session)
                        .stream().map(this::toMessageResponse).toList())
                .orElseGet(List::of);
    }

    private CoachDto.MessageResponse toMessageResponse(CoachMessage m) {
        return new CoachDto.MessageResponse(m.getId(), mapRole(m.getRole()), m.getContent(), m.getCreatedAt());
    }

    private String mapRole(MessageRole role) {
        if (role == null) return "user";
        return switch (role) {
            case USER      -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM    -> "system";
        };
    }
}