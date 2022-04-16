package main.model;

import javax.persistence.*;

/**
 * Entity that describes index table which contains all index info
 */

@Entity
@Table(name = "index_table")
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "page_id")
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "lemma_id")
    private Lemma lemma;

    @Column(name = "rank_value")
    private float rank;

    public Index() {
    }

    public Index(Page page, Lemma lemma, float rank) {
        this.page = page;
        this.lemma = lemma;
        this.rank = rank;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Lemma getLemma() {
        return lemma;
    }

    public void setLemma(Lemma lemma) {
        this.lemma = lemma;
    }

    public float getRank() {
        return rank;
    }

    public void setRank(float rank) {
        this.rank = rank;
    }
}
