package com.helma.helmabackend.service.crowdfunding;

import com.helma.helmabackend.entity.crowdfunding.ApplicationRaise;
import com.helma.helmabackend.entity.crowdfunding.Payment;
import com.helma.helmabackend.entity.crowdfunding.Pledge;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

@Service
public class StripeCheckoutService {

    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF"
    );

    private static final Set<String> THREE_DECIMAL_CURRENCIES = Set.of(
            "BHD", "JOD", "KWD", "OMR", "TND"
    );

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public StripeCheckoutSessionData createCheckoutSession(
            Payment payment,
            Pledge pledge,
            ApplicationRaise campaign
    ) throws StripeException {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key is not configured. Set STRIPE_SECRET_KEY.");
        }

        Stripe.apiKey = stripeSecretKey.trim();

        // Stripe account currently does not accept TND in your setup.
        // For development/testing we charge the same displayed amount in USD.
        String stripeCurrency = "USD";
        Long amountInMinorUnits = toMinorUnits(payment.getAmount(), stripeCurrency);

        String campaignName = campaign.getBusinessName() == null || campaign.getBusinessName().isBlank()
                ? "Helma campaign pledge"
                : campaign.getBusinessName().trim();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl
                        + "/investor/my-payments?stripe=success&paymentId="
                        + payment.getId()
                        + "&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontendUrl
                        + "/investor/my-payments?stripe=cancel&paymentId="
                        + payment.getId()
                        + "&session_id={CHECKOUT_SESSION_ID}")
                .setClientReferenceId("helma_payment_" + payment.getId())
                .putMetadata("paymentId", String.valueOf(payment.getId()))
                .putMetadata("pledgeId", String.valueOf(pledge.getId()))
                .putMetadata("applicationRaiseId", String.valueOf(campaign.getId()))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(stripeCurrency.toLowerCase(Locale.ROOT))
                                                .setUnitAmount(amountInMinorUnits)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Helma pledge - " + campaignName)
                                                                .setDescription("Investment pledge payment for " + campaignName)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return new StripeCheckoutSessionData(session.getId(), session.getUrl());
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            return "USD";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private Long toMinorUnits(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Payment amount is required.");
        }

        int scale = currencyScale(currency);
        return amount
                .setScale(scale, RoundingMode.HALF_UP)
                .movePointRight(scale)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }

    private int currencyScale(String currency) {
        String normalized = normalizeCurrency(currency);
        if (ZERO_DECIMAL_CURRENCIES.contains(normalized)) {
            return 0;
        }
        if (THREE_DECIMAL_CURRENCIES.contains(normalized)) {
            return 3;
        }
        return 2;
    }

    public record StripeCheckoutSessionData(String sessionId, String checkoutUrl) {}
}
