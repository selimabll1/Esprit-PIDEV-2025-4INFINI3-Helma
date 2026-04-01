package esprit.tn.projet_pi.controller;

import esprit.tn.projet_pi.entity.SavingsGoal;
import esprit.tn.projet_pi.entity.Voucher;
import esprit.tn.projet_pi.repository.SavingsGoalRepository;
import esprit.tn.projet_pi.repository.VoucherRepository;
import esprit.tn.projet_pi.service.QRCodeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/vouchers")
@CrossOrigin(origins = "http://localhost:4200")
public class VoucherController {

    private final VoucherRepository voucherRepository;
    private final SavingsGoalRepository goalRepository;
    private final QRCodeService qrService;

    public VoucherController(VoucherRepository voucherRepository,QRCodeService qrService ,SavingsGoalRepository goalRepository) {
        this.voucherRepository = voucherRepository;
        this.goalRepository = goalRepository;
        this.qrService = qrService;

    }

    // CLAIM VOUCHER (create if first time, else return existing)
    @GetMapping("/{goalId}")
    public ResponseEntity<?> claimVoucher(@PathVariable Long goalId){

        SavingsGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));

        if(goal.getCurrentAmount() < goal.getTargetAmount()){
            return ResponseEntity.badRequest().body("Goal not completed yet");
        }

        Voucher voucher = voucherRepository.findBySavingsGoalId(goalId)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        // ⭐ THE IMPORTANT PART ⭐
        if(voucher.getCode() == null){

            voucher.setCode(generateCode());
            voucher.setClaimed(true);
            voucherRepository.save(voucher);
        }

        return ResponseEntity.ok(voucher);
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Voucher>> getUserVouchers(@PathVariable Long userId){
        return ResponseEntity.ok(
                voucherRepository.findBySavingsGoalUserId(userId)
        );
    }

    private String generateCode(){
        return "RX-" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
    }









    // GET /qr/{id} → retourne une image PNG du QR Code
    @GetMapping(value="/qr/{id}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> getVoucherQR(@PathVariable Long id) throws Exception {

        // Recherche du voucher en base
        Optional<Voucher> voucherOpt = voucherRepository.findById(id);

        // Si non trouvé → 404
        if(voucherOpt.isEmpty()){
            return ResponseEntity.status(404)
                    .body("Voucher not found or goal not achieved yet");
        }

        // Génération du QR via le service
        Voucher voucher = voucherOpt.get();
        byte[] qr = qrService.generateVoucherQR(voucher);

        return ResponseEntity.ok(qr);
    }







}
