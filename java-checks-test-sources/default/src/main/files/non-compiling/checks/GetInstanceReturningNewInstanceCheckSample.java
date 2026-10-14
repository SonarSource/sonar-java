package checks;

class GetInstanceReturningNewInstanceCheckSample {

  static class UnresolvedCreatedType {

    static UnresolvedCreatedType getInstance() {
      return new Unknown();
    }
  }
}
