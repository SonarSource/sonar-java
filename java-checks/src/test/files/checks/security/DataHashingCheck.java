import java.security.MessageDigest;
import java.security.Provider;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import com.google.common.hash.Hashing;
import org.apache.commons.codec.digest.DigestUtils;

class DataHashing {

  void messageDigest(String algorithm, String providerStr, Provider provider) throws Exception {
    MessageDigest.getInstance(algorithm); // Noncompliant
    MessageDigest.getInstance(algorithm, providerStr); // Noncompliant
    MessageDigest.getInstance(algorithm, provider); // Noncompliant
  }

  void javaxCrypto(char[] password, byte[] salt, int iterationCount, int keyLength) throws Exception {
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512"); // Noncompliant
    SecretKeyFactory factory2 = SecretKeyFactory.getInstance("randomstring"); // compliant arg does not starts with PBKDF2
    String someVar;
    SecretKeyFactory factory3 = SecretKeyFactory.getInstance(someVar); // compliant arg does not starts with PBKDF2
    PBEKeySpec spec = new PBEKeySpec(password, salt, iterationCount, keyLength);
    factory.generateSecret(spec).getEncoded();
  }

  void Guava() {
    Hashing.md5(); // Noncompliant
    Hashing.sha1(); // Noncompliant
    Hashing.sha256(); // Noncompliant
    Hashing.sha384(); // Noncompliant
    Hashing.sha512(); // Noncompliant
  }

  void apacheCommons(String strName, byte[] data, String str, java.io.InputStream stream) throws Exception {
    new DigestUtils(strName); // Noncompliant
    new DigestUtils(); // Noncompliant

    DigestUtils.getMd2Digest(); // Noncompliant [[sc=17;ec=29]] {{Make sure that hashing data is safe here.}}
    DigestUtils.getMd5Digest(); // Noncompliant
    DigestUtils.getShaDigest(); // Noncompliant
    DigestUtils.getSha1Digest(); // Noncompliant
    DigestUtils.getSha256Digest(); // Noncompliant
    DigestUtils.getSha384Digest(); // Noncompliant
    DigestUtils.getSha512Digest(); // Noncompliant


    DigestUtils.md2(data); // Noncompliant
    DigestUtils.md2(stream); // Noncompliant
    DigestUtils.md2(str); // Noncompliant
    DigestUtils.md2Hex(data); // Noncompliant
    DigestUtils.md2Hex(stream); // Noncompliant
    DigestUtils.md2Hex(str); // Noncompliant

    DigestUtils.md5(data); // Noncompliant
    DigestUtils.md5(stream); // Noncompliant
    DigestUtils.md5(str); // Noncompliant
    DigestUtils.md5Hex(data); // Noncompliant
    DigestUtils.md5Hex(stream); // Noncompliant
    DigestUtils.md5Hex(str); // Noncompliant

    DigestUtils.sha(data); // Noncompliant
    DigestUtils.sha(stream); // Noncompliant
    DigestUtils.sha(str); // Noncompliant
    DigestUtils.shaHex(data); // Noncompliant
    DigestUtils.shaHex(stream); // Noncompliant
    DigestUtils.shaHex(str); // Noncompliant

    DigestUtils.sha1(data); // Noncompliant
    DigestUtils.sha1(stream); // Noncompliant
    DigestUtils.sha1(str); // Noncompliant
    DigestUtils.sha1Hex(data); // Noncompliant
    DigestUtils.sha1Hex(stream); // Noncompliant
    DigestUtils.sha1Hex(str); // Noncompliant

    DigestUtils.sha256(data); // Noncompliant
    DigestUtils.sha256(stream); // Noncompliant
    DigestUtils.sha256(str); // Noncompliant
    DigestUtils.sha256Hex(data); // Noncompliant
    DigestUtils.sha256Hex(stream); // Noncompliant
    DigestUtils.sha256Hex(str); // Noncompliant

    DigestUtils.sha384(data); // Noncompliant
    DigestUtils.sha384(stream); // Noncompliant
    DigestUtils.sha384(str); // Noncompliant
    DigestUtils.sha384Hex(data); // Noncompliant
    DigestUtils.sha384Hex(stream); // Noncompliant
    DigestUtils.sha384Hex(str); // Noncompliant

    DigestUtils.sha512(data); // Noncompliant
    DigestUtils.sha512(stream); // Noncompliant
    DigestUtils.sha512(str); // Noncompliant
    DigestUtils.sha512Hex(data); // Noncompliant
    DigestUtils.sha512Hex(stream); // Noncompliant
    DigestUtils.sha512Hex(str); // Noncompliant
  }
}
