import java.io.Closeable;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

class Inheritance {

  interface Animal extends Comparable<Animal> {
    String name();

    default String greet() {
      return "I am " + name();
    }

    @Override
    default int compareTo(Animal other) {
      return name().compareTo(other.name());
    }
  }

  abstract static class Base implements Animal, Closeable {
    protected int legs;

    protected abstract void move();

    @Override
    public String name() {
      return getClass().getSimpleName();
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean equals(Object other) {
      return super.equals(other);
    }

    @Override
    public int hashCode() {
      return legs;
    }
  }

  static class Dog extends Base {
    @Override
    protected void move() {
      legs = 4;
    }

    @Override
    public String greet() {
      return "Woof " + super.greet();
    }

    @Override
    public String toString() {
      return name();
    }
  }

  static final class Puppy extends Dog {
    @Override
    protected void move() {
      super.move();
    }
  }

  static class Names extends AbstractList<String> {
    @Override
    public String get(int index) {
      return "n" + index;
    }

    @Override
    public int size() {
      return 3;
    }

    @Override
    public Iterator<String> iterator() {
      return super.iterator();
    }
  }

  static class Mapper<T> implements Function<T, String>, Comparator<T> {
    @Override
    public String apply(T value) {
      return String.valueOf(value);
    }

    @Override
    public int compare(T left, T right) {
      return apply(left).compareTo(apply(right));
    }
  }

  void use(Dog dog, Puppy puppy, Names names, Mapper<Integer> mapper) throws IOException {
    dog.move();
    puppy.move();
    dog.greet();
    puppy.greet();
    dog.close();
    names.get(0);
    names.size();
    names.iterator().next();
    mapper.apply(1);
    mapper.compare(1, 2);
    mapper.reversed();
    List<String> view = names.subList(0, 1);
    view.size();
    Animal animal = dog;
    animal.compareTo(puppy);
  }
}
