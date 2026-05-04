package com.helma.helmabackend.integration.mailjet;

import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.errors.MailjetException;
import com.mailjet.client.resource.Emailv31;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailjetEmailService {

    private final MailjetClient client;

    @Value("${mailjet.fromEmail}")
    private String fromEmail;

    @Value("${mailjet.fromName}")
    private String fromName;

    public void sendStatusEmail(String toEmail, String toName, String subject, String textBody, String htmlBody) {
        try {
            JSONObject message = new JSONObject()
                    .put(Emailv31.Message.FROM, new JSONObject()
                            .put("Email", fromEmail)
                            .put("Name", fromName))
                    .put(Emailv31.Message.TO, new JSONArray()
                            .put(new JSONObject()
                                    .put("Email", toEmail)
                                    .put("Name", toName == null ? "" : toName)))
                    .put(Emailv31.Message.SUBJECT, subject)
                    .put(Emailv31.Message.TEXTPART, textBody)
                    .put(Emailv31.Message.HTMLPART, htmlBody);

            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray().put(message));

            MailjetResponse response = client.post(request);

            // Optional: log non-200 responses (don’t crash the API for email failures)
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                // log response.getData()
            }

        } catch (MailjetException | RuntimeException ex) {
            // log error and continue (don’t break business flow)
        }
    }
}