package Bendispository.Abschlussprojekt.controller;

import Bendispository.Abschlussprojekt.model.*;
import Bendispository.Abschlussprojekt.model.transactionModels.LeaseTransaction;
import Bendispository.Abschlussprojekt.repos.ItemRepo;
import Bendispository.Abschlussprojekt.repos.PersonsRepo;
import Bendispository.Abschlussprojekt.repos.RatingRepo;
import Bendispository.Abschlussprojekt.repos.RequestRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.ConflictTransactionRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.LeaseTransactionRepo;
import Bendispository.Abschlussprojekt.repos.transactionRepos.PaymentTransactionRepo;
import Bendispository.Abschlussprojekt.service.AuthenticationService;
import Bendispository.Abschlussprojekt.service.ProPaySubscriber;
import Bendispository.Abschlussprojekt.service.RequestService;
import Bendispository.Abschlussprojekt.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import static Bendispository.Abschlussprojekt.model.RequestStatus.PENDING;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RequestController {

    private final RequestRepo requestRepo;
    private final ItemRepo itemRepo;
    private final LeaseTransactionRepo leaseTransactionRepo;
    private final PersonsRepo personsRepo;
    private final TransactionService transactionService;
    private final PaymentTransactionRepo paymentTransactionRepo;
    private final ProPaySubscriber proPaySubscriber;
    private final AuthenticationService authenticationService;
    private final RequestService requestService;
    private final RatingRepo ratingRepo;
    private final ConflictTransactionRepo conflictTransactionRepo;

    @Autowired
    public RequestController(RequestRepo requestRepo,
                             ItemRepo itemRepo,
                             LeaseTransactionRepo leaseTransactionRepo,
                             PersonsRepo personsRepo,
                             PaymentTransactionRepo paymentTransactionRepo,
                             RatingRepo ratingrepo,
                             ConflictTransactionRepo conflictTransactionRepo,
                             RequestService requestService) {
        this.ratingRepo = ratingrepo;
        this.requestRepo = requestRepo;
        this.itemRepo = itemRepo;
        this.leaseTransactionRepo = leaseTransactionRepo;
        this.personsRepo = personsRepo;
        this.paymentTransactionRepo = paymentTransactionRepo;
        this.conflictTransactionRepo = conflictTransactionRepo;
        this.authenticationService = new AuthenticationService(personsRepo);
        this.proPaySubscriber = new ProPaySubscriber(personsRepo,
                                                     leaseTransactionRepo);
        this.transactionService = new TransactionService(leaseTransactionRepo,
                                                         requestRepo,
                                                         proPaySubscriber,
                                                         paymentTransactionRepo,
                                                         conflictTransactionRepo,
                                                         ratingRepo);
        this.requestService = requestService;
    }

    @GetMapping(path = "/item{id}/requestItem")
    public String request(Model model, @PathVariable Long id, RedirectAttributes redirectAttributes){
        itemRepo.findById(id).ifPresent(o -> model.addAttribute("thisItem",o));
        if (itemRepo.findById(id).get().getOwner().getUsername()
                .equals(authenticationService.getCurrentUser().getUsername())){
            return "redirect:/Item/{id}"; // soll auf editieren gehen
        }
        List<Request> requests = requestRepo.findByRequesterAndAndRequestedItemAndStatus
                (authenticationService.getCurrentUser(), itemRepo.findById(id).get(), RequestStatus.PENDING);
        if (!(requests.isEmpty())) {
            redirectAttributes.addFlashAttribute("message",
                    "You cannot request the same item twice!");
            return "redirect:/Item/{id}";
        }

        List <LeaseTransaction> list = leaseTransactionRepo
                                         .findAllByItemIdAndEndDateGreaterThan(id, LocalDate.now());

        Collections.sort(list, Comparator.comparing(LeaseTransaction::getStartDate));
        model.addAttribute("leases", list);
        return "formRequest";
    }

    @PostMapping(path = "/item{id}/requestItem")
    public String addRequestToLender(String startDate,
                                     String endDate,
                                     Model model,
                                     @PathVariable Long id,
                                     RedirectAttributes redirectAttributes
                                     ){

        return requestService.addRequest(model, redirectAttributes, startDate, endDate, id);
    }

    @GetMapping(path="/profile/requests")
    public String Requests(Model model){
        Long id = authenticationService.getCurrentUser().getId();
        requestService.showRequests(model,id);
        return "requests";
    }

    @PostMapping(path="/profile/requests")
    public String AcceptDeclineRequests(Model model,
                                        Long requestID,
                                        Optional<Integer> delete,
                                        Integer requestMyItems,
                                        RedirectAttributes redirectAttributes){
        Request request = requestRepo.findById(requestID).orElse(null);
        Long id = authenticationService.getCurrentUser().getId();

        if(delete.isPresent())
            if(delete.get() == -1){
                requestRepo.deleteById(requestID);
                return "requests";
            }

        if(requestMyItems == -1){
            request.setStatus(RequestStatus.DENIED);
            requestRepo.save(request);
            requestService.showRequests(model,id);
            return "requests";
        }
        if(transactionService.lenderApproved(request)){
            requestService.showRequests(model,id);
            return "requests";
        }
        requestService.showRequests(model,id);
        redirectAttributes.addFlashAttribute("message", "Hopeful Leaser does not have the funds for making a deposit!");
        return "redirect:/Item/{id}";
    }

    @GetMapping(path="/profile/rentedItems")
    public String rentedItems(Model model){
        Person me = authenticationService.getCurrentUser();
        List<LeaseTransaction> myRentedItems = leaseTransactionRepo.findAllByLeaserAndItemIsReturnedIsFalse(me);
        model.addAttribute("myRentedItems", myRentedItems);
        List<LeaseTransaction> myLeasedItems = leaseTransactionRepo.findAllByItemOwnerAndItemIsReturnedIsFalse(me);
        model.addAttribute("myLeasedItems", myLeasedItems);
        return "rentedItems";
    }

    @PostMapping(path = "/profile/rentedItems")
    public String returnItem(Model model,
                             Long id){
        LeaseTransaction leaseTransaction = leaseTransactionRepo.findById(id).orElse(null);
        transactionService.itemReturnedToLender(leaseTransaction);
        return "rentedItems";
    }

    @GetMapping(path= "/profile/returneditems")
    public String returnedItem(Model model){
        Person me = authenticationService.getCurrentUser();
        List<LeaseTransaction> transactionList =
                leaseTransactionRepo
                        .findAllByItemIsReturnedIsTrueAndLeaseIsConcludedIsFalseAndItemOwner(me);
        model.addAttribute("transactionList", transactionList);
        return "returnedItems";
    }

    @PostMapping(path= "/profile/returneditems")
    public String stateOfItem(Model model,
                              Long transactionId,
                              Integer itemIntact){
        LeaseTransaction leaseTransaction = leaseTransactionRepo
                                                    .findById(transactionId)
                                                    .orElse(null);
        Long id = authenticationService.getCurrentUser().getId();
        Person me = personsRepo.findById(id).orElse(null);
        if(itemIntact == -1){
            // Anliegen bleibt in returnedItems(?) => Oder eher offene Anliegen?
            return "redirect:/profile/returneditems/" + transactionId + "/issue";
        }
        transactionService.itemIsIntact(leaseTransaction);
        List<LeaseTransaction> transactionList =
                leaseTransactionRepo
                        .findAllByItemIsReturnedIsTrueAndLeaseIsConcludedIsFalseAndItemOwner(me);
        model.addAttribute("transactionList", transactionList);
        // Feld: iwie Bewertung /Clara
        return "returnedItems";
    }

    @GetMapping(path= "/profile/returneditems/{transactionId}/issue")
    public String returnedItemIsNotIntact(Model model,
                                          @PathVariable Long transactionId){
        LeaseTransaction transaction = leaseTransactionRepo.findById(transactionId).orElse(null);
        String comment = "";
        model.addAttribute("comment", comment);
        model.addAttribute("transaction", transaction);
        return "issue";
    }

    @PostMapping(path= "/profile/returneditems/{id}/issue")
    public String returnedItemIsNotIntactPost(Model model,
                                              @PathVariable Long id,
                                              String comment){
        Long userId = authenticationService.getCurrentUser().getId();
        Person me = personsRepo.findById(userId).orElse(null);
        LeaseTransaction leaseTransaction = leaseTransactionRepo
                .findById(id)
                .orElse(null);
        leaseTransaction.setLeaseIsConcluded(true);
        transactionService.itemIsNotIntact(me, leaseTransaction, comment);
        List<LeaseTransaction> transactionList =
              leaseTransactionRepo
                    .findAllByItemIsReturnedIsTrueAndLeaseIsConcludedIsFalseAndItemOwner(me);
        model.addAttribute("transactionList", transactionList);
        return "returnedItems";
    }
}

