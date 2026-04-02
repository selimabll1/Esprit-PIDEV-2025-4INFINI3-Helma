package com.helma.helmabackend.service;

import com.helma.helmabackend.entity.ContratLeasing;
import com.helma.helmabackend.entity.StatutContrat;
import com.helma.helmabackend.repository.ContratLeasingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.io.font.constants.StandardFonts;
import com.helma.helmabackend.entity.PaiementLeasing;
import com.helma.helmabackend.entity.StatutPaiement;
import org.springframework.beans.factory.annotation.Value;
import java.io.File;

import java.math.BigDecimal;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ContratLeasingService {

    private final ContratLeasingRepository contratLeasingRepository;
    private final com.helma.helmabackend.repository.DemandeLeasingRepository demandeLeasingRepository;
    private final MailService mailService;

    @Value("${app.pdf.directory}")
    private String pdfDirectory;

    public ContratLeasing create(ContratLeasing contrat) {
        if (contrat.getDemande() != null && contrat.getDemande().getId() != null) {
            com.helma.helmabackend.entity.DemandeLeasing demande = demandeLeasingRepository.findById(contrat.getDemande().getId())
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'id: " + contrat.getDemande().getId()));
            contrat.setDemande(demande);


            if (contrat.getPaiements() == null) {
                contrat.setPaiements(new ArrayList<>());
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
            LocalDate dateEcheance = contrat.getDateDebut();

            for (int i = 0; i < demande.getDureeMois(); i++) {
                PaiementLeasing paiement = PaiementLeasing.builder()
                        .contrat(contrat)
                        .mois(dateEcheance.format(formatter))
                        .montant(contrat.getLoyerMensuel())
                        .statutPaiement(StatutPaiement.EN_ATTENTE)
                        .build();

                contrat.getPaiements().add(paiement);
                dateEcheance = dateEcheance.plusMonths(1);
            }
        }
        return contratLeasingRepository.save(contrat);
    }

    public ContratLeasing update(Long id, ContratLeasing contrat) {
        ContratLeasing existing = findById(id);

        if (contrat.getDemande() != null && contrat.getDemande().getId() != null) {
            com.helma.helmabackend.entity.DemandeLeasing demande = demandeLeasingRepository.findById(contrat.getDemande().getId())
                    .orElseThrow(() -> new RuntimeException("Demande non trouvée avec l'id: " + contrat.getDemande().getId()));
            existing.setDemande(demande);
        } else {
            existing.setDemande(contrat.getDemande());
        }

        existing.setLoyerMensuel(contrat.getLoyerMensuel());
        existing.setDateDebut(contrat.getDateDebut());
        existing.setDateFin(contrat.getDateFin());
        existing.setStatut(contrat.getStatut());
        return contratLeasingRepository.save(existing);
    }

    public void delete(Long id) {
        if (!contratLeasingRepository.existsById(id)) {
            throw new RuntimeException("Contrat non trouvé avec l'id: " + id);
        }
        contratLeasingRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ContratLeasing findById(Long id) {
        return contratLeasingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé avec l'id: " + id));
    }

    @Transactional(readOnly = true)
    public List<ContratLeasing> findAll() {
        return contratLeasingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ContratLeasing findByDemandeId(Long demandeId) {
        return contratLeasingRepository.findByDemandeId(demandeId)
                .orElseThrow(() -> new RuntimeException("Contrat non trouvé pour la demande id: " + demandeId));
    }

    @Transactional(readOnly = true)
    public List<ContratLeasing> findByStatut(StatutContrat statut) {
        return contratLeasingRepository.findByStatut(statut);
    }

    public ContratLeasing updateStatut(Long id, StatutContrat statut) {
        ContratLeasing contrat = findById(id);
        contrat.setStatut(statut);
        return contratLeasingRepository.save(contrat);
    }


    @Transactional(readOnly = true)
    public BigDecimal calculateRevenuMensuelTotal() {
        BigDecimal revenu = contratLeasingRepository.calculateRevenuMensuelTotal();
        return revenu != null ? revenu : BigDecimal.ZERO;
    }


    @Transactional(readOnly = true)
    public BigDecimal calculateRevenuAnnuelEstime() {
        BigDecimal revenuMensuel = calculateRevenuMensuelTotal();
        return revenuMensuel.multiply(BigDecimal.valueOf(12));
    }


    public void genererEtEnvoyerContrat(Long contratId, String emailClient, String nomClient) {
        ContratLeasing contrat = findById(contratId);
        String pdfPath = genererPDF(contrat, nomClient);
        mailService.envoyerContratParMail(emailClient, nomClient, pdfPath);
        log.info("Contrat {} généré et envoyé à {}", contratId, emailClient);
    }


    private String genererPDF(ContratLeasing contrat, String nomClient) {
        File dossier = new File(pdfDirectory);
        if (!dossier.exists()) {
            dossier.mkdirs();
        }

        String nomFichier = "contrat_" + contrat.getId() + ".pdf";
        String cheminComplet = pdfDirectory + nomFichier;

        try {

            PdfWriter writer = new PdfWriter(cheminComplet);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            document.add(new Paragraph("CONTRAT DE LEASING")
                    .setFont(boldFont)
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Informations Client")
                    .setFont(boldFont).setFontSize(12));

            document.add(new Paragraph("Nom : " + nomClient)
                    .setFont(normalFont).setFontSize(12));

            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Détails du Contrat")
                    .setFont(boldFont).setFontSize(12));

            document.add(new Paragraph("Numéro de contrat : " + contrat.getId())
                    .setFont(normalFont).setFontSize(12));

            document.add(new Paragraph("Date de début     : " + contrat.getDateDebut())
                    .setFont(normalFont).setFontSize(12));

            document.add(new Paragraph("Date de fin       : " + contrat.getDateFin())
                    .setFont(normalFont).setFontSize(12));

            document.add(new Paragraph("Loyer mensuel     : " + contrat.getLoyerMensuel() + " TND")
                    .setFont(normalFont).setFontSize(12));

            document.add(new Paragraph("Statut            : " + contrat.getStatut())
                    .setFont(normalFont).setFontSize(12));

            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Signature : ________________________")
                    .setFont(normalFont).setFontSize(12));

            document.close();
            log.info("PDF généré : {}", cheminComplet);

        } catch (Exception e) {
            log.error("Erreur génération PDF : {}", e.getMessage());
            throw new RuntimeException("Erreur génération PDF : " + e.getMessage());
        }

        return cheminComplet;
    }


}
