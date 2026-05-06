package tn.esprit.projet_pi.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class UserEmailResolver {

    @Value("${helma.user-service.url:http://localhost:8080}")
    private String userServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public Optional<String> resolveEmail(Long userId) {
        try {
            Map<?, ?> user = restTemplate.getForObject(userServiceUrl + "/api/users/" + userId, Map.class);
            Object email = user != null ? user.get("email") : null;
            if (email instanceof String value && !value.isBlank()) {
                return Optional.of(value);
            }
            log.warn("Aucun champ email trouve pour userId {}", userId);
        } catch (Exception e) {
            log.warn("Impossible de recuperer l'email utilisateur {}: {}", userId, e.getMessage());
        }
        return Optional.empty();
    }
}
