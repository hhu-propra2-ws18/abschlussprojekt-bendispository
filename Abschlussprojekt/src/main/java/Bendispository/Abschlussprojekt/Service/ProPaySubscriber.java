package Bendispository.Abschlussprojekt.Service;

import Bendispository.Abschlussprojekt.Model.LeaseTransaction;
import Bendispository.Abschlussprojekt.Model.Person;
import Bendispository.Abschlussprojekt.Repo.LeaseTransactionRepo;
import Bendispository.Abschlussprojekt.Repo.PersonsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class ProPaySubscriber {

    @Autowired
    PersonsRepo personsRepo;

    @Autowired
    LeaseTransactionRepo leaseTransactionRepo;

    public int makeDeposit(int deposit, String leaserName, String lenderName){
        Reservation reservation = makeReservation(leaserName, lenderName, deposit, Reservation.class);
        return reservation.getId();
    }

    private <T> T makeReservation(String leaserName, String lenderName, int deposit, Class<T> type) {
        final Mono<T> mono = WebClient
                .create()
                .get()
                .uri(builder ->
                        builder.scheme("https")
                                .host("propra-propay.herokuapp.com")
                                .pathSegment("reservation", "reserve", lenderName, leaserName)
                                .query("amount={deposit}")
                                .build())
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .retrieve()
                .bodyToMono(type);
        return mono.block();
    }

    public boolean checkDeposit(int requiredDeposit, String username){
        ProPayAccount account = getAccount(username, ProPayAccount.class);
        if(account.getAmount() >= requiredDeposit)
            return true;
        return false;
    }

    private <T> T getAccount(String username, Class<T> type) {
        final Mono<T> mono = WebClient
                        .create()
                        .get()
                        .uri(builder ->
                                 builder.scheme("https")
                                        .host("propra-propay.herokuapp.com")
                                        .pathSegment("account", username)
                                        .build())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                        .retrieve()
                        .bodyToMono(type);
        return mono.block();
    }

}
