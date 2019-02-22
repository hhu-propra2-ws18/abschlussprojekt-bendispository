package Bendispository.Abschlussprojekt.service;

import Bendispository.Abschlussprojekt.model.Person;
import Bendispository.Abschlussprojekt.model.Rating;
import Bendispository.Abschlussprojekt.model.Request;
import Bendispository.Abschlussprojekt.model.RequestStatus;
import Bendispository.Abschlussprojekt.model.transactionModels.*;
import Bendispository.Abschlussprojekt.repos.RatingRepo;
import Bendispository.Abschlussprojekt.repos.RequestRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.ConflictTransactionRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.LeaseTransactionRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.PaymentTransactionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Component
public class TransactionService {

    private final RequestRepo requestRepo;

    private final LeaseTransactionRepo leaseTransactionRepo;

    private final PaymentTransactionRepo paymentTransactionRepo;

    private final ConflictTransactionRepo conflictTransactionRepo;

    private RatingRepo ratingRepo;

    private ProPaySubscriber proPaySubscriber;

    @Autowired
    public TransactionService(LeaseTransactionRepo leaseTransactionRepo,
                              RequestRepo requestRepo,
                              ProPaySubscriber proPaySubscriber,
                              PaymentTransactionRepo paymentTransactionRepo,
                              ConflictTransactionRepo conflictTransactionRepo,
                              RatingRepo ratingRepo) {
        super();
        this.leaseTransactionRepo = leaseTransactionRepo;
        this.requestRepo = requestRepo;
        this.proPaySubscriber = proPaySubscriber;
        this.paymentTransactionRepo = paymentTransactionRepo;
        this.conflictTransactionRepo = conflictTransactionRepo;
        this.ratingRepo = ratingRepo;
    }

    public boolean lenderApproved(Request request){
        double deposit = request.getRequestedItem().getDeposit();
        Person requester = request.getRequester();

        if(proPaySubscriber.checkDeposit(deposit,
                                         requester.getUsername())) {
            int depositId = proPaySubscriber.makeDeposit(request);
            LeaseTransaction leaseTransaction = new LeaseTransaction();
            leaseTransaction.addLeaseTransaction(request, depositId);

            PaymentTransaction payment = makePayment(requester, request.getRequestedItem().getOwner(),
                                                                deposit, leaseTransaction, PaymentType.DEPOSIT);
            leaseTransaction.addPaymentTransaction(payment);
            leaseTransactionRepo.save(leaseTransaction);

            request.setLeaseTransaction(leaseTransaction);
            setRequestApproved(request);
            createRating(request);
            requestRepo.save(request);

            return true;
        }
        return false;
    }
    private void createRating(Request request){
        Rating rating1 = new Rating();
        rating1.setRequest(request);
        rating1.setRater(request.getRequester());

        Rating rating2 = new Rating();
        rating2.setRequest(request);
        rating2.setRater(request.getRequestedItem().getOwner());

        ratingRepo.save(rating1);
        ratingRepo.save(rating2);
    }

    private void setRequestApproved(Request request){
        setOtherRequestsOnDenied(request);
        request.setStatus(RequestStatus.APPROVED);
    }

    private void setOtherRequestsOnDenied(Request request) {
        List<Request> requestList = requestRepo.findAllByRequestedItem(request.getRequestedItem());
        for(Request r  : requestList)
            if(isOverlapping(r.getStartDate(), r.getEndDate(),
                             request.getStartDate(), request.getEndDate()))
                r.setStatus(RequestStatus.DENIED);
    }

    public boolean itemIsAvailableOnTime(Request request) {
        List<LeaseTransaction> leaseTransactionList =
                leaseTransactionRepo
                        .findAllByItemId(request.getRequestedItem().getId());
        for(LeaseTransaction l  : leaseTransactionList)
            if(isOverlapping(l.getStartDate(), l.getEndDate(),
                    request.getStartDate(), request.getEndDate()))
                return false;
        return true;
    }

