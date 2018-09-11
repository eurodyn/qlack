package com.eurodyn.qlack.fuse.mailing.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mai_distribution_list")
@Getter
@Setter
public class DistributionList implements java.io.Serializable {

  @Id
  private String id;

  @Column(name = "list_name", nullable = false, length = 45)
  private String name;

  @Column(name = "description", length = 45)
  private String description;

  @Column(name = "created_by", length = 254)
  private String createdBy;

  @Column(name = "created_on")
  private Long createdOn;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "mai_distr_list_has_contact",
      joinColumns = {
          @JoinColumn(name = "distribution_list_id", nullable = false, updatable = false)
      },
      inverseJoinColumns = {
          @JoinColumn(name = "contact_id", nullable = false, updatable = false)
      }
  )
  private Set<Contact> contacts = new HashSet<Contact>(0);

  // -- Constructors

  public DistributionList() {
    this.id = java.util.UUID.randomUUID().toString();
  }

  // -- Queries

  public static List<DistributionList> findAll(EntityManager em) {
    String jpql = "SELECT dl FROM DistributionList dl";

    return em.createQuery(jpql, DistributionList.class).getResultList();
  }

  public static List<DistributionList> findByName(EntityManager em, String name) {
    String jpql = "SELECT dl FROM DistributionList dl WHERE dl.name = :name";

    return em.createQuery(jpql, DistributionList.class).setParameter("name", name)
        .getResultList();
  }

}
