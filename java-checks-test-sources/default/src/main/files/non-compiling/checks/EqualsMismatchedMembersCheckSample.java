package checks;

class EqualsMismatchedMembersCheckSample {

  class UnknownMembers {
    UnknownType a;
    UnknownType b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof UnknownMembers that)) {
        return false;
      }
      return a == that.b && Unknown.equal(a, that.a); // Noncompliant
    }
  }

  class UnknownReceiver {
    int a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof UnknownReceiver that)) {
        return false;
      }
      return this.a == unknown.b && this.a == that.a;
    }
  }

  class VoidGetter {
    int a;
    int b;

    void getA() {
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof VoidGetter that)) {
        return false;
      }
      return getA() == that.b;
    }
  }
}
