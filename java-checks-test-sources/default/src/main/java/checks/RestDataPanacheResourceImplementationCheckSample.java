package checks;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheEntityResource;
import io.quarkus.hibernate.orm.rest.data.panache.PanacheRepositoryResource;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.rest.data.panache.PanacheMongoEntityResource;
import io.quarkus.mongodb.rest.data.panache.PanacheMongoRepositoryResource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.Collections;
import java.util.List;

class Person extends PanacheEntityBase {
  public Long id;
  public String name;
}

class PersonRepository implements PanacheRepositoryBase<Person, Long> {
}

class MongoPerson extends PanacheMongoEntityBase {
  public Long id;
  public String name;
}

class MongoPersonRepository implements PanacheMongoRepositoryBase<MongoPerson, Long> {
}

interface PeopleResource extends PanacheEntityResource<Person, Long> {
}

class PeopleResourceImpl implements PeopleResource { // Noncompliant {{Remove this implementation class; Quarkus generates the resource implementation automatically.}} [[sc=7;ec=25]]
}

interface PersonRepositoryResource extends PanacheRepositoryResource<PersonRepository, Person, Long> {
}

class PersonRepositoryResourceImpl implements PersonRepositoryResource { // Noncompliant [[sc=7;ec=35]]
}

interface MongoPersonResource extends PanacheMongoEntityResource<MongoPerson, Long> {
}

class MongoPersonResourceImpl implements MongoPersonResource { // Noncompliant [[sc=7;ec=30]]
}

interface MongoPersonRepositoryResource extends PanacheMongoRepositoryResource<MongoPersonRepository, MongoPerson, Long> {
}

class MongoPersonRepositoryResourceImpl implements MongoPersonRepositoryResource { // Noncompliant [[sc=7;ec=40]]
}

abstract class AbstractPersonResource implements PeopleResource { // Noncompliant [[sc=16;ec=38]]
}

class ConcretePersonResource extends AbstractPersonResource { // Noncompliant [[sc=7;ec=29]]
}

interface CompliantResource extends PanacheEntityResource<Person, Long> {
}

interface ResourceWithDefaults extends PanacheEntityResource<Person, Long> {
  @GET
  @Path("/name/{name}")
  @Produces("application/json")
  default List<Person> findByName(@PathParam("name") String name) {
    return Collections.emptyList();
  }
}

interface RegularRestInterface {
  Person get(Long id);
}

class RegularRestImpl implements RegularRestInterface {
  @Override
  public Person get(Long id) {
    return null;
  }
}

class PersonService {
  public Person findById(Long id) {
    return null;
  }
}
