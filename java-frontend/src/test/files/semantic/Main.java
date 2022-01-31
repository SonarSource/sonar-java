package semantic;

class Main {
  void m(Dependency dependency, GenericDependency<String> genericDependency) {
    dependency.m(null);
    genericDependency.m(null);
  }
}
