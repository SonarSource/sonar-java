import java.util.Collection;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.AfterInvocationProvider;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.expression.SecurityExpressionOperations;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

abstract class A implements AccessDecisionVoter<Whatever> {}   // Noncompliant [[sc=29;ec=58]] {{Make sure that Permissions are controlled safely here.}}
abstract class B implements AccessDecisionManager {}           // Noncompliant
abstract class C implements AfterInvocationProvider {}         // Noncompliant
abstract class D implements PermissionEvaluator {}             // Noncompliant
abstract class E implements SecurityExpressionOperations {}    // Noncompliant
abstract class F implements MethodSecurityExpressionHandler {} // Noncompliant
abstract class G implements GrantedAuthority {}                // Noncompliant
interface H extends PermissionGrantingStrategy {}              // Noncompliant
abstract class I implements MyInterface {}

abstract class J extends GlobalMethodSecurityConfiguration {}  // Noncompliant
class K extends MyClass {}

class Whatever {

  @PreAuthorize(value = "foo") // Noncompliant 
  void foo() { }

  @PreFilter(filterTarget = "bar", value = "bar") // Noncompliant 
  void bar() { }

  @PostAuthorize(value = "qix") // Noncompliant 
  void qix() { }

  @PostFilter(value = "gul") // Noncompliant 
  void gul() { }

  @Secured("yolo") // Noncompliant 
  void yak() { }

  @Deprecated
  void lol() { }

  @MyAnnotation
  void tst(MutableAclService mas, MutableAcl ma, ObjectIdentity o, HttpSecurity httpSec) throws Exception {
    mas.createAcl(o);            // Noncompliant
    mas.deleteAcl(o, true);      // Noncompliant
    mas.updateAcl(ma);           // Noncompliant
    httpSec.authorizeRequests(); // Noncompliant

    mas.readAclById(o); // Compliant
    foo();

    GrantedAuthority myGrantedAuthority = new GrantedAuthority() { // Noncompliant [[sc=47;ec=63]] {{Make sure that Permissions are controlled safely here.}}
      @Override
      public String getAuthority() {
        return "hello";
      }
    };

    GrantedAuthority myOtherGrantedAuthority = new SimpleGrantedAuthority("foo"); // Noncompliant

    AccessDecisionManager myAccessDecisionManager = new AccessDecisionManager() { // Noncompliant [[sc=57;ec=78]] {{Make sure that Permissions are controlled safely here.}}
      @Override
      public void decide(Authentication authentication, Object object, Collection<ConfigAttribute> configAttributes) throws AccessDeniedException, InsufficientAuthenticationException { }
      @Override
      public boolean supports(ConfigAttribute attribute) { return false; }
      @Override
      public boolean supports(Class<?> clazz) { return false; }
    };

    GlobalMethodSecurityConfiguration myGlobalMethodSecurityConfiguration1 = new GlobalMethodSecurityConfiguration() { }; // Noncompliant
    GlobalMethodSecurityConfiguration myGlobalMethodSecurityConfiguration2 = new GlobalMethodSecurityConfiguration();

    MyClass myClass = new MyClass() { };
  }

  MyGrantedAuthority myOtherGrantedAuthority = new MyGrantedAuthority(); // Noncompliant {{Make sure that Permissions are controlled safely here.}}
}

class MyGrantedAuthority implements GrantedAuthority { // Noncompliant
  void foo() {
  }
}

@RolesAllowed("jsr250") // Noncompliant
class JSR_250 {
  @RolesAllowed(value = "foo") // Noncompliant [[sc=3;ec=31]] {{Make sure that Permissions are controlled safely here.}}
  void foo() { }

  @PermitAll // Noncompliant
  void bar() { }

  @DenyAll // Noncompliant
  void qix() { }
}

@interface MyAnnotation { }
interface MyInterface {}
@MyAnnotation
class MyClass {}
