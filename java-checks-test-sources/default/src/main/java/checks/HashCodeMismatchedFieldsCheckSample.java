package checks;

import java.util.Map;
import java.util.Objects;

class HashCodeMismatchedFieldsCheckSample {

  static class OrderKey {
    private final long id;
    private final int version;

    OrderKey(long id, int version) {
      this.id = id;
      this.version = version;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof OrderKey key && id == key.id;
    }

    @Override
    public int hashCode() { // Noncompliant {{This hashCode() implementation is inconsistent with equals(): it reads "version", which equals() never reads, so equal objects may hash differently.}}
      return Objects.hash(id, version); // secondary=-1
    }
  }

  static class CompliantOrderKey {
    private final long id;
    private final int version;

    CompliantOrderKey(long id, int version) {
      this.id = id;
      this.version = version;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof CompliantOrderKey key && id == key.id && version == key.version;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, version);
    }
  }

  static class Person {
    private final String firstName;
    private final String lastName;

    Person(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    String getFirstName() {
      return firstName;
    }

    String getLastName() {
      return lastName;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Person person && getFirstName().equals(person.getFirstName());
    }

    @Override
    public int hashCode() { // Noncompliant {{This hashCode() implementation is inconsistent with equals(): it reads "lastName", which equals() never reads, so equal objects may hash differently.}}
      return Objects.hash(getFirstName(), getLastName()); // secondary=-1
    }
  }

  static class CompliantPerson {
    private final String firstName;
    private final String lastName;

    CompliantPerson(String firstName, String lastName) {
      this.firstName = firstName;
      this.lastName = lastName;
    }

    String getFirstName() {
      return firstName;
    }

    String getLastName() {
      return lastName;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof CompliantPerson person
        && getFirstName().equals(person.getFirstName())
        && getLastName().equals(person.getLastName());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getFirstName(), getLastName());
    }
  }

  static class MultipleMismatches {
    private final long id;
    private final int version;
    private final String tag;

    MultipleMismatches(long id, int version, String tag) {
      this.id = id;
      this.version = version;
      this.tag = tag;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof MultipleMismatches that && id == that.id;
    }

    @Override
    public int hashCode() { // Noncompliant {{This hashCode() implementation is inconsistent with equals(): it reads "tag", "version", which equals() never reads, so equal objects may hash differently.}} [[secondary=-1,-1]]
      return Objects.hash(id, version, tag);
    }
  }

  static class MemoizedHash {
    private final int x;
    private final int y;
    private int cachedHash;

    MemoizedHash(int x, int y) {
      this.x = x;
      this.y = y;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof MemoizedHash point && x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
      if (cachedHash == 0) {
        cachedHash = Objects.hash(x, y);
      }
      return cachedHash;
    }
  }

  static class ComplexHashCodeHelper {
    private final Map<String, String> values;

    ComplexHashCodeHelper(Map<String, String> values) {
      this.values = values;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ComplexHashCodeHelper that && values.equals(that.values);
    }

    @Override
    public int hashCode() {
      return computeHash();
    }

    private int computeHash() {
      return values.entrySet().stream().mapToInt(Object::hashCode).sum();
    }
  }

  static class ComplexEqualsHelper {
    private final Map<String, String> values;

    ComplexEqualsHelper(Map<String, String> values) {
      this.values = values;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ComplexEqualsHelper that && sameValues(that);
    }

    private boolean sameValues(ComplexEqualsHelper that) {
      return values.entrySet().stream().allMatch(e -> Objects.equals(e.getValue(), that.values.get(e.getKey())));
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }
  }

  static class OnlyEquals {
    private final long id;

    OnlyEquals(long id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof OnlyEquals that && id == that.id;
    }
  }

  static class OnlyHashCode {
    private final long id;

    OnlyHashCode(long id) {
      this.id = id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

  static class ExtraInEquals {
    private final long id;
    private final int version;

    ExtraInEquals(long id, int version) {
      this.id = id;
      this.version = version;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ExtraInEquals that && id == that.id && version == that.version;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

  static class ReferenceEquality {
    private final long id;

    ReferenceEquality(long id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object other) {
      return this == other;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id);
    }
  }

  static class StaticFieldInHashCode {
    private static int instanceCount;
    private final long id;

    StaticFieldInHashCode(long id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof StaticFieldInHashCode that && id == that.id;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, instanceCount);
    }
  }

  static class BaseWithField {
    protected final long baseId;

    BaseWithField(long baseId) {
      this.baseId = baseId;
    }
  }

  static class InheritedFieldUser extends BaseWithField {
    private final int localId;

    InheritedFieldUser(long baseId, int localId) {
      super(baseId);
      this.localId = localId;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof InheritedFieldUser that && localId == that.localId;
    }

    @Override
    public int hashCode() {
      return Objects.hash(localId, baseId);
    }
  }

  abstract static class AbstractPair {
    abstract boolean equals(Object other);

    abstract int hashCode();
  }

  interface HasIdentity {
    boolean equals(Object other);

    int hashCode();
  }
}
