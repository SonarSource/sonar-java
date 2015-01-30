import java.util.List;




class Animal {

  public List<? extends Animal> getAnimals() {        // NOK
    Collection<?> c = new ArrayList<String>();        // OK
  }

  public List<Animal> getAnimals(){                   // OK
  }

  class InnerCat {
    public List<? extends Cat> getCats() {            // NOK
    }
  }

  class InnerDog extends Animal {
    public List<? extends Animal> getAnimals() { //OK method is overriden
      return super.getAnimals();
    }
  }
}
