package tn.esprit.helma.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class MailTestController {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @GetMapping("/mail")
    public Map<String, String> testMail(@RequestParam(defaultValue = "kyassine2003@gmail.com") String to) {
        log.info("[MAIL-TEST] Tentative d'envoi de {} vers {}", from, to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("[Helma] Test SMTP");
            helper.setText("Si vous recevez cet email, la configuration SMTP fonctionne correctement.", false);
            mailSender.send(message);
            log.info("[MAIL-TEST] Envoi réussi vers {}", to);
            return Map.of("status", "OK", "message", "Email envoyé à " + to);
        } catch (Exception ex) {
            log.error("[MAIL-TEST] Échec — type: {} — raison: {}", ex.getClass().getSimpleName(), ex.getMessage());
            return Map.of("status", "ERREUR", "type", ex.getClass().getSimpleName(), "message", ex.getMessage() != null ? ex.getMessage() : "null");
        }
    }
}
