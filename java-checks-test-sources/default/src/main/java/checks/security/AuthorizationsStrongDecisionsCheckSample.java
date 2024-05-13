package checks.security;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.function.Predicate;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

public class AuthorizationsStrongDecisionsCheckSample {

  // AccessDecisionVoter =================================================

  class WeakNightVoter implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Noncompliant {{"vote" method should return at least one time ACCESS_DENIED.}}
//             ^^^^
      Calendar calendar = Calendar.getInstance();
      int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
      if(currentHour >= 8 && currentHour <= 19) {
        return ACCESS_GRANTED;
      }
      // when users connect during the night, do not make decision
      return ACCESS_ABSTAIN;
    }

    public int vote(Object object, Collection collection) { // Compliant, not a vote method
      return ACCESS_GRANTED;
    }

    @Override
    public boolean supports(ConfigAttribute configAttribute) {
      return false;
    }

    @Override
    public boolean supports(Class aClass) {
      return false;
    }
  }

  class StrongNightVoter implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Compliant
      Calendar calendar = Calendar.getInstance();
      int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
      if(currentHour >= 8 && currentHour <= 19) {
        return ACCESS_GRANTED;
      }
      // employees are not allowed to connect during the night
      return ACCESS_DENIED; // Compliant
    }

    @Override
    public boolean supports(ConfigAttribute attribute) {
      return true;
    }

    @Override
    public boolean supports(Class clazz) {
      return true;
    }
  }

  abstract class OneNonConditionalReturn implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) {
      return getVote(authentication); // FN, single non-trivial return, do not report to avoid FP
    }

    private int getVote(Authentication authentication) {
      if (authentication.isAuthenticated()) {
        return ACCESS_GRANTED;
      } else {
        return ACCESS_ABSTAIN;
      }
    }
  }

  abstract class OneComplexReturn implements AccessDecisionVoter {
    private String targetDomainObject;

    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Compliant, FN, we don't detect that getVote does not return ACCESS_ABSTAIN
      if(targetDomainObject.endsWith("target")) {
        return getVote(authentication);
      }
      return ACCESS_GRANTED;
    }

    private int getVote(Authentication authentication) {
      if (authentication.isAuthenticated()) {
        return ACCESS_GRANTED;
      } else {
        return ACCESS_ABSTAIN;
      }
    }
  }

  abstract class ReturnVariableCompliant implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Compliant
      int access = ACCESS_DENIED;
      if(testAccess(authentication, object, collection)) {
        access = ACCESS_GRANTED;
      }
      return access;
    }
  }

  abstract class ReturnVariableGranted implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Compliant, FN
      int access = ACCESS_ABSTAIN;
      if(testAccess(authentication, object, collection)) {
        access = ACCESS_GRANTED;
      }
      return access;
    }
  }

  abstract class ReturnVariableComplex implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Compliant
      int access = ACCESS_ABSTAIN;
      if(testAccess(authentication, object, collection)) {
        access = getVote(authentication);
      }
      return access;
    }

    protected abstract int getVote(Authentication authentication);
  }

  abstract class ReturnQualifiedName implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Noncompliant
      if(testAccess(authentication, object, collection)) {
        return AccessDecisionVoter.ACCESS_GRANTED;
      } else if (collection.isEmpty()) {
        return ReturnQualifiedName.ACCESS_ABSTAIN;
      }
      return AccessDecisionVoter.ACCESS_ABSTAIN;
    }
  }

  // Return literals, but nothing complex, still make sense to recommend returning ACCESS_DENIED, and not own int.
  abstract class ReturnSomethingElse implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Noncompliant
      if (collection.isEmpty()) {
        return 1;
      } else if (object.equals("a")) {
        return -1;
      }
      return ACCESS_GRANTED;
    }
  }

  abstract class ThrowException implements AccessDecisionVoter {
    @Override
    public int vote(Authentication authentication, Object object, Collection collection) { // Compliant, Exceptions are considered as strong decision
      if (collection.isEmpty()) {
        throw new UnauthorizedException();
      }
      return ACCESS_GRANTED;
    }
  }

  // PermissionEvaluator =================================================

  class MyPermissionEvaluatorNonCompliant implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) { // Noncompliant {{"hasPermission" method should return at least one time false.}}
//                 ^^^^^^^^^^^^^
      Object user = authentication.getPrincipal();
      if(user.equals(permission)) {
        return true;
      }
      return true;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) { // Noncompliant
      Object user = authentication.getPrincipal();
      if(user.equals(s)) {
        return true;
      }
      return true;
    }
  }

  class MyPermissionEvaluatorCompliant implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) { // Compliant
      Object user = authentication.getPrincipal();
      if(user.equals(permission)) {
        return true;
      }
      return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) { // Compliant
      Object user = authentication.getPrincipal();
      if(user.equals(s)) {
        return false;
      }
      return true;
    }
  }

  class OneTrivialReturn implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) { // Compliant
      return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) { // Noncompliant
      return true;
    }
  }

  class NonTrivialReturn implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) { // Compliant
      return getPermission(authentication);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) { // Compliant
      if (s.endsWith("s")) {
        return true;
      } else {
        return getPermission(authentication);
      }
    }

    private boolean getPermission(Authentication authentication) {
      if (authentication.isAuthenticated()) {
        return true;
      } else {
        return false;
      }
    }
  }

  class NonTrivialReturn2 implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) { // Compliant
      boolean hasPermission = true;
      if (permission.equals("+")) {
        hasPermission = false;
      }
      return hasPermission;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) {
      // Compliant, FN, but boolean variable always equals to true will be reported by others rules
      boolean hasPermission = true;
      if (s.equals("+")) {
        hasPermission = true;
      }
      return hasPermission;
    }
  }

  class ReturnInOtherScope implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object o, Object o1) { // Noncompliant
      if (o.equals(o1)) {
        Predicate<String> p = s -> {
          return false; // Not in the same scope!
        };
        return true;
      }
      return true;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) { // Noncompliant
      if (o.equals(s)) {
        PermissionEvaluator evaluator = new PermissionEvaluator() {
          @Override
          public boolean hasPermission(Authentication authentication, Object o, Object o1) { // Noncompliant
            return true;
          }

          @Override
          public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) { // Compliant
            return false;
          }
        };
        return true;
      }
      return true;
    }
  }

  class ThrowsException implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object o, Object o1) {   // Compliant, Exceptions are considered as strong decision
      if (o1.equals("+")) {
        throw new UnauthorizedException();
      }
      return true;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) {   // Compliant, Exceptions are considered as strong decision
      if (s.equals("+")) {
        throw new UnauthorizedException();
      }
      return true;
    }
  }

  boolean testAccess(Authentication authentication, Object object, Collection collection) {
    return true;
  }

  private class UnauthorizedException extends RuntimeException {
  }
}
