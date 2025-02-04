package checks.spring;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

public interface UsePageableParameterForPagedQueryCheckSample extends Repository<Entity, String> {
  Page<Entity> findByName(String name); // Noncompliant {{Add a "Pageable" parameter to this method to support pagination.}}
//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  Slice<Entity> findByLastName(String lastName); // Noncompliant {{Add a "Pageable" parameter to this method to support pagination.}}

  Page<Entity> findByName(String name, Pageable pageable); // Compliant

  Slice<Entity> findByLastName(String lastName, Pageable pageable); // Compliant

  Page<Entity> getAll(MyPageable pageable); // Compliant
}

interface CrudRepo extends CrudRepository<Entity, String> {
  Page<Entity> findByName(String name); // Noncompliant {{Add a "Pageable" parameter to this method to support pagination.}}

  Page<Entity> findByName(String name, Pageable pageable); // Compliant
}

interface StandardInterface {
  Slice<Entity> findByLastName(String lastName);
}

class RepoImpl implements Repository<Entity, String> {
  Page<Entity> findByName(String name) { // Compliant, this is a custom implementation and will not cause issues to spring
    return null;
  }
}

class AClass {
  public Page<Entity> findByName(String name) { // Compliant, not a spring repository
    return null;
  }
}

class Entity {
}

interface MyPageable extends Pageable {
}
