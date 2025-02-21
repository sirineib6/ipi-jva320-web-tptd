package com.ipi.jva320.service;

import com.ipi.jva320.exception.SalarieException;
import com.ipi.jva320.model.Entreprise;
import com.ipi.jva320.model.SalarieAideADomicile;
import com.ipi.jva320.repository.SalarieAideADomicileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SalarieAideADomicileService {

    @Autowired
    private SalarieAideADomicileRepository salarieAideADomicileRepository;

    public SalarieAideADomicileService() throws SalarieException {
    }

    /**
     * @return le nombre de salariés dans la base
     */
    public Long countSalaries() {
        return salarieAideADomicileRepository.count();
    }

    /**
     * @return le nombre de salariés dans la base
     */
    public List<SalarieAideADomicile> getSalaries() {
        return StreamSupport.stream(salarieAideADomicileRepository.findAll().spliterator(), false)
                .collect(Collectors.toList());
    }

    /**
     * @return le nombre de salariés dans la base
     */
    public List<SalarieAideADomicile> getSalaries(String nom) {
        return salarieAideADomicileRepository.findAllByNom(nom, null);
    }

    /**
     * @return le nombre de salariés dans la base
     */
    public List<SalarieAideADomicile> getSalaries(String nom, Pageable pageable) {
        return salarieAideADomicileRepository.findAllByNom(nom, pageable);
    }

    /**
     * @return le nombre de salariés dans la base
     */
    public Page<SalarieAideADomicile> getSalaries(Pageable pageable) {
        return salarieAideADomicileRepository.findAll(pageable);
    }

    /**
     * @return le salarie
     */
    public SalarieAideADomicile getSalarie(Long id) {
        Optional<SalarieAideADomicile> res = salarieAideADomicileRepository.findById(id);
        return res.isEmpty() ? null : res.get();
    }

    /**
     * Créée un nouveau salarié en base de données.
     * @param salarieAideADomicile à créer
     * @return salarieAideADomicile créé (avec son id en base)
     * @throws SalarieException si son nom est déjà pris ou si l'id est fourni TODO NON
     */
    public SalarieAideADomicile creerSalarieAideADomicile(SalarieAideADomicile salarieAideADomicile)
            throws SalarieException, EntityExistsException {
        if (salarieAideADomicile.getId() != null) {
            throw new SalarieException("L'id ne doit pas être fourni car il est généré");
        }
        /*Optional<SalarieAideADomicile> existantOptional = salarieAideADomicileRepository.findById(salarieAideADomicile.getId());
        if (!existantOptional.isEmpty()) {
            throw new SalarieException("Un salarié existe déjà avec l'id " + existant.getId()); // TODO id ou nom ??
        }*/
        return salarieAideADomicileRepository.save(salarieAideADomicile);
    }

    public SalarieAideADomicile updateSalarieAideADomicile(SalarieAideADomicile salarieAideADomicile)
            throws SalarieException, EntityExistsException {
        if (salarieAideADomicile.getId() == null) {
            throw new SalarieException("L'id doit être fourni");
        }
        Optional<SalarieAideADomicile> existantOptional = salarieAideADomicileRepository.findById(salarieAideADomicile.getId());
        if (existantOptional.isEmpty()) {
            throw new SalarieException("Le salarié n'existe pas déjà d'id " + salarieAideADomicile.getId()); // TODO id ou nom ??
        }
        return salarieAideADomicileRepository.save(salarieAideADomicile);
    }

    public void deleteSalarieAideADomicile(Long id)
            throws SalarieException, EntityExistsException {
        if (id == null) {
            throw new SalarieException("L'id doit être fourni");
        }
        Optional<SalarieAideADomicile> existantOptional = salarieAideADomicileRepository.findById(id);
        if (existantOptional.isEmpty()) {
            throw new SalarieException("Le salarié n'existe pas déjà d'id " + id); // TODO id ou nom ??
        }
        salarieAideADomicileRepository.deleteById(id);
    }


    public long calculeLimiteEntrepriseCongesPermis(LocalDate moisEnCours, double congesPayesAcquisAnneeNMoins1,
                                                    LocalDate moisDebutContrat,
                                                    LocalDate premierJourDeConge, LocalDate dernierJourDeConge) {
        // proportion selon l'avancement dans l'année, pondérée avec poids plus gros sur juillet et août (20 vs 8) :
        double proportionPondereeDuConge = Math.max(Entreprise.proportionPondereeDuMois(premierJourDeConge),
                Entreprise.proportionPondereeDuMois(dernierJourDeConge));
        double limiteConges = proportionPondereeDuConge * congesPayesAcquisAnneeNMoins1;

        Double partCongesPrisTotauxAnneeNMoins1 = salarieAideADomicileRepository.partCongesPrisTotauxAnneeNMoins1();

        double proportionMoisEnCours = ((premierJourDeConge.getMonthValue()
                - Entreprise.getPremierJourAnneeDeConges(moisEnCours).getMonthValue()) % 12) / 12d;
        double proportionTotauxEnRetardSurLAnnee = proportionMoisEnCours - partCongesPrisTotauxAnneeNMoins1;
        limiteConges += proportionTotauxEnRetardSurLAnnee * 0.2 * congesPayesAcquisAnneeNMoins1;

        // marge supplémentaire de 10% du nombre de mois jusqu'à celui du dernier jour de congé
        int distanceMois = (dernierJourDeConge.getMonthValue() - moisEnCours.getMonthValue()) % 12;
        limiteConges += limiteConges * 0.1 * distanceMois / 12;

        // année ancienneté : bonus jusqu'à 10
        int anciennete = moisEnCours.getYear() - moisDebutContrat.getYear();
        limiteConges += Math.min(anciennete, 10);

        // arrondi pour éviter les miettes de calcul en Double :
        BigDecimal limiteCongesBd = new BigDecimal(Double.toString(limiteConges));
        limiteCongesBd = limiteCongesBd.setScale(3, RoundingMode.HALF_UP);
        return Math.round(limiteCongesBd.doubleValue());
    }


    /**
     * Calcule les jours de congés à décompter (par calculeJoursDeCongeDecomptesPourPlage()),
     * et si valide (voir plus bas) les décompte au salarié et le sauve en base de données
     * @param salarieAideADomicile TODO nom ?
     * @param jourDebut
     * @param jourFin peut être dans l'année suivante mais uniquement son premier jour
     * @throws SalarieException si pas de jour décompté, ou avant le mois en cours, ou dans l'année suivante
     * (hors l'exception du premier jour pour résoudre le cas d'un samedi), ou la nouvelle totalité
     * des jours de congé pris décomptés dépasse le nombre acquis en N-1 ou la limite de l'entreprise
     * (calculée par calculeLimiteEntrepriseCongesPermis())
     */
    public void ajouteConge(SalarieAideADomicile salarieAideADomicile, LocalDate jourDebut, LocalDate jourFin)
            throws SalarieException {
        if (!salarieAideADomicile.aLegalementDroitADesCongesPayes()) {
            throw new SalarieException("N'a pas légalement droit à des congés payés !");
        }

        LinkedHashSet<LocalDate> joursDecomptes = salarieAideADomicile
                .calculeJoursDeCongeDecomptesPourPlage(jourDebut, jourFin);

        if (joursDecomptes.size() == 0) {
            throw new SalarieException("Pas besoin de congés !");
        }

        // on vérifie que le congé demandé est dans les mois restants de l'année de congés en cours du salarié :
        if (joursDecomptes.stream().findFirst().get()
                .isBefore(salarieAideADomicile.getMoisEnCours())) {
            throw new SalarieException("Pas possible de prendre de congé avant le mois en cours !");
        }
        LinkedHashSet<LocalDate> congesPayesPrisDecomptesAnneeN = new LinkedHashSet<>(joursDecomptes.stream()
                .filter(d -> !d.isAfter(LocalDate.of(Entreprise.getPremierJourAnneeDeConges(
                        salarieAideADomicile.getMoisEnCours()).getYear() + 1, 5, 31)))
                .collect(Collectors.toList()));
        int nbCongesPayesPrisDecomptesAnneeN = congesPayesPrisDecomptesAnneeN.size();
        if (joursDecomptes.size() > nbCongesPayesPrisDecomptesAnneeN + 1) {
            // NB. 1 jour dans la nouvelle année est toujours toléré, pour résoudre le cas d'un congé devant se finir un
            // samedi le premier jour de la nouvelle année de congés...
            throw new SalarieException("Pas possible de prendre de congé dans l'année de congés suivante (hors le premier jour)");
        }

        if (nbCongesPayesPrisDecomptesAnneeN > salarieAideADomicile.getCongesPayesRestantAnneeNMoins1()) {
            throw new SalarieException("Conges Payes Pris Decomptes (" + nbCongesPayesPrisDecomptesAnneeN
                    + ") dépassent les congés acquis en année N-1 : "
                    + salarieAideADomicile.getCongesPayesRestantAnneeNMoins1());
        }

        double limiteEntreprise = this.calculeLimiteEntrepriseCongesPermis(
                salarieAideADomicile.getMoisEnCours(),
                salarieAideADomicile.getCongesPayesAcquisAnneeNMoins1(),
                salarieAideADomicile.getMoisDebutContrat(),
                jourDebut, jourFin);
        if (nbCongesPayesPrisDecomptesAnneeN < limiteEntreprise) {
            throw new SalarieException("Conges Payes Pris Decomptes (" + nbCongesPayesPrisDecomptesAnneeN
                    + ") dépassent la limite des règles de l'entreprise : " + limiteEntreprise);
        }

        salarieAideADomicile.getCongesPayesPris().addAll(joursDecomptes);
        salarieAideADomicile.setCongesPayesPrisAnneeNMoins1(nbCongesPayesPrisDecomptesAnneeN);

        salarieAideADomicileRepository.save(salarieAideADomicile);
    }


    public void clotureMois(SalarieAideADomicile salarieAideADomicile, double joursTravailles) throws SalarieException {
        // incrémente les jours travaillés de l'année N du salarié de celles passées en paramètres
        salarieAideADomicile.setJoursTravaillesAnneeN(salarieAideADomicile.getJoursTravaillesAnneeN() + joursTravailles);

        salarieAideADomicile.setCongesPayesAcquisAnneeN(salarieAideADomicile.getCongesPayesAcquisAnneeN()
                + salarieAideADomicile.CONGES_PAYES_ACQUIS_PAR_MOIS);

        salarieAideADomicile.setMoisEnCours(salarieAideADomicile.getMoisEnCours().plusMonths(1));

        if (salarieAideADomicile.getMoisEnCours().getMonth().getValue() == 6) {
            clotureAnnee(salarieAideADomicile);
        }

        salarieAideADomicileRepository.save(salarieAideADomicile);
    }


    void clotureAnnee(SalarieAideADomicile salarieAideADomicile) {
        salarieAideADomicile.setJoursTravaillesAnneeNMoins1(salarieAideADomicile.getJoursTravaillesAnneeN());
        salarieAideADomicile.setCongesPayesAcquisAnneeNMoins1(salarieAideADomicile.getCongesPayesAcquisAnneeN());
        salarieAideADomicile.setCongesPayesPrisAnneeNMoins1(0);
        salarieAideADomicile.setJoursTravaillesAnneeN(0);
        salarieAideADomicile.setCongesPayesAcquisAnneeN(0);

        // on ne garde que les jours de congés pris sur la nouvelle année (voir ajouteCongés()) :
        salarieAideADomicile.setCongesPayesPris(new LinkedHashSet<>(salarieAideADomicile.getCongesPayesPris().stream()
                .filter(d -> d.isAfter(LocalDate.of(Entreprise.getPremierJourAnneeDeConges(
                        salarieAideADomicile.getMoisEnCours()).getYear(), 5, 31)))
                .collect(Collectors.toList())));

        salarieAideADomicileRepository.save(salarieAideADomicile);
    }

    public SalarieAideADomicile updateSalarie(SalarieAideADomicile salarieAideADomicile) throws SalarieException {
        if (salarieAideADomicile.getId() == null) {
            throw new SalarieException("L'id doit être fourni");
        }
        Optional<SalarieAideADomicile> existant = salarieAideADomicileRepository.findById(salarieAideADomicile.getId());
        if (existant.isEmpty()) {
            throw new SalarieException("Le salarié n'existe pas");
        }
        return salarieAideADomicileRepository.save(salarieAideADomicile);
    }

    public void deleteSalarie(Long id) throws SalarieException {
        Optional<SalarieAideADomicile> existant = salarieAideADomicileRepository.findById(id);
        if (existant.isEmpty()) {
            throw new SalarieException("Le salarié avec l'id " + id + " n'existe pas");
        }
        salarieAideADomicileRepository.deleteById(id);
    }
    public Optional<SalarieAideADomicile> getOptionalSalarie(Long id) {
        return salarieAideADomicileRepository.findById(id);
    }


    public SalarieAideADomicile findSalarieById(Long id) throws SalarieException {
        Optional<SalarieAideADomicile> optionalSalarie = salarieAideADomicileRepository.findById(id);
        return optionalSalarie.orElseThrow(() -> new SalarieException("Salarie non trouvé"));
    }

    public List<SalarieAideADomicile> findByNom(String nom) {
        return salarieAideADomicileRepository.findByNomContainingIgnoreCase(nom); // Supposant que la méthode existe dans le repository
    }

    public interface SalarieRepository extends JpaRepository<SalarieAideADomicile, Long> {
        List<SalarieAideADomicile> findByNomContainingIgnoreCase(String nom);
    }

}



