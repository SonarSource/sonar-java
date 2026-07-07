package checks;

import jakarta.persistence.EntityManager;

class EntityManagerMergeUnusedResultCheckSampleJakarta {

  void noncompliant(EntityManager em, Object entity) {
    em.merge(entity); // Noncompliant {{Use the return value of "merge()".}}
    // ^^^^^
  }

  void compliant(EntityManager em, Object entity) {
    Object managed = em.merge(entity); // Compliant
    return_merge(em, entity); // Compliant
  }

  Object return_merge(EntityManager em, Object entity) {
    return em.merge(entity); // Compliant
  }

  void pass_as_arg(EntityManager em, Object entity) {
    process(em.merge(entity)); // Compliant
  }

  void process(Object o) {
  }
}
