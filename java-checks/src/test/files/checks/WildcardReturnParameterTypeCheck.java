import java.util.List;




class Animal {

  public List<? extends Animal> getAnimals() {        // NOK
    Collection<?> c = new ArrayList<String>();        // Compliant
  }

  public List<Animal> getAnimals(){                   // Compliant
  }

  class InnerCat {
    public List<? extends Cat> getCats() {            // NonCompliant
    }
  }

  class InnerDog extends Animal {
    public List<? extends Animal> getAnimals() { // Compliant method is overriden
      return super.getAnimals();
   }

   public List<Class<?>> foo() {} // Compliant Class is ignored
   public List<? extends Class<String>> bar() {} // NonCompliant wildcard is not in Class
   private List<? extends Cat> getCats() { //Compliant private method are ignored

    }
  }
}
