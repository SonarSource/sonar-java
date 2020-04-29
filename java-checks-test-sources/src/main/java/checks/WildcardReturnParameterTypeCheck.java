package checks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class WildcardReturnParameterTypeCheck {

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

    public List<Class<?>> foo() { // Compliant Class is ignored
      return null;
    }

    public List<? extends Class<String>> bar() { // Noncompliant {{Remove usage of generic wildcard type.}}
      return null;
    }

    private List<? extends Cat> getCats() { //Compliant private method are ignored
      return null;
    }
  }
}
