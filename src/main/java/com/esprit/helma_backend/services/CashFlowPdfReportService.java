package com.esprit.helma_backend.services;

import com.esprit.helma_backend.dto.*;
import com.lowagie.text.*;
import com.lowagie.text.List;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

@Service
public class CashFlowPdfReportService {

    private static final Color BRAND_DARK = new Color(8, 43, 44);
    private static final Color BRAND_MINT = new Color(42, 157, 143);
    private static final Color BRAND_GOLD = new Color(233, 196, 96);
    private static final Color BRAND_SOFT = new Color(240, 244, 242);
    private static final Color POSITIVE = new Color(25, 130, 106);
    private static final Color NEGATIVE = new Color(200, 107, 93);
    private static final Color MUTED = new Color(100, 120, 120);
    private static final Color BORDER = new Color(220, 231, 229);
    private static final Color HEADER_BG = new Color(9, 55, 56);

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRANCE);
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRANCE);

    public byte[] generate(String userName,
                           LocalDate referenceMonth,
                           java.util.List<CashFlowDto.Response> history,
                           ForecastDto.Response forecast,
                           BurnRateDto.Response burnRate,
                           HealthScoreDto.Response health,
                           TrustBadgeDto.Response badge,
                           java.util.List<BudgetDto.Response> monthBudgets,
                           java.util.List<SavingsGoalDto.Response> goals,
                           java.util.List<RiskCaseDto.Response> openRisks) {
        try {
            java.util.List<CashFlowDto.Response> orderedHistory = history == null
                    ? java.util.List.of()
                    : history.stream()
                    .sorted(Comparator.comparing(CashFlowDto.Response::monthStart))
                    .toList();

            CashFlowDto.Response currentMonth = orderedHistory.isEmpty()
                    ? null
                    : orderedHistory.get(orderedHistory.size() - 1);

            CashFlowDto.Response previousMonth = orderedHistory.size() >= 2
                    ? orderedHistory.get(orderedHistory.size() - 2)
                    : null;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 34, 34, 46, 42);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new HeaderFooterEvent(userName, referenceMonth));

            document.open();

            addCover(document, userName, referenceMonth, currentMonth, forecast, badge, health);
            addSectionTitle(document, "1. Résumé du mois");
            addExecutiveSummary(document, referenceMonth, currentMonth, previousMonth, burnRate, health, badge);

            addSectionTitle(document, "2. Lecture simple de ta situation");
            addSimpleReading(document, currentMonth, previousMonth, burnRate, health, badge, openRisks);

            addSectionTitle(document, "3. Historique & comparaison");
            addHistoryTable(document, orderedHistory, currentMonth, previousMonth);

            addSectionTitle(document, "4. Budget, épargne et discipline financière");
            addBudgetAndSavingsSection(document, monthBudgets, currentMonth, goals);

            addSectionTitle(document, "5. Prévision sur les 3 prochains mois");
            addForecastSection(document, forecast, burnRate);

            addSectionTitle(document, "6. Packs recommandés HELMA");
            addProductRecommendations(document, monthBudgets, burnRate, health, badge, openRisks);

            addSectionTitle(document, "7. Plan d'action concret pour le mois prochain");
            addActionPlan(document, currentMonth, burnRate, health, monthBudgets, goals, openRisks);

            addSectionTitle(document, "8. Méthodologie");
            addMethodology(document, forecast);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PDF report", e);
        }
    }

    private void addCover(Document document,
                          String userName,
                          LocalDate referenceMonth,
                          CashFlowDto.Response currentMonth,
                          ForecastDto.Response forecast,
                          TrustBadgeDto.Response badge,
                          HealthScoreDto.Response health) throws DocumentException {

        PdfPTable hero = new PdfPTable(1);
        hero.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBackgroundColor(BRAND_DARK);
        cell.setPadding(28f);

        Paragraph k = new Paragraph("HELMA", font(12, Font.BOLD, Color.WHITE));
        k.setSpacingAfter(8f);
        cell.addElement(k);

        Paragraph title = new Paragraph("Monthly Entrepreneur Brief", font(24, Font.BOLD, Color.WHITE));
        title.setLeading(30f);
        cell.addElement(title);

        Paragraph subtitle = new Paragraph(
                "Analyse mensuelle claire, utile et orientée décision pour jeunes entrepreneurs.",
                font(11, Font.NORMAL, new Color(228, 238, 236))
        );
        subtitle.setSpacingBefore(10f);
        subtitle.setLeading(17f);
        cell.addElement(subtitle);

        Paragraph meta = new Paragraph(
                "Profil : " + safe(userName, "Utilisateur")
                        + "   •   Mois analysé : " + (referenceMonth != null ? referenceMonth.format(MONTH_FMT) : "—")
                        + "   •   Généré le : " + LocalDate.now().format(DAY_FMT),
                font(10, Font.NORMAL, new Color(224, 234, 233))
        );
        meta.setSpacingBefore(14f);
        cell.addElement(meta);

        hero.addCell(cell);
        document.add(hero);
        document.add(Chunk.NEWLINE);

        PdfPTable chips = new PdfPTable(3);
        chips.setWidthPercentage(100);
        chips.setSpacingAfter(12f);
        chips.setWidths(new float[]{1f, 1f, 1f});

        chips.addCell(chipCell(
                "Trust badge",
                badge != null && badge.level() != null ? badge.level().name() : "N/A",
                BRAND_GOLD
        ));
        chips.addCell(chipCell(
                "Health score",
                health != null && health.score() != null ? health.score().setScale(0, RoundingMode.HALF_UP).toPlainString() + "/100" : "N/A",
                BRAND_MINT
        ));
        chips.addCell(chipCell(
                "Net du mois",
                currentMonth != null ? signedMoney(currentMonth.netFlow()) : "N/A",
                nz(currentMonth != null ? currentMonth.netFlow() : null).signum() >= 0 ? BRAND_MINT : NEGATIVE
        ));

        document.add(chips);
    }

    private void addExecutiveSummary(Document document,
                                     LocalDate referenceMonth,
                                     CashFlowDto.Response currentMonth,
                                     CashFlowDto.Response previousMonth,
                                     BurnRateDto.Response burnRate,
                                     HealthScoreDto.Response health,
                                     TrustBadgeDto.Response badge) throws DocumentException {

        PdfPTable grid = new PdfPTable(4);
        grid.setWidthPercentage(100);
        grid.setSpacingAfter(14f);
        grid.setWidths(new float[]{1f, 1f, 1f, 1f});

        grid.addCell(kpiCell("Revenus du mois", money(currentMonth != null ? currentMonth.totalIncome() : null), POSITIVE));
        grid.addCell(kpiCell("Dépenses du mois", money(currentMonth != null ? currentMonth.totalExpense() : null), NEGATIVE));
        grid.addCell(kpiCell("Flux net", signedMoney(currentMonth != null ? currentMonth.netFlow() : null),
                nz(currentMonth != null ? currentMonth.netFlow() : null).signum() >= 0 ? POSITIVE : NEGATIVE));
        grid.addCell(kpiCell("Solde cumulé", money(currentMonth != null ? currentMonth.cumulativeBalance() : null),
                nz(currentMonth != null ? currentMonth.cumulativeBalance() : null).signum() >= 0 ? POSITIVE : NEGATIVE));
        document.add(grid);

        java.util.List<String> bullets = new ArrayList<>();

        if (currentMonth == null) {
            bullets.add("Aucune donnée mensuelle disponible pour construire un vrai résumé.");
        } else {
            BigDecimal net = nz(currentMonth.netFlow());
            if (net.signum() >= 0) {
                bullets.add("Sur " + referenceMonth.format(MONTH_FMT) + ", ton activité a généré plus d'argent qu'elle n'en a dépensé.");
            } else {
                bullets.add("Sur " + referenceMonth.format(MONTH_FMT) + ", les dépenses ont dépassé les revenus.");
            }

            if (previousMonth != null) {
                BigDecimal deltaNet = nz(currentMonth.netFlow()).subtract(nz(previousMonth.netFlow()));
                if (deltaNet.signum() > 0) {
                    bullets.add("Le résultat mensuel s'améliore par rapport au mois précédent de " + money(deltaNet.abs()) + ".");
                } else if (deltaNet.signum() < 0) {
                    bullets.add("Le résultat mensuel recule par rapport au mois précédent de " + money(deltaNet.abs()) + ".");
                } else {
                    bullets.add("Le résultat mensuel reste proche du mois précédent.");
                }
            }
        }

        if (burnRate != null && burnRate.runwayMonths() != null) {
            bullets.add("Ton runway est estimé à " + burnRate.runwayMonths().setScale(1, RoundingMode.HALF_UP).toPlainString()
                    + " mois, ce qui correspond à un niveau " + safeEnum(burnRate.status()) + ".");
        }

        if (health != null && health.score() != null) {
            bullets.add("Ton health score est de " + health.score().setScale(0, RoundingMode.HALF_UP).toPlainString()
                    + "/100, avec une lecture globale " + safe(health.label(), "N/A") + ".");
        }

        if (badge != null && badge.level() != null) {
            bullets.add("Ton niveau de confiance actuel est " + badge.level().name() + ".");
        }

        addBulletParagraphs(document, bullets);
    }

    private void addSimpleReading(Document document,
                                  CashFlowDto.Response currentMonth,
                                  CashFlowDto.Response previousMonth,
                                  BurnRateDto.Response burnRate,
                                  HealthScoreDto.Response health,
                                  TrustBadgeDto.Response badge,
                                  java.util.List<RiskCaseDto.Response> openRisks) throws DocumentException {

        java.util.List<String> points = new ArrayList<>();

        if (currentMonth != null) {
            BigDecimal income = nz(currentMonth.totalIncome());
            BigDecimal expense = nz(currentMonth.totalExpense());

            if (income.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal expenseRatio = expense.divide(income, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                points.add("Sur 100 TND encaissés ce mois-ci, environ "
                        + expenseRatio.setScale(0, RoundingMode.HALF_UP).toPlainString()
                        + " TND sont repartis en dépenses.");
            }

            if (nz(currentMonth.cumulativeBalance()).compareTo(BigDecimal.ZERO) > 0) {
                points.add("Tu termines le mois avec un coussin de trésorerie positif, ce qui te donne une marge de sécurité.");
            } else {
                points.add("Tu termines le mois avec une trésorerie fragile ou négative : c'est la priorité à corriger.");
            }
        }

        if (burnRate != null) {
            if (burnRate.runwayMonths() == null) {
                points.add("Le runway reste difficile à lire faute d'historique complet.");
            } else if (burnRate.runwayMonths().compareTo(new BigDecimal("4")) >= 0) {
                points.add("Le runway est confortable : en gardant ce rythme, ton activité peut absorber plusieurs mois de dépenses.");
            } else if (burnRate.runwayMonths().compareTo(new BigDecimal("2")) >= 0) {
                points.add("Le runway est moyen : la situation n'est pas critique, mais elle doit être surveillée.");
            } else {
                points.add("Le runway est court : une baisse de revenus ou une dépense forte peut vite créer une tension.");
            }
        }

        if (health != null && health.highlights() != null) {
            for (String h : health.highlights()) {
                if (h != null && !h.isBlank()) {
                    points.add("Indicateur clé : " + h);
                }
            }
        }

        if (openRisks != null && !openRisks.isEmpty()) {
            points.add("Tu as " + openRisks.size() + " risk case(s) ouvert(s), ce qui pèse sur la confiance et les recommandations de financement.");
        } else {
            points.add("Aucun risk case ouvert : c'est un très bon signal pour la stabilité du profil.");
        }

        if (badge != null && badge.level() != null) {
            switch (badge.level()) {
                case ELITE -> points.add("Ton badge ELITE signifie que ton profil est très rassurant pour des partenaires ou financeurs.");
                case TRUSTED -> points.add("Ton badge TRUSTED signifie que ton activité montre déjà des signaux solides de discipline financière.");
                case BUILDING -> points.add("Ton badge BUILDING signifie que ta base est correcte mais qu'il faut encore renforcer la régularité.");
                case UNVERIFIED -> points.add("Ton badge UNVERIFIED signifie que l'historique ou les signaux de confiance restent encore trop limités.");
            }
        }

        addBulletParagraphs(document, points);
    }

    private void addHistoryTable(Document document,
                                 java.util.List<CashFlowDto.Response> history,
                                 CashFlowDto.Response currentMonth,
                                 CashFlowDto.Response previousMonth) throws DocumentException {

        if (history == null || history.isEmpty()) {
            document.add(new Paragraph("Pas d'historique disponible.", font(11, Font.NORMAL, BRAND_DARK)));
            document.add(Chunk.NEWLINE);
            return;
        }

        if (previousMonth != null && currentMonth != null) {
            Paragraph comparison = new Paragraph(
                    "Comparaison simple : revenus "
                            + signedMoney(nz(currentMonth.totalIncome()).subtract(nz(previousMonth.totalIncome())))
                            + " vs mois précédent, dépenses "
                            + signedMoney(nz(currentMonth.totalExpense()).subtract(nz(previousMonth.totalExpense())))
                            + ", net "
                            + signedMoney(nz(currentMonth.netFlow()).subtract(nz(previousMonth.netFlow()))) + ".",
                    font(10, Font.NORMAL, BRAND_DARK)
            );
            comparison.setSpacingAfter(10f);
            comparison.setLeading(16f);
            document.add(comparison);
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12f);
        table.setWidths(new float[]{1.25f, 1.05f, 1.05f, 1.05f, 1.15f});

        addHeader(table, "Mois");
        addHeader(table, "Revenus");
        addHeader(table, "Dépenses");
        addHeader(table, "Net");
        addHeader(table, "Solde");

        for (CashFlowDto.Response row : history) {
            addCell(table, row.monthStart() != null ? row.monthStart().format(MONTH_FMT) : "—");
            addCell(table, money(row.totalIncome()), POSITIVE);
            addCell(table, money(row.totalExpense()), NEGATIVE);
            addCell(table, signedMoney(row.netFlow()), nz(row.netFlow()).signum() >= 0 ? POSITIVE : NEGATIVE);
            addCell(table, money(row.cumulativeBalance()), nz(row.cumulativeBalance()).signum() >= 0 ? POSITIVE : NEGATIVE);
        }

        document.add(table);
    }

    private void addBudgetAndSavingsSection(Document document,
                                            java.util.List<BudgetDto.Response> monthBudgets,
                                            CashFlowDto.Response currentMonth,
                                            java.util.List<SavingsGoalDto.Response> goals) throws DocumentException {

        java.util.List<String> bullets = new ArrayList<>();

        BigDecimal totalBudget = monthBudgets == null ? BigDecimal.ZERO : monthBudgets.stream()
                .map(BudgetDto.Response::limitAmount)
                .map(this::nz)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = nz(currentMonth != null ? currentMonth.totalExpense() : null);

        if (monthBudgets == null || monthBudgets.isEmpty()) {
            bullets.add("Aucun budget n'a été défini pour le mois de référence. Sans plafond, il est plus difficile de piloter les dépenses.");
        } else {
            bullets.add("Le budget total du mois est de " + money(totalBudget) + ".");
            bullets.add("Les dépenses du mois sont de " + money(totalSpent) + ".");

            if (totalBudget.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal usage = totalSpent.divide(totalBudget, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                bullets.add("Tu as utilisé environ " + usage.setScale(0, RoundingMode.HALF_UP).toPlainString() + "% du budget déclaré.");
            }
        }

        if (goals == null || goals.isEmpty()) {
            bullets.add("Aucun objectif d'épargne n'est enregistré. Pour un jeune entrepreneur, même un petit coussin d'épargne est utile.");
        } else {
            long completed = goals.stream().filter(g -> Boolean.TRUE.equals(g.completed())).count();
            bullets.add("Tu as " + goals.size() + " objectif(s) d'épargne, dont " + completed + " déjà atteint(s).");
        }

        addBulletParagraphs(document, bullets);

        if (monthBudgets != null && !monthBudgets.isEmpty()) {
            PdfPTable budgetTable = new PdfPTable(3);
            budgetTable.setWidthPercentage(100);
            budgetTable.setSpacingBefore(8f);
            budgetTable.setSpacingAfter(12f);
            budgetTable.setWidths(new float[]{1.4f, 1f, 1f});

            addHeader(budgetTable, "Catégorie");
            addHeader(budgetTable, "Plafond");
            addHeader(budgetTable, "Mois");

            for (BudgetDto.Response budget : monthBudgets) {
                addCell(budgetTable, safe(budget.category(), "GLOBAL"));
                addCell(budgetTable, money(budget.limitAmount()));
                addCell(budgetTable, budget.monthStart() != null ? budget.monthStart().format(MONTH_FMT) : "—");
            }

            document.add(budgetTable);
        }

        if (goals != null && !goals.isEmpty()) {
            PdfPTable goalTable = new PdfPTable(4);
            goalTable.setWidthPercentage(100);
            goalTable.setSpacingBefore(6f);
            goalTable.setSpacingAfter(6f);
            goalTable.setWidths(new float[]{1.5f, 1.1f, 1.1f, 1.1f});

            addHeader(goalTable, "Objectif");
            addHeader(goalTable, "Progression");
            addHeader(goalTable, "Hebdo");
            addHeader(goalTable, "Statut");

            for (SavingsGoalDto.Response goal : goals.stream().limit(4).toList()) {
                addCell(goalTable, safe(goal.name(), "Objectif"));
                addCell(goalTable, money(goal.currentAmount()) + " / " + money(goal.targetAmount()));
                addCell(goalTable, money(goal.weeklyTarget()));
                addCell(goalTable, Boolean.TRUE.equals(goal.completed()) ? "Atteint" : "En cours",
                        Boolean.TRUE.equals(goal.completed()) ? POSITIVE : BRAND_DARK);
            }

            document.add(goalTable);
        }
    }

    private void addForecastSection(Document document,
                                    ForecastDto.Response forecast,
                                    BurnRateDto.Response burnRate) throws DocumentException {

        if (forecast == null || forecast.months() == null || forecast.months().isEmpty()) {
            document.add(new Paragraph("Prévision indisponible.", font(11, Font.NORMAL, BRAND_DARK)));
            document.add(Chunk.NEWLINE);
            return;
        }

        java.util.List<String> bullets = new ArrayList<>();
        bullets.add("Méthode utilisée : " + safe(forecast.forecastMethod(), "NO_DATA") + ".");
        bullets.add("Niveau de confiance : " + safe(forecast.confidenceLevel(), "LOW") + ".");
        bullets.add("Qualité de l'historique : " + safe(forecast.historyQuality(), "LIMITED") + ".");
        bullets.add("Tendance revenus : " + safe(forecast.incomeTrend(), "UNKNOWN") + ".");
        bullets.add("Tendance dépenses : " + safe(forecast.expenseTrend(), "UNKNOWN") + ".");

        if (forecast.avgPredictedNetFlow() != null) {
            if (forecast.avgPredictedNetFlow().signum() >= 0) {
                bullets.add("La prévision moyenne reste positive sur les 3 prochains mois.");
            } else {
                bullets.add("La prévision moyenne est négative : il faut resserrer la gestion à court terme.");
            }
        }

        if (forecast.projectedCashoutDate() != null) {
            bullets.add("Attention : rupture de trésorerie projetée autour du " + forecast.projectedCashoutDate().format(DAY_FMT) + ".");
        }

        if (burnRate != null && burnRate.finCoachTips() != null) {
            for (String tip : burnRate.finCoachTips().stream().limit(2).toList()) {
                bullets.add("Conseil FinCoach : " + tip);
            }
        }

        addBulletParagraphs(document, bullets);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(12f);
        table.setWidths(new float[]{1.15f, 1.05f, 1.05f, 1.05f, 1.15f});

        addHeader(table, "Mois");
        addHeader(table, "Revenus");
        addHeader(table, "Dépenses");
        addHeader(table, "Net");
        addHeader(table, "Solde prévu");

        for (ForecastDto.ForecastMonth m : forecast.months()) {
            addCell(table, m.monthStart() != null ? m.monthStart().format(MONTH_FMT) : "—");
            addCell(table, money(m.predictedIncome()), POSITIVE);
            addCell(table, money(m.predictedExpense()), NEGATIVE);
            addCell(table, signedMoney(m.predictedNetFlow()),
                    nz(m.predictedNetFlow()).signum() >= 0 ? POSITIVE : NEGATIVE);
            addCell(table, money(m.predictedBalance()),
                    nz(m.predictedBalance()).signum() >= 0 ? POSITIVE : NEGATIVE);
        }

        document.add(table);
    }

    private void addProductRecommendations(Document document,
                                           java.util.List<BudgetDto.Response> monthBudgets,
                                           BurnRateDto.Response burnRate,
                                           HealthScoreDto.Response health,
                                           TrustBadgeDto.Response badge,
                                           java.util.List<RiskCaseDto.Response> openRisks) throws DocumentException {

        java.util.List<ProductPack> packs = buildProductPacks(monthBudgets, burnRate, health, badge, openRisks);

        Paragraph intro = new Paragraph(
                "Ces packs sont des propositions de démonstration HELMA. Ils aident à illustrer quel type de produit pourrait convenir selon le profil du mois.",
                font(10, Font.NORMAL, BRAND_DARK)
        );
        intro.setLeading(16f);
        intro.setSpacingAfter(10f);
        document.add(intro);

        for (ProductPack pack : packs) {
            PdfPTable box = new PdfPTable(1);
            box.setWidthPercentage(100);
            box.setSpacingAfter(10f);

            PdfPCell cell = new PdfPCell();
            cell.setPadding(14f);
            cell.setBackgroundColor(BRAND_SOFT);
            cell.setBorderColor(BORDER);

            Paragraph p1 = new Paragraph(pack.type + " · " + pack.name, font(13, Font.BOLD, BRAND_DARK));
            p1.setSpacingAfter(6f);
            cell.addElement(p1);

            Paragraph p2 = new Paragraph(pack.description, font(10, Font.NORMAL, BRAND_DARK));
            p2.setLeading(16f);
            p2.setSpacingAfter(6f);
            cell.addElement(p2);

            Paragraph p3 = new Paragraph("Pourquoi ce pack : " + pack.why, font(10, Font.NORMAL, MUTED));
            p3.setLeading(15f);
            p3.setSpacingAfter(6f);
            cell.addElement(p3);

            Paragraph p4 = new Paragraph("Lien démo : " + pack.fakeLink, font(9, Font.UNDERLINE, BRAND_MINT));
            cell.addElement(p4);

            box.addCell(cell);
            document.add(box);
        }
    }

    private void addActionPlan(Document document,
                               CashFlowDto.Response currentMonth,
                               BurnRateDto.Response burnRate,
                               HealthScoreDto.Response health,
                               java.util.List<BudgetDto.Response> monthBudgets,
                               java.util.List<SavingsGoalDto.Response> goals,
                               java.util.List<RiskCaseDto.Response> openRisks) throws DocumentException {

        List bullets = new List(List.UNORDERED);
        bullets.setIndentationLeft(16f);

        if (currentMonth != null && nz(currentMonth.netFlow()).signum() < 0) {
            bullets.add(new ListItem("Réduis immédiatement 1 à 2 dépenses non essentielles le mois prochain.", font(10, Font.NORMAL, BRAND_DARK)));
        } else {
            bullets.add(new ListItem("Protège le flux net positif en gardant la discipline sur les dépenses.", font(10, Font.NORMAL, BRAND_DARK)));
        }

        if (monthBudgets == null || monthBudgets.isEmpty()) {
            bullets.add(new ListItem("Définis au moins 2 budgets simples : fonctionnement et développement.", font(10, Font.NORMAL, BRAND_DARK)));
        } else {
            bullets.add(new ListItem("Revois les catégories budget les plus sensibles avant le début du prochain mois.", font(10, Font.NORMAL, BRAND_DARK)));
        }

        if (goals == null || goals.isEmpty()) {
            bullets.add(new ListItem("Crée un objectif d'épargne de sécurité, même petit, pour bâtir un coussin de trésorerie.", font(10, Font.NORMAL, BRAND_DARK)));
        } else {
            bullets.add(new ListItem("Continue les versements réguliers sur tes objectifs d'épargne actifs.", font(10, Font.NORMAL, BRAND_DARK)));
        }

        if (burnRate != null && burnRate.runwayMonths() != null && burnRate.runwayMonths().compareTo(new BigDecimal("3")) < 0) {
            bullets.add(new ListItem("Priorité : allonger le runway à plus de 3 mois avant toute dépense ambitieuse.", font(10, Font.NORMAL, BRAND_DARK)));
        }

        if (openRisks != null && !openRisks.isEmpty()) {
            bullets.add(new ListItem("Ferme ou traite les risk cases ouverts pour améliorer la confiance du profil.", font(10, Font.NORMAL, BRAND_DARK)));
        }

        if (health != null && health.score() != null && health.score().compareTo(new BigDecimal("70")) < 0) {
            bullets.add(new ListItem("Objectif du prochain cycle : pousser le health score au-dessus de 70 grâce à un meilleur budget, plus de stabilité et moins de risques.", font(10, Font.NORMAL, BRAND_DARK)));
        }

        document.add(bullets);
    }

    private void addMethodology(Document document, ForecastDto.Response forecast) throws DocumentException {
        Paragraph p = new Paragraph(
                "Ce document est construit automatiquement à partir des cash flows mensuels, prévisions, budgets, objectifs d'épargne, risk cases et scores HELMA. "
                        + "Les chiffres de prévision restent indicatifs et servent d'aide à la décision. "
                        + "Méthode de forecast : " + safe(forecast != null ? forecast.forecastMethod() : null, "NO_DATA") + ".",
                font(10, Font.NORMAL, BRAND_DARK)
        );
        p.setLeading(16f);
        document.add(p);
    }

    private java.util.List<ProductPack> buildProductPacks(java.util.List<BudgetDto.Response> monthBudgets,
                                                          BurnRateDto.Response burnRate,
                                                          HealthScoreDto.Response health,
                                                          TrustBadgeDto.Response badge,
                                                          java.util.List<RiskCaseDto.Response> openRisks) {
        java.util.List<ProductPack> packs = new ArrayList<>();

        boolean riskLight = openRisks == null || openRisks.isEmpty();
        BigDecimal runway = burnRate != null ? burnRate.runwayMonths() : null;
        BigDecimal healthScore = health != null ? health.score() : null;
        String badgeLevel = badge != null && badge.level() != null ? badge.level().name() : "UNVERIFIED";

        java.util.List<String> categories = monthBudgets == null
                ? java.util.List.of()
                : monthBudgets.stream()
                .map(BudgetDto.Response::category)
                .filter(c -> c != null && !c.isBlank())
                .map(String::toUpperCase)
                .toList();

        boolean leaseFit = containsAny(categories, "SOFTWARE", "EQUIPMENT", "LAPTOP", "COMPUTER", "TOOLS", "MACHINE", "LOCAL", "RENT");
        boolean loanFit = containsAny(categories, "MARKETING", "STOCK", "INVENTORY", "TRANSPORT", "PAYROLL", "CAMPAIGN", "RAW", "FOOD", "PROJECT X");

        if (leaseFit) {
            packs.add(new ProductPack(
                    "MICRO-LEASE",
                    "Smart Equip Flex",
                    "Pack conçu pour financer un équipement, un outil digital, un laptop ou un besoin matériel léger avec paiement progressif.",
                    "Tes budgets montrent un besoin orienté matériel / outil / exploitation. C'est typiquement un bon cas d'usage micro-lease.",
                    "https://helma-demo.local/packs/smart-equip-flex"
            ));
        }

        if (loanFit) {
            packs.add(new ProductPack(
                    "MICRO-LOAN",
                    "Boost Cash Starter",
                    "Pack court terme pensé pour stock, marketing, roulement ou lancement d'une petite opération commerciale.",
                    "Tes catégories budgétaires montrent un besoin de cash opérationnel plus qu'un besoin d'actif à louer.",
                    "https://helma-demo.local/packs/boost-cash-starter"
            ));
        }

        if ((runway != null && runway.compareTo(new BigDecimal("3")) < 0) || !riskLight || (healthScore != null && healthScore.compareTo(new BigDecimal("70")) < 0)) {
            packs.add(new ProductPack(
                    "ÉPARGNE",
                    "Safe Buffer Mini",
                    "Pack d'épargne progressif pour construire un mini coussin de sécurité avant d'augmenter l'endettement ou les charges fixes.",
                    "Le profil du mois montre qu'il faut d'abord consolider la sécurité financière avant de pousser plus loin le financement.",
                    "https://helma-demo.local/packs/safe-buffer-mini"
            ));
        }

        if ("TRUSTED".equals(badgeLevel) || "ELITE".equals(badgeLevel)) {
            packs.add(new ProductPack(
                    "MICRO-LOAN",
                    "Growth Sprint 3M",
                    "Pack de démonstration pour soutenir une montée en charge sur 3 mois quand le profil est déjà assez rassurant.",
                    "Le badge et les indicateurs du profil montrent une capacité à envisager un financement de croissance plus sereinement.",
                    "https://helma-demo.local/packs/growth-sprint-3m"
            ));
        }

        if (packs.isEmpty()) {
            packs.add(new ProductPack(
                    "ÉPARGNE",
                    "Starter Discipline Pack",
                    "Pack d'épargne de départ pour structurer le comportement financier avant d'ouvrir l'accès à d'autres produits.",
                    "Aucun besoin produit très net ne ressort encore des catégories ou du profil du mois.",
                    "https://helma-demo.local/packs/starter-discipline-pack"
            ));
        }

        return packs.stream().limit(4).toList();
    }

    private boolean containsAny(java.util.List<String> categories, String... patterns) {
        for (String category : categories) {
            for (String pattern : patterns) {
                if (category.contains(pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, font(15, Font.BOLD, BRAND_DARK));
        p.setSpacingBefore(10f);
        p.setSpacingAfter(10f);
        document.add(p);
    }

    private void addBulletParagraphs(Document document, java.util.List<String> bullets) throws DocumentException {
        List list = new List(List.UNORDERED);
        list.setIndentationLeft(16f);

        for (String bullet : bullets) {
            if (bullet != null && !bullet.isBlank()) {
                list.add(new ListItem(bullet, font(10, Font.NORMAL, BRAND_DARK)));
            }
        }

        document.add(list);
        document.add(Chunk.NEWLINE);
    }

    private PdfPCell chipCell(String label, String value, Color accent) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12f);
        cell.setBackgroundColor(Color.WHITE);
        cell.setBorderColor(BORDER);

        Paragraph l = new Paragraph(label, font(9, Font.NORMAL, MUTED));
        Paragraph v = new Paragraph(value, font(13, Font.BOLD, accent));
        v.setSpacingBefore(6f);

        cell.addElement(l);
        cell.addElement(v);
        return cell;
    }

    private PdfPCell kpiCell(String label, String value, Color valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12f);
        cell.setBackgroundColor(Color.WHITE);
        cell.setBorderColor(BORDER);

        Paragraph l = new Paragraph(label, font(9, Font.NORMAL, MUTED));
        Paragraph v = new Paragraph(value, font(13, Font.BOLD, valueColor));
        v.setSpacingBefore(6f);

        cell.addElement(l);
        cell.addElement(v);
        return cell;
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(8f);
        cell.setBorderColor(HEADER_BG);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text) {
        addCell(table, text, BRAND_DARK);
    }

    private void addCell(PdfPTable table, String text, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9, Font.NORMAL, color)));
        cell.setPadding(7f);
        cell.setBorderColor(BORDER);
        table.addCell(cell);
    }

    private Font font(float size, int style, Color color) {
        return FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
    }

    private String money(BigDecimal value) {
        return nz(value).setScale(0, RoundingMode.HALF_UP).toPlainString() + " TND";
    }

    private String signedMoney(BigDecimal value) {
        BigDecimal v = nz(value).setScale(0, RoundingMode.HALF_UP);
        return (v.signum() >= 0 ? "+" : "") + v.toPlainString() + " TND";
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String safeEnum(Enum<?> value) {
        return value == null ? "N/A" : value.name();
    }

    private record ProductPack(
            String type,
            String name,
            String description,
            String why,
            String fakeLink
    ) {}

    private static class HeaderFooterEvent extends PdfPageEventHelper {
        private final String userName;
        private final LocalDate referenceMonth;

        private HeaderFooterEvent(String userName, LocalDate referenceMonth) {
            this.userName = userName;
            this.referenceMonth = referenceMonth;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.saveState();

            cb.setColorStroke(new Color(220, 231, 229));
            cb.moveTo(document.left(), document.top() + 12);
            cb.lineTo(document.right(), document.top() + 12);
            cb.stroke();

            ColumnText.showTextAligned(
                    cb,
                    Element.ALIGN_LEFT,
                    new Phrase(
                            "HELMA · Brief mensuel · " + userName + " · " + (referenceMonth != null ? referenceMonth.format(MONTH_FMT) : "—"),
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(100, 120, 120))
                    ),
                    document.left(),
                    document.top() + 18,
                    0
            );

            ColumnText.showTextAligned(
                    cb,
                    Element.ALIGN_RIGHT,
                    new Phrase(
                            "Page " + writer.getPageNumber(),
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL, new Color(100, 120, 120))
                    ),
                    document.right(),
                    document.bottom() - 18,
                    0
            );

            cb.restoreState();
        }
    }
}