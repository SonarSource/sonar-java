import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;

class A {

  String[] requestedAttrsField;
  public User lookupUser(String username, String base, String[] requestedAttrs) {

    DirContext dctx = new InitialDirContext(env);
    String[] requestedAttrsLocal = new String[12];
    SearchControls sc = new SearchControls();
    sc.setReturningAttributes(requestedAttrs);  // Noncompliant
    sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

    String filter = "(&(objectClass=user)(sAMAccountName=" + username + "))";

    NamingEnumeration results = dctx.search(base,  // Noncompliant
        filter,  // Noncompliant; parameter concatenated directly into string
        sc);
    NamingEnumeration results = dctx.search(base+"",  // Noncompliant
        filter,  // Noncompliant; parameter concatenated directly into string
        sc);
    sc.setReturningAttributes(requestedAttrsField);  // Noncompliant
    sc.setReturningAttributes(new String[]{" ", username});  // Noncompliant
    sc.setReturningAttributes(new String[]{" ", " Foo"});  // compliant
    sc.setReturningAttributes(requestedAttrsLocal);  // Noncompliant
  }

}