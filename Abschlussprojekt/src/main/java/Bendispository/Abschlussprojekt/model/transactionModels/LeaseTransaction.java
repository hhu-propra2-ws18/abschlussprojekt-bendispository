package Bendispository.Abschlussprojekt.model.transactionModels;

import Bendispository.Abschlussprojekt.model.Item;
import Bendispository.Abschlussprojekt.model.Person;
import Bendispository.Abschlussprojekt.model.Request;

import lombok.Data;

import javax.persistence.*;
import java.util.Optional;

@Data
@Entity
public class LeaseTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(
               fetch = FetchType.EAGER)
    private Person leaser;

    @ManyToOne(
              fetch = FetchType.EAGER)
    private Person lender;

    @ManyToOne(
               fetch = FetchType.EAGER)
    private Item item;

    // number of days
    private int duration;

    private int depositId;

    private boolean itemIsReturned = false;

    private boolean itemIsReturnedOnTime = false;

    private boolean itemIsIntact = false;

    private ConcludeTransaction ccTrans;

    public void addLeaseTransaction(Request request){
        LeaseTransaction lsTrans = new LeaseTransaction();
        lsTrans.setItem(request.getRequestedItem());
        lsTrans.setLeaser(request.getRequester());
        lsTrans.setLender(request.getRequestedItem().getOwner());
        lsTrans.setDuration(request.getDuration());
        ccTrans.addConcludeTransaction();
    }

}
