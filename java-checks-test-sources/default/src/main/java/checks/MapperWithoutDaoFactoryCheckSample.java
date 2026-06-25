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

  @Mapper
  interface MultiLevelInheritance extends IntermediateInterface {
  }

  interface IntermediateInterface extends BaseFactoryInterface {
  }

  interface BaseFactoryInterface {
    @DaoFactory
    MultiLevelDao dao();
  }

  interface MultiLevelDao {
  }

  @Mapper
  interface MultipleInheritance extends Interface1, Interface2 {
  }

  interface Interface1 {
    String method1();
  }

  interface Interface2 {
    @DaoFactory
    MultiInheritDao dao();
  }

  interface MultiInheritDao {
  }

  @Mapper
  interface ComplexInheritance extends InterfaceWithFactory, InterfaceWithoutFactory {
  }

  interface InterfaceWithFactory {
    @DaoFactory
    ComplexDao dao();
  }

  interface InterfaceWithoutFactory {
    String getName();
  }

  interface ComplexDao {
  }

  @Mapper
  interface CircularReference extends SelfReferencingBase {
  }

  interface SelfReferencingBase {
    @DaoFactory
    CircularDao dao();
  }

  interface CircularDao {
  }

  @Mapper
  interface DeepInheritanceChain extends Level1 {
  }

  interface Level1 extends Level2 {
  }

  interface Level2 extends Level3 {
  }

  interface Level3 {
    @DaoFactory
    DeepDao dao();
  }

  interface DeepDao {
  }

  // Diamond inheritance pattern - tests visiting same interface multiple times
  @Mapper
  interface DiamondMapper extends DiamondLeft, DiamondRight {
  }

  interface DiamondLeft extends DiamondBase {
  }

  interface DiamondRight extends DiamondBase {
  }

  interface DiamondBase {
    @DaoFactory
    DiamondDao dao();
  }

  interface DiamondDao {
  }

  // Diamond without DaoFactory - tests visited set when no factory found
  @Mapper
  interface DiamondWithoutFactory extends DiamondLeftNoFactory, DiamondRightNoFactory { // Noncompliant
  }

  interface DiamondLeftNoFactory extends DiamondBaseNoFactory {
  }

  interface DiamondRightNoFactory extends DiamondBaseNoFactory {
  }

  interface DiamondBaseNoFactory {
    String getData();
  }

}
