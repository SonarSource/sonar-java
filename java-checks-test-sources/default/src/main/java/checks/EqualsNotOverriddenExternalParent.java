package checks;

public class EqualsNotOverriddenExternalParent {
  String name;

  @Override
  public boolean equals(Object obj) {
    return obj instanceof EqualsNotOverriddenExternalParent;
  }
}
