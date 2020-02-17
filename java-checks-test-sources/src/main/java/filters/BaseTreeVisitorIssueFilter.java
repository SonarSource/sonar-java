package filters;

class BaseTreeVisitorIssueFilter {
  Integer i;
  Integer j;
  I anonymous = new I() {
    @Override
    public String bar() {
      return null;
    }
  };
  class AllowedClassName {}
}

interface I {
  String bar();
}
