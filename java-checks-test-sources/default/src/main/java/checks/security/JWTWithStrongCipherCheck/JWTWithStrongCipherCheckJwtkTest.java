package checks.security.JWTWithStrongCipherCheck;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import static org.apache.commons.lang.time.DateUtils.addMinutes;

/**
 * JSON web token handling with https://github.com/jwtk/jjwt
 */
public class JWTWithStrongCipherCheckJwtkTest {

  private static final String SECRET_KEY = Base64.getEncoder().encodeToString("not-so-secret".getBytes());
  private static final String LOGIN = "login";

  static JwtBuilder builderAsFieldNotInitialized;
  static JwtBuilder builderAsField = Jwts.builder();

  public void jwtkJWT() {
    String tokenNotSigned = getTokenNotSigned();
    String tokenSigned = getTokenSigned();

    // PARSE WITHOUT SIGNATURE TESTCASES
    Object body1 = Jwts.parser().parse(tokenNotSigned).getBody(); // Noncompliant [[sc=34;ec=39]] {{The JWT signature (JWS) should be verified before using this token.}}
    Object body2 = Jwts.parser().parse(tokenSigned).getBody(); // Noncompliant

    // Despite the fact that we set a signing key, parse is subject to the none algorithm. See rule description.
    Object body3 = Jwts.parser().setSigningKey(SECRET_KEY).parse(tokenNotSigned).getBody(); // Noncompliant [[sc=60;ec=65]] {{The JWT signature (JWS) should be verified before using this token.}}
    Object body4 = Jwts.parser().setSigningKey(SECRET_KEY).parse(tokenSigned).getBody(); // Noncompliant

    // parseClaimsJws WITH SIGNATURE TESTCASES
    Object body5 = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(tokenNotSigned).getBody(); // Compliant
    Object body6 = Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(tokenSigned).getBody(); // Compliant

    // parseClaimsJwt WITHOUT SIGNATURE TESTCASES
    Object body7 = Jwts.parser().parseClaimsJwt(tokenNotSigned).getBody(); // Compliant
    Object body8 = Jwts.parser().parseClaimsJwt(tokenSigned).getBody(); // Compliant

    JwtParser parser = Jwts.parser();
    parser.parse(tokenSigned); // Noncompliant
  }

  private static String getTokenNotSigned() {
    JwtBuilder builder1 = Jwts.builder();
    builder1.setIssuedAt(new Date());
    String token1 = builder1.compact(); // Noncompliant

    JwtBuilder builder2 = Jwts.builder().setIssuedAt(new Date());
    String token2 = builder2.compact(); // Noncompliant

    JwtBuilder builder3 = Jwts.builder();
    String token3 = builder3.setIssuedAt(new Date()).compact(); // Noncompliant

    JwtBuilder builder4;
    builder4 = Jwts.builder();
    String token4 = builder4.compact(); // Noncompliant

    JwtBuilder builder5 = Jwts.builder();
    builder5 = builder5.setIssuedAt(new Date());
    String token5 = builder5.compact(); // Noncompliant

    return Jwts.builder()
      .setId("123")
      .setSubject(LOGIN)
      .setIssuedAt(new Date())
      .setExpiration(addMinutes(new Date(), 20))
      .compact(); // Noncompliant [[sc=8;ec=15]] {{Sign this token using a strong cipher algorithm.}}
  }

  private String getTokenSignedWithNone() throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
    SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
    DESKeySpec dks = new DESKeySpec("mysuperkey".getBytes());
    Key key = keyFactory.generateSecret(dks);

    return Jwts.builder() // Compliant, not accepted by JWTK  (throws IllegalArgumentException).
      .setId("123")
      .setSubject(LOGIN)
      .setIssuedAt(new Date())
      .setExpiration(addMinutes(new Date(), 20))
      .signWith(SignatureAlgorithm.NONE, key)
      .compact();
  }

  private static String getTokenSigned() {
    String token0 = Jwts.builder()
      .setId("123")
      .setSubject(LOGIN)
      .setIssuedAt(new Date())
      .setExpiration(addMinutes(new Date(), 20))
      .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
      .compact(); // Compliant

    JwtBuilder builder1 = Jwts.builder();
    builder1 = builder1.setIssuedAt(new Date());
    builder1.signWith(SignatureAlgorithm.HS256, SECRET_KEY);
    String token1 = builder1.compact(); // Compliant

    JwtBuilder builder2 = Jwts.builder();
    builder2 = signTheToken(builder2);
    String token2 = builder2.compact(); // Compliant, signed somewhere else.

    JwtBuilder builder3 = Jwts.builder();
    builder3.setIssuedAt(new Date());
    if (SECRET_KEY.equals("1234")) {
      builder3.signWith(SignatureAlgorithm.HS256, SECRET_KEY);
    }
    String token3 = builder3.compact(); // Compliant, signed conditionally, we do not report an issue to avoid FP.

    JwtBuilder builder4 = Jwts.builder();
    builder4 = addDateToToken(builder4);
    String token4 = builder4.compact(); // Compliant, FN, we do not report an issue to avoid FP.

    JwtBuilder builder5 = Jwts.builder();
    builder5.signWith(SignatureAlgorithm.HS256, SECRET_KEY);
    String token5 = builder5.setIssuedAt(new Date()).compact(); // Compliant

    JwtBuilder builder6 = Jwts.builder().setIssuedAt(new Date());
    String token6 = builder6.signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact(); // Compliant

    JwtBuilder builder7 = Jwts.builder();
    builder7.setIssuedAt(new Date())
      .signWith(SignatureAlgorithm.HS256, SECRET_KEY);
    String token7 = builder7.compact(); // Compliant

    JwtBuilder builder8 = Jwts.builder().signWith(SignatureAlgorithm.HS256, SECRET_KEY);
    builder8.setIssuedAt(new Date());
    String token8 = builder8.compact(); // Compliant

    JwtBuilder builder9;
    builder9 = Jwts.builder();
    String token9 = builder9.signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();

    String token10 = builderAsField.compact(); // Compliant, field can be signed anywhere
    String token11 = builderAsFieldNotInitialized.compact(); // Compliant, field can be signed anywhere

    return getTokenBuilder().compact(); // Compliant, FN, created somewhere else.
  }

  private static JwtBuilder getTokenBuilder() {
    return Jwts.builder() // Compliant, can be signed later
      .setId("123")
      .setSubject(LOGIN)
      .setIssuedAt(new Date())
      .setExpiration(addMinutes(new Date(), 20));
  }

  private static JwtBuilder signTheToken(JwtBuilder builder) {
    return builder.signWith(SignatureAlgorithm.HS256, SECRET_KEY);
  }

  private static JwtBuilder addDateToToken(JwtBuilder builder) {
    return builder.setIssuedAt(new Date());
  }

}
