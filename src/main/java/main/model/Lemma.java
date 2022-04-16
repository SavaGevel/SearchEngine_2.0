package main.model;


import javax.persistence.*;

/**
 * Entity that describes lemma table which contains all lemmas info
 */

@Entity
@Table(name = "lemma")
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = false)
    private String lemma;

    private int frequency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    public Lemma() {
    }

    public Lemma(String lemma, int frequency, Site site) {
        this.lemma = lemma;
        this.frequency = frequency;
        this.site = site;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

}
