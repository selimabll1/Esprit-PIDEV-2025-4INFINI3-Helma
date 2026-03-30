package tn.esprit.projet_pi.service;

import org.springframework.stereotype.Service;
import tn.esprit.projet_pi.dto.request.LoanPaymentDTO;
import tn.esprit.projet_pi.entity.LoanPayment;
import tn.esprit.projet_pi.entity.LoanTransaction;
import tn.esprit.projet_pi.entity.RepaymentSchedule;
import tn.esprit.projet_pi.enums.PaymentStatus;
import tn.esprit.projet_pi.enums.TransactionType;
import tn.esprit.projet_pi.exception.InvalidPaymentException;
import tn.esprit.projet_pi.exception.LoanNotFoundException;
import tn.esprit.projet_pi.repository.LoanPaymentRepository;
import tn.esprit.projet_pi.repository.LoanTransactionRepository;
import tn.esprit.projet_pi.repository.RepaymentScheduleRepository;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class RepaymentService {

        private final RepaymentScheduleRepository repaymentScheduleRepository;
        private final LoanPaymentRepository loanPaymentRepository;
        private final LoanTransactionRepository loanTransactionRepository;

        public RepaymentService(RepaymentScheduleRepository repaymentScheduleRepository,
                        LoanPaymentRepository loanPaymentRepository,
                        LoanTransactionRepository loanTransactionRepository) {
                this.repaymentScheduleRepository = repaymentScheduleRepository;
                this.loanPaymentRepository = loanPaymentRepository;
                this.loanTransactionRepository = loanTransactionRepository;
        }

        /**
         * Paie une échéance identifiée par dto.scheduleId.
         * Crée un LoanPayment et un LoanTransaction liés par le même UUID.
         */
        public LoanPayment payInstallment(LoanPaymentDTO dto) {

                // 1. Trouver l'échéance
                RepaymentSchedule schedule = repaymentScheduleRepository
                                .findById(dto.getScheduleId())
                                .orElseThrow(() -> new LoanNotFoundException(
                                                "Schedule not found with id: " + dto.getScheduleId()));

                // 2. Vérifier que l'échéance n'est pas déjà payée
                if (schedule.getStatus() == PaymentStatus.PAID) {
                        throw new InvalidPaymentException("Schedule already paid");
                }

                // 3. Marquer l'échéance comme payée
                schedule.setPaidAmount(dto.getAmount());
                schedule.setStatus(PaymentStatus.PAID);
                repaymentScheduleRepository.save(schedule);

                // 4. Référence partagée entre paiement et transaction
                String reference = UUID.randomUUID().toString();

                // 5. Créer le paiement
                LoanPayment payment = LoanPayment.builder()
                                .loanId(dto.getLoanId())
                                .scheduleId(dto.getScheduleId())
                                .amount(dto.getAmount())
                                .paidAt(LocalDate.now())
                                .paymentMethod(dto.getPaymentMethod())
                                .reference(reference)
                                .build();
                payment = loanPaymentRepository.save(payment);

                // 6. Créer la transaction comptable
                LoanTransaction transaction = LoanTransaction.builder()
                                .loanId(dto.getLoanId())
                                .type(TransactionType.REPAYMENT)
                                .amount(dto.getAmount())
                                .transactionDate(LocalDate.now())
                                .reference(reference)
                                .build();
                loanTransactionRepository.save(transaction);

                return payment;
        }
}
