package esprit.tn.projet_pi.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class VoucherCodeGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom random = new SecureRandom();

    public String generateCode() {
        StringBuilder code = new StringBuilder("SV-");

        for(int i=0;i<10;i++){
            code.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return code.toString();
    }
}