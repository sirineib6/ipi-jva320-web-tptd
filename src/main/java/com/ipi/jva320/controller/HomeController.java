package com.ipi.jva320.controller;

import com.ipi.jva320.exception.SalarieException;
import com.ipi.jva320.model.SalarieAideADomicile;
import com.ipi.jva320.service.SalarieAideADomicileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private SalarieAideADomicileService salarieAideADomicileService;

    @GetMapping("/")
    public String home(ModelMap model) {
        Long employeeCount = salarieAideADomicileService.countSalaries();
        model.put("employeeCount", employeeCount);
        return "home";
    }

    @GetMapping("/salaries/{id}")
    public String salarie(@PathVariable Long id, ModelMap model) {
        SalarieAideADomicile salarie = salarieAideADomicileService.getSalarie(id);
        if (salarie == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Salarié non trouvé");
        }
        model.put("salarie", salarie);
        return "detail_Salarie";
    }

    @PostMapping("/salaries/{id}")
    public String updateSalarie(@PathVariable Long id, SalarieAideADomicile salarie) throws SalarieException {
        salarie.setId(id);
        salarieAideADomicileService.updateSalarie(salarie);
        return "redirect:/salaries/" + id;
    }

    @PostMapping("/salaries/{id}/delete")
    public String deleteSalarie(@PathVariable Long id) throws SalarieException {
        salarieAideADomicileService.deleteSalarie(id);
        return "redirect:/salaries";
    }

    @GetMapping("/salaries/search")
    public String searchSalarie(@RequestParam("nom") String nom, ModelMap model) {
        List<SalarieAideADomicile> salaries = salarieAideADomicileService.findByNom(nom);
        if (!salaries.isEmpty()) {
            model.put("salaries", salaries); // Afficher tous les résultats
            return "list"; // On suppose que "list" est la vue qui affiche les résultats
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun salarié trouvé avec ce nom");
        }
    }


    @GetMapping("/salaries/aide/new")
    public String newSalarie() {
        return "new_Salarie";
    }

    @PostMapping("/salaries/save")
    public String createSalarie(SalarieAideADomicile salarie, ModelMap model) throws SalarieException {
        SalarieAideADomicile salarieCree = salarieAideADomicileService.creerSalarieAideADomicile(salarie);
        model.put("salarie", salarieCree);
        return "redirect:/salaries/" + salarieCree.getId();
    }

    @GetMapping("/salaries")
    public String salaries(ModelMap model) {
        List<SalarieAideADomicile> salaries = salarieAideADomicileService.getSalaries();
        model.put("salaries", salaries);
        return "list";
    }

    private final SalarieAideADomicileService salarieService;

    public HomeController(SalarieAideADomicileService salarieService) {
        this.salarieService = salarieService;
    }



    @GetMapping("/salaries")
    public String searchByName(@RequestParam(name = "nom", required = false) String nom, Model model) {
        if (nom != null && !nom.isEmpty()) {
            List<SalarieAideADomicile> salaries = salarieService.findByNom(nom);
            if (salaries.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun salarié trouvé pour le nom : " + nom);
            }
            model.addAttribute("salaries", salaries);
        } else {
            // Affiche tous les salariés si aucun nom n'est fourni
            List<SalarieAideADomicile> allSalaries = salarieAideADomicileService.getSalaries();
            model.addAttribute("salaries", allSalaries);
        }
        return "list";
    }



}
