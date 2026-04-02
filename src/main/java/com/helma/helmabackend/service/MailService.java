package com.helma.helmabackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;


    public void envoyerContratParMail(String destinataire, String nomClient, String pdfPath) {
        try {
            MimeMessage message = mailSender.createMimeMessage();


            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("rihemzoghlami21@gmail.com");
            helper.setTo(destinataire);
            helper.setSubject("Votre contrat de leasing — Helma");


            String corps = "<html><body>"
                    + "<h2>Bonjour " + nomClient + ",</h2>"
                    + "<p>Veuillez trouver ci-joint votre contrat de leasing.</p>"
                    + "<p>Pour toute question, n'hésitez pas à nous contacter.</p>"
                    + "<br>"
                    + "<p>Cordialement,</p>"
                    + "<p><strong>L'équipe Helma</strong></p>"
                    + "</body></html>";

            helper.setText(corps, true); // true = HTML


            FileSystemResource pdf = new FileSystemResource(new File(pdfPath));
            helper.addAttachment("contrat_leasing.pdf", pdf);

            mailSender.send(message);
            log.info("Mail envoyé avec succès à : {}", destinataire);

        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi du mail à {} : {}", destinataire, e.getMessage());
            throw new RuntimeException("Erreur envoi mail : " + e.getMessage());
        }
    }
}