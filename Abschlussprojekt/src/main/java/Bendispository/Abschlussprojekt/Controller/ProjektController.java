package Bendispository.Abschlussprojekt.Controller;

import Bendispository.Abschlussprojekt.model.Item;
import Bendispository.Abschlussprojekt.model.Person;
import Bendispository.Abschlussprojekt.repo.ItemRepo;
import Bendispository.Abschlussprojekt.repo.PersonsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;


@Controller
public class ProjektController {
    @Autowired
    ItemRepo itemRepo;
    @Autowired
    PersonsRepo personRepo;

    @GetMapping(path = "/addItem")
    public String addItemPage(){
        return "AddItem";
    }

    @PostMapping(path = "/addItem")
    public String addItemsToDatabase(Model model, Item item){
        model.addAttribute("newItem", item);
        itemRepo.save(item);
        return "AddItem";
    }

    @GetMapping(path = "/Item/{id}" )
    public String ItemProfile(Model model, @PathVariable Long id) {
        Optional <Item> item = itemRepo.findById(id);
        model.addAttribute("itemProfile", item.get());
        return "ItemProfile";
    }
    @GetMapping(path = "/registration")
    public String Registration(Model model, Person person) {
        model.addAttribute("newPerson", person);
        personRepo.save(person);
        return "registration";
    }
    @GetMapping(path= "/")
    public String Overview(Model model){
        List<Item> all = itemRepo.findAll();
        model.addAttribute("OverviewAllItems", all);
        return "OverviewAllItems";
    }
}
