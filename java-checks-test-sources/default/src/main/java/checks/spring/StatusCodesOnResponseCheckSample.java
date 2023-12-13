package checks.spring;

import javax.ws.rs.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

public class StatusCodesOnResponseCheckSample {

  class User {
  }

  @Controller
  class UserController {

    public ResponseEntity<User> getOkUser() {
      return ResponseEntity.ok(new User()); // Compliant
    }

    public ResponseEntity<User> getBadReqUser() {
      return ResponseEntity.badRequest().build(); // Compliant
    }

    public ResponseEntity<User> getUserNoncompliant() {

      try {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(new User()); // Noncompliant [[sc=16;ec=60]] {{Use the "ResponseEntity.ok()" method or set the status to
                                                                              // "HttpStatus.OK".}}
      } catch (NotFoundException e) {
        return ResponseEntity.status(OK).build(); // Noncompliant [[sc=16;ec=41]] {{Use the "ResponseEntity.badRequest()" or "ResponseEntity.notFound()" method or set the
                                                  // status to "HttpStatus.INTERNAL_SERVER_ERROR" or "HttpStatus.NOT_FOUND".}}
      } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Compliant
      }
    }

    public ResponseEntity<User> getUserNoncompliant2() {
      try {
        return ResponseEntity.badRequest().build(); // Noncompliant
      } catch (NotFoundException e) {
        return ResponseEntity.ok().build(); // Noncompliant
      } catch (Exception e) {
        return ResponseEntity.status(getHttpStatus()).build(); // Compliant
      }
    }

    public ResponseEntity<User> getUserNoncompliant3() {
      try {
        return ResponseEntity.notFound().build(); // Noncompliant
      } catch (NotFoundException e) {
        return ResponseEntity.ok().build(); // Noncompliant
      } catch (Exception e) {
        return ResponseEntity.notFound().build(); // Compliant
      }
    }

    public ResponseEntity<User> getUser() {
      return ResponseEntity.status(OK).build(); // Compliant
    }

    public ResponseEntity<User> getUser2() {
      return ResponseEntity.status(INTERNAL_SERVER_ERROR).build(); // Compliant
    }

    public ResponseEntity<User> getUserCompliant() {
      try {
        return ResponseEntity.ok(new User()); // Compliant
      } catch (NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Compliant
      } catch (Exception e) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR).build(); // Compliant
      }
    }

    public ResponseEntity<User> getUserCompliant2() {
      try {
        return ResponseEntity.status(OK).build(); // Compliant
      } catch (NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Compliant
      }
    }
  }

  private HttpStatus getHttpStatus() {
    return HttpStatus.NOT_FOUND;
  }

}
