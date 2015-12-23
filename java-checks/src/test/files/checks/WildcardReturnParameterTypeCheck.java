import java.util.List;




class Animal {

  public List<? extends Animal> getAnimals() {        // Noncompliant
    Collection<?> c = new ArrayList<String>();        // Compliant
  }

  public List<Animal> getAnimals(){                   // Compliant
  }

  class InnerCat {
    public List<? extends Cat> getCats() {            // Noncompliant [[sc=17;ec=18]]
    }
  }

  class InnerDog extends Animal {
    public List<? extends Animal> getAnimals() { // Compliant method is overriden
      return super.getAnimals();
   }

   public List<Class<?>> foo() {} // Compliant Class is ignored
   public List<? extends Class<String>> bar() {} // Noncompliant {{Remove usage of generic wildcard type.}}
   private List<? extends Cat> getCats() { //Compliant private method are ignored

    }
  }
}