    // Disclaimer: https://stackoverflow.com/a/17107966
    public static boolean isOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return (start1.isBefore(end2) && start2.isBefore(end1));
    }

    public void itemReturnedToLender(LeaseTransaction leaseTransaction){
        Person leaser = leaseTransaction.getLeaser();
        Person lender = leaseTransaction.getItem().getOwner();
        leaseTransaction.setItemIsReturned(true);

        double amount = (double) leaseTransaction.getDuration() * leaseTransaction.getItem().getCostPerDay();
        PaymentTransaction payment = makePayment(leaser, lender, amount,
                                                 leaseTransaction, PaymentType.RENTPRICE);
        proPaySubscriber.transferMoney(leaser.getUsername(), lender.getUsername(), amount);
        leaseTransaction.addPaymentTransaction(payment);
        isReturnedInTime(leaseTransaction, leaser, lender);
        leaseTransactionRepo.save(leaseTransaction);
    }

    private void isReturnedInTime(LeaseTransaction leaseTransaction, Person leaser, Person lender){
        if(isTimeViolation(leaseTransaction)){
            double amount = leaseTransaction.getItem().getCostPerDay() * leaseTransaction.getLengthOfTimeframeViolation();
            PaymentTransaction payment = makePayment(leaser, lender, amount, leaseTransaction, PaymentType.DAMAGES);
            proPaySubscriber.transferMoney(leaser.getUsername(), lender.getUsername(), amount);
            leaseTransaction.addPaymentTransaction(payment);
        }
    }

    public void itemIsIntact(LeaseTransaction leaseTransaction){
        proPaySubscriber
                .releaseReservation(
                        leaseTransaction.getLeaser().getUsername(),
                        leaseTransaction.getDepositId(),
                        ProPayAccount.class);
        conclude(leaseTransaction);
    }

    public void itemIsNotIntactConclusion(LeaseTransaction leaseTransaction) {
        proPaySubscriber
                .releaseReservationAndPunishUser(
                        leaseTransaction.getLeaser().getUsername(),
                        leaseTransaction.getDepositId(),
                        ProPayAccount.class);
        conclude(leaseTransaction);
    }

    private void conclude(LeaseTransaction leaseTransaction){
        for (PaymentTransaction payment : leaseTransaction.getPayments()){
            if(payment.getType() == PaymentType.DEPOSIT){
                payment.setPaymentIsConcluded(true);
                paymentTransactionRepo.save(payment);
                break;
            }
        }
        leaseTransaction.setLeaseIsConcluded(true);
        leaseTransactionRepo.save(leaseTransaction);
    }

    private PaymentTransaction makePayment(Person leaser, Person lender, double amount,
                                           LeaseTransaction leaseTransaction, PaymentType type){
        PaymentTransaction paymentTransaction = new PaymentTransaction(leaser, lender, amount);
        paymentTransaction.setType(type);
        paymentTransaction.setLeaseTransaction(leaseTransaction);
        paymentTransaction.setPaymentIsConcluded(true);
        paymentTransactionRepo.save(paymentTransaction);
        //proPaySubscriber.transferMoney(leaser.getUsername(), lender.getUsername(), amount);
        return paymentTransaction;
    }

    public void itemIsNotIntact(Person me, LeaseTransaction leaseTransaction, String commentary) {
        ConflictTransaction conflictTransaction = new ConflictTransaction();
        conflictTransaction.setLeaseTransaction(leaseTransaction);
        conflictTransaction.setCommentary(commentary);
        conflictTransactionRepo.save(conflictTransaction);
    }

    public boolean isTimeViolation(LeaseTransaction leaseTransaction) {
        if(LocalDate.now().isAfter(leaseTransaction.getEndDate())) {
            Period period = Period.between(leaseTransaction.getEndDate(), LocalDate.now());
            int timeViolation = period.getDays();
            leaseTransaction.setTimeframeViolation(true);
            leaseTransaction.setLengthOfTimeframeViolation(timeViolation);
            return true;
        }
        return false;
    }
}
