package esprit.tn.projet_pi.repository;


 import esprit.tn.projet_pi.entity.User;
 import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
