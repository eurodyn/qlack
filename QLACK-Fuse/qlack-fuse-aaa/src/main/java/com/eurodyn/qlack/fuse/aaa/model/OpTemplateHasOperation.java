package com.eurodyn.qlack.fuse.aaa.model;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

/**
 * The persistent class for the aaa_op_template_has_operation database table.
 */
@Entity
@Table(name = "aaa_op_template_has_operation")
@Getter
@Setter
public class OpTemplateHasOperation extends AAAModel {

  private static final long serialVersionUID = 1L;

  @Version
  private long dbversion;

  private boolean deny;

  //bi-directional many-to-one association to OpTemplate
  @ManyToOne
  @JoinColumn(name = "template")
  private OpTemplate template;

  //bi-directional many-to-one association to Operation
  @ManyToOne
  @JoinColumn(name = "operation")
  private Operation operation;

  //bi-directional many-to-one association to Resource
  @ManyToOne
  @JoinColumn(name = "resource_id")
  private Resource resource;

  public OpTemplateHasOperation() {
    setId(UUID.randomUUID().toString());
  }

//  public static OpTemplateHasOperation findByTemplateIDAndOperationName(String templateID,
//      String operationName, EntityManager em) {
//    Query q = em.createQuery("SELECT o FROM com.eurodyn.qlack.fuse.aaa.model.OpTemplateHasOperation o WHERE "
//        + "o.template.id = :templateID AND o.operation.name = :operationName AND o.resource IS NULL");
//    q.setParameter("templateID", templateID);
//    q.setParameter("operationName", operationName);
//    List<OpTemplateHasOperation> queryResults = q.getResultList();
//    if (queryResults.isEmpty()) {
//      return null;
//    }
//    return queryResults.get(0);
//  }

//  public static OpTemplateHasOperation findByTemplateAndResourceIDAndOperationName(
//      String templateID, String operationName, String resourceID, EntityManager em) {
//    Query q = em.createQuery("SELECT o FROM com.eurodyn.qlack.fuse.aaa.model.OpTemplateHasOperation o WHERE "
//        + "o.template.id = :templateID AND o.operation.name = :operationName AND o.resource.id = :resourceID");
//    q.setParameter("templateID", templateID);
//    q.setParameter("operationName", operationName);
//    q.setParameter("resourceID", resourceID);
//    List<OpTemplateHasOperation> queryResults = q.getResultList();
//    if (queryResults.isEmpty()) {
//      return null;
//    }
//    return queryResults.get(0);
//  }

}