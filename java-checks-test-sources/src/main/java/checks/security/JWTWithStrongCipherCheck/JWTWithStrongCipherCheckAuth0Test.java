package checks.security.JWTWithStrongCipherCheck;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureGenerationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Base64;
import java.util.Date;

import static com.auth0.jwt.algorithms.Algorithm.none;
import static org.apache.commons.lang.time.DateUtils.addMinutes;

/**
 * JSON web token handling with https://github.com/auth0/java-jwt
 */
public class JWTWithStrongCipherCheckAuth0Test {

  private static final String SECRET_KEY = Base64.getEncoder().encodeToString("not-so-secret".getBytes());
  private static final String LOGIN = "login";

  public void auth0JWT() {
    JWTVerifier nonCompliantVerifier = JWT.require(Algorithm.none()) // Noncompliant [[sc=52;ec=68]] {{Use only strong cipher algorithms when verifying the signature of this JWT.}}
      .withSubject(LOGIN)
      .build();

    JWTVerifier nonCompliantVerifier2 = JWT.require(none()) // Noncompliant
      .withSubject(LOGIN)
      .build();

    JWTVerifier nonCompliantVerifier3 = JWT.require(com.auth0.jwt.algorithms.Algorithm.none()) // Noncompliant
      .withSubject(LOGIN)
      .build();

    JWTVerifier compliantVerifier1 = JWT.require(Algorithm.HMAC256(SECRET_KEY)) // Compliant
      .withSubject(LOGIN)
      .build();

    JWTVerifier compliantVerifier2 = JWT.require(new JWTWithStrongCipherCheckAuth0Test.MyAlgorithm("name", "description")) // Compliant
      .withSubject(LOGIN)
      .build();

    String tokenNotSigned = JWT.create()
      .withSubject(LOGIN)
      .withExpiresAt(addMinutes(new Date(), 20))
      .withIssuedAt(new Date())
      .sign(Algorithm.none()); // Noncompliant [[sc=13;ec=29]] {{Use only strong cipher algorithms when signing this JWT.}}

    String tokenSigned = JWT.create()
      .withSubject(LOGIN)
      .withExpiresAt(addMinutes(new Date(), 20))
      .withIssuedAt(new Date())
      .sign(Algorithm.HMAC256(SECRET_KEY)); // Compliant

    JWTCreator.Builder builder = JWT.create()
      .withSubject(LOGIN)
      .withExpiresAt(addMinutes(new Date(), 20))
      .withIssuedAt(new Date());

    String tokenSignedLater = builder.sign(Algorithm.none()); // Noncompliant
  }

  private class MyAlgorithm extends Algorithm {

    protected MyAlgorithm(String name, String description) {
      super(name, description);
    }

    @Override
    public void verify(DecodedJWT decodedJWT) throws SignatureVerificationException {

    }

    @Override
    public byte[] sign(byte[] bytes) throws SignatureGenerationException {
      return new byte[0];
    }
  }
}
