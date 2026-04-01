package esprit.tn.projet_pi.config;


import esprit.tn.projet_pi.entity.User;
import esprit.tn.projet_pi.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUser(UserRepository userRepository){
        return args -> {

            if(!userRepository.existsById(1L)){
                User user = new User();
                user.setId(1L);
                user.setName("Static User");

                userRepository.save(user);
            }
        };
    }
}
