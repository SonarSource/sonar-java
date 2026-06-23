package checks;

import com.datastax.oss.quarkus.runtime.api.mapper.Mapper;
import com.datastax.oss.quarkus.runtime.api.mapper.DaoFactory;

class MapperWithoutDaoFactoryCheckSample {

  @Mapper
  interface EmptyMapper { // Noncompliant {{Add at least one "@DaoFactory" method to this "@Mapper" interface.}}
  }

  @Mapper
  public interface FruitMapper { // Noncompliant
  }

  @Mapper
  interface MapperWithOtherMethods { // Noncompliant
    String getVersion();
    default int count() {
      return 0;
    }
  }

  @Mapper
  interface ExtendingMapper extends BaseInterface {
  }

  interface BaseInterface {
    @DaoFactory
    BaseDao baseDao();
  }

  interface BaseDao {
  }

  interface RegularInterface {
  }

  @Mapper
  public interface CompliantFruitMapper {
    @DaoFactory
    FruitDao fruitDao();
  }

  interface FruitDao {
  }

  @Mapper
  interface MultipleFactories {
    @DaoFactory
    UserDao userDao();

    @DaoFactory
    ProductDao productDao();
  }

  interface UserDao {
  }

  interface ProductDao {
  }

  @Mapper
  interface FactoryWithParameter {
    @DaoFactory
    KeyspaceDao dao(String keyspace);
  }

  interface KeyspaceDao {
  }

  @Mapper
  interface MixedMethods {
    @DaoFactory
    OrderDao orderDao();

    String getVersion();
  }

  interface OrderDao {
  }

  // Compliant: the rule only checks @Mapper interfaces.
  @Mapper
  abstract class MapperClass {
  }

  // Compliant: the rule only checks @Mapper interfaces.
  @Mapper
  enum MapperEnum {
  }

  // Compliant: the rule only checks @Mapper interfaces.
  @Mapper
  record MapperRecord() {
  }

}
