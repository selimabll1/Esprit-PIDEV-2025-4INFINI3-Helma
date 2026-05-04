package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.enums.ApplicationRaiseStatus;
import com.helma.helmabackend.integration.mailjet.MailjetEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApplicationRaiseNotificationService {

    private final MailjetEmailService mailjetEmailService;

    public void notifyStatusChanged(ApplicationRaise a, ApplicationRaiseStatus oldStatus) {
        String toEmail = a.getContactEmail();
        String toName = a.getContactFirstName() + " " + a.getContactLastName();

        ApplicationRaiseStatus newStatus = a.getStatus();
        if (toEmail == null || toEmail.isBlank()) return;

        String subject = "Helma — Application status updated: " + newStatus;

        String text = buildText(a, oldStatus, newStatus);
        String html = buildHtml(a, oldStatus, newStatus);

        mailjetEmailService.sendStatusEmail(toEmail, toName, subject, text, html);
    }

    private String buildText(ApplicationRaise a, ApplicationRaiseStatus oldS, ApplicationRaiseStatus newS) {
        return """
               Hello %s,

               Your crowdfunding application "%s" is now: %s.
               Previous status: %s.

               Funding goal: %s %s
               Type: %s

               — Helma Team
               """.formatted(
                safe(a.getContactFirstName()),
                safe(a.getBusinessName()),
                newS, oldS,
                a.getFundingGoal(), a.getCurrency(),
                a.getType()
        );
    }

    private String buildHtml(ApplicationRaise a, ApplicationRaiseStatus oldS, ApplicationRaiseStatus newS) {
        return """
               <p>Hello <b>%s</b>,</p>
               <p>Your crowdfunding application <b>%s</b> status is now: <b>%s</b>.</p>
               <p>Previous status: %s</p>
               <hr/>
               <p><b>Type:</b> %s<br/>
               <b>Funding goal:</b> %s %s</p>
               <p>— Helma Team</p>
               """.formatted(
                safe(a.getContactFirstName()),
                safe(a.getBusinessName()),
                newS, oldS,
                a.getType(),
                a.getFundingGoal(), a.getCurrency()
        );
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}