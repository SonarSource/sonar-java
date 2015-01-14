@SuppressWarnings("all")
class Accessibility {
  private Object foo;

  private class Example1 {
    class Member {
    }
    class Superclass {
      class Member {
      }
    }

    class Subclass extends Superclass {
      class Target extends Member { // Member = Superclass.Member
      }
    }
  }

  private class Example2 {
    class Member {
    }

    class Superclass {
      private class Member {
      }
    }

    class Subclass extends Superclass {
      class Target extends Member { // Member = Example2.Member
      }
    }
  }

}

class Plop extends Accessibility {
      void fun() {
        foo.getClass();
      }
}
