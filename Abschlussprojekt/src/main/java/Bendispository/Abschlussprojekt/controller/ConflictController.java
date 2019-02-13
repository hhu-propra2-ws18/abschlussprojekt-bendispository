package Bendispository.Abschlussprojekt.controller;

import Bendispository.Abschlussprojekt.Model.ConflictTransaction;
import Bendispository.Abschlussprojekt.Repo.ConflictTransactionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;


//

@Controller
public class ConflictController {

    @Autowired
    ConflictTransactionRepo conflictTransactionRepo;

    @GetMapping(path = "/profile/conflictTransaction")
    public String listAllConflictTransaction(Model model){
        List<ConflictTransaction> allConflicts = conflictTransactionRepo.findAll();
        model.addAttribute("allConflicts", allConflicts);
        return "conflictTransaction";
    }

    @GetMapping(path = "/profile/conflictsTransaction{id}")
    public String showTransactionById(Model model, @PathVariable Long id){
        Optional<ConflictTransaction> conflict = conflictTransactionRepo.findById(id);
        model.addAttribute("conflict", conflict.get());
        return "conflictTransaction";
    }

    @PostMapping(path = "/profile/conflictTransaction{id}")
    public String addChangesConflictTransaction(Model model, @PathVariable Long id, ConflictTransaction conflictTransaction){
        model.addAttribute("changeConflict", conflictTransaction);
        conflictTransactionRepo.save(conflictTransaction);
        return "conflictTransaction";
    }
}
