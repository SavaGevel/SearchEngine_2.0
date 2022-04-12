package main.repositories;

import main.model.Index;
import main.model.Lemma;
import main.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndexRepository extends JpaRepository<Index, Integer> {

    Index findIndexByPageAndLemma(Page page, Lemma lemma);

}
