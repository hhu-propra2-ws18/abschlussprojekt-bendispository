package Bendispository.Abschlussprojekt.service;

import Bendispository.Abschlussprojekt.model.transactionModels.ConflictTransaction;
import Bendispository.Abschlussprojekt.repos.RatingRepo;
import Bendispository.Abschlussprojekt.repos.RequestRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.ConflictTransactionRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.LeaseTransactionRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.PaymentTransactionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class ConflictService {

    private final ConflictTransactionRepo conflictTransactionRepo;

    private final TransactionService transactionService;

    private RatingRepo ratingRepo;

    @Autowired
    public ConflictService(
                           ConflictTransactionRepo conflictTransactionRepo,
                           TransactionService transactionService) {
        super();
        this.conflictTransactionRepo = conflictTransactionRepo;
        this.transactionService = transactionService;
    }

    public boolean resolveConflict(ConflictTransaction conflict, ConflictTransactionRepo conflictTransactionRepo, boolean depositBackToLeaser){
        conflict.setLeaserAccepted(true);
        conflict.setLenderAccepted(true);
        conflict.setLeaserGotTheDepositBack(depositBackToLeaser);
        conflictTransactionRepo.save(conflict);
        if(depositBackToLeaser)
            return transactionService.itemIsIntact(conflict.getLeaseTransaction());
        else
            return transactionService.itemIsNotIntactConclusion(conflict.getLeaseTransaction());
    }
}
