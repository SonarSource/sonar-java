package checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collector;

class WildcardReturnParameterTypeCheckSample {

  class Animal {
    public List<? extends Animal> getAnimals() {        // Noncompliant
      Collection<?> c = new ArrayList<String>();        // Compliant
      return null;
    }

    public List<Animal> getAnimals2() {                   // Compliant
      return null;
    }
  }

  class Cat extends Animal {
    public List<? extends Cat> getCats() {            // Noncompliant [[sc=17;ec=18]]
      return null;
    }
  }

  class Dog extends Animal {
    public List<? extends Animal> getAnimals() { // Compliant method is overriden
      return super.getAnimals();
    }

    public List<Class<?>> getListOfClass() { // Compliant Class is ignored
      return null;
    }

    public Class<List<?>> getClassofList() { // Noncompliant
      return null;
    }

    public List<? extends Class<String>> bar() { // Noncompliant {{Remove usage of generic wildcard type.}}
      return null;
    }

    public List<? // Noncompliant
      extends List<?>> getSomething() { // Noncompliant
      return null;
    }

    private List<? extends Cat> getCats() { //Compliant private method are ignored
      return null;
    }
  }

  public Collector<Integer, ?, Integer> getCollector() { // Compliant Collector second argument is ignored, second parameter is an implementation detail
    return null;
  }

  public Collector<?, // Noncompliant
    Integer,
    ?> getCollector2() {  // Noncompliant
    return null;
  }

  public MyCollector<Integer, ?, Integer>  getMyCollector() {  // Noncompliant
    return null;
  }

  class MyCollector<A,B,C> {

  }
}
