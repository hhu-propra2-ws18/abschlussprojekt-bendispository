package Bendispository.Abschlussprojekt.model.transactionModels;

import Bendispository.Abschlussprojekt.model.Person;
import Bendispository.Abschlussprojekt.repos.transactionRepos.PaymentTransactionRepo;
import Bendispository.Abschlussprojekt.service.ProPaySubscriber;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.util.Optional;

@Data
@Entity
public class PaymentTransaction {

    // relies on ProPay
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    private Person leaser;

    @ManyToOne
    private Person lender;

    private int amount;

    private boolean transferIsOk;

    private boolean depositIsBlocked;

    private boolean depositIsReturned;

    private boolean lenderAccepted;

    @OneToOne
    private ConflictTransaction conflictTransaction;

    public PaymentTransaction(Person leaser, Person lender, int amount){
        this.leaser = leaser;
        this.lender = lender;
        this.amount = amount;
    }

    /*
    public void isTransferIsOk(){

    }*/
}
