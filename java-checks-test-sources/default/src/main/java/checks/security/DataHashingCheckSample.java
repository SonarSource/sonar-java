package checks.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.util.Properties;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.Md4PasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;

class HashMethodsCheck {

  private static final String ALGORITHM = "MD2";

  void myMethod(String algorithm, Provider provider, Properties props) throws NoSuchAlgorithmException, NoSuchProviderException {
    MessageDigest md = null;
    md = MessageDigest.getInstance("MD2"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
//                     ^^^^^^^^^^^
    md = MessageDigest.getInstance("MD4"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("MD6"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("MD5"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
//                     ^^^^^^^^^^^
    md = MessageDigest.getInstance("HAVAL-128"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("HMAC-MD5"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("RIPEMD"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("RIPEMD-128"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("RIPEMD160"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("HMACRIPEMD160"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA-0"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA-1"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA-224"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    md = MessageDigest.getInstance("SHA-256"); // Compliant
    md = MessageDigest.getInstance("SHA-384"); // Compliant
    md = MessageDigest.getInstance("SHA-512"); // Compliant
    md = DigestUtils.getDigest("MD2"); // Noncompliant
    md = DigestUtils.getDigest("MD5"); // Noncompliant
    md = DigestUtils.getDigest("SHA-1"); // Noncompliant
    md = DigestUtils.getDigest("SHA-256");
    md = DigestUtils.getMd5Digest(); // Noncompliant
    md = DigestUtils.getShaDigest(); // Noncompliant
    md = DigestUtils.getSha1Digest(); // Noncompliant
    md = DigestUtils.getSha256Digest();
    DigestUtils.md2(""); // Noncompliant
    DigestUtils.md2Hex(""); // Noncompliant
    DigestUtils.md5(""); // Noncompliant
    DigestUtils.md5Hex(""); // Noncompliant
    DigestUtils.sha1(""); // Noncompliant
    DigestUtils.sha1Hex(""); // Noncompliant
    DigestUtils.sha(""); // Noncompliant
    DigestUtils.shaHex(""); // Noncompliant
    DigestUtils.sha256("");
    DigestUtils.sha256Hex("");
    md = MessageDigest.getInstance(algorithm);
    md = DigestUtils.getDigest(algorithm);
    md5Hex(""); // Noncompliant
    com.google.common.hash.Hashing.md5(); // Noncompliant
    com.google.common.hash.Hashing.sha1(); // Noncompliant
    com.google.common.hash.Hashing.sha256();
    md = MessageDigest.getInstance("MD5", provider); // Noncompliant
    md = MessageDigest.getInstance("SHA1", "provider"); // Noncompliant
    md = MessageDigest.getInstance("sha-1", "provider"); // Noncompliant

    String myAlgo = props.getProperty("myCoolAlgo", "SHA1");

    md = MessageDigest.getInstance(myAlgo, provider); // Noncompliant
    md = MessageDigest.getInstance(getAlgo(), provider);
    md = DigestUtils.getDigest(props.getProperty("mySuperOtherAlgo", "SHA-1")); // Noncompliant
    md = DigestUtils.getDigest(props.getProperty("mySuperOtherAlgo"));

    md = MessageDigest.getInstance(ALGORITHM); // Noncompliant
  }

  private String getAlgo() {
    return null;
  }

}

class ExtendedFile extends java.io.File {
  public ExtendedFile(@NotNull String pathname) {
    super(pathname);
  }

  void myMethod() throws NoSuchAlgorithmException {
    MessageDigest md = null;
    md = MessageDigest.getInstance(separator);
  }
}

class CryptoAPIs {

  void mac() throws NoSuchAlgorithmException {
    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacMD5"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    mac = javax.crypto.Mac.getInstance("HmacSHA1"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    mac = javax.crypto.Mac.getInstance("HmacSHA256");
  }

  void signature() throws NoSuchAlgorithmException {
    java.security.Signature signature = java.security.Signature.getInstance("SHA1withDSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    signature = java.security.Signature.getInstance("SHA1withRSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    signature = java.security.Signature.getInstance("MD2withRSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    signature = java.security.Signature.getInstance("MD5withRSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    signature = java.security.Signature.getInstance("SHA256withRSA"); // Compliant
  }

  void keys() throws NoSuchAlgorithmException {
    javax.crypto.KeyGenerator keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA1"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    keyGenerator = javax.crypto.KeyGenerator.getInstance("HmacSHA256");
    keyGenerator = javax.crypto.KeyGenerator.getInstance("AES");

    java.security.KeyPairGenerator keyPair = java.security.KeyPairGenerator.getInstance("HmacSHA1"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
  }

  void dsa() throws NoSuchAlgorithmException {
    java.security.AlgorithmParameters.getInstance("DSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    java.security.AlgorithmParameters.getInstance("DiffieHellman");
    java.security.AlgorithmParameterGenerator.getInstance("DSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    java.security.AlgorithmParameterGenerator.getInstance("DiffieHellman");
    java.security.KeyPairGenerator.getInstance("DSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    java.security.KeyPairGenerator.getInstance("DiffieHellman");
    java.security.KeyFactory.getInstance("DSA"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    java.security.KeyFactory.getInstance("DiffieHellman");
  }
}

class DeprecatedSpring {
  void foo() {
    new ShaPasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new ShaPasswordEncoder(512); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new Md5PasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new LdapShaPasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new LdapShaPasswordEncoder(org.springframework.security.crypto.keygen.KeyGenerators.secureRandom()); // Noncompliant
    new Md4PasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new MessageDigestPasswordEncoder("algo"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    NoOpPasswordEncoder.getInstance(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new StandardPasswordEncoder(); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new StandardPasswordEncoder("foo"); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    new BCryptPasswordEncoder();
  }
}

class SpringDigestUtils {

  void digestUtils() throws IOException {
    org.springframework.util.DigestUtils.appendMd5DigestAsHex(new byte[10], new StringBuilder()); // Noncompliant {{Make sure this weak hash algorithm is not used in a sensitive context here.}}
    org.springframework.util.DigestUtils.appendMd5DigestAsHex(new FileInputStream(""), new StringBuilder()); // Compliant, file content
    org.springframework.util.DigestUtils.md5Digest(new byte[10]); // Noncompliant
    org.springframework.util.DigestUtils.md5Digest(new FileInputStream("")); // Compliant, file content
    org.springframework.util.DigestUtils.md5DigestAsHex(new byte[10]); // Noncompliant
    org.springframework.util.DigestUtils.md5DigestAsHex(new FileInputStream("")); // Compliant, file content
  }

}

class FileSourcedData {

  void oneShot(java.nio.file.Path path, java.io.File file) throws IOException {
    DigestUtils.md5Hex(java.nio.file.Files.newInputStream(path));
    DigestUtils.sha1Hex(java.nio.file.Files.readAllBytes(path));
    DigestUtils.md5Hex(java.nio.file.Files.readString(path));
    DigestUtils.md5Hex(new java.io.BufferedInputStream(new FileInputStream(file)));
    DigestUtils.md5Hex(org.apache.commons.io.IOUtils.toByteArray(new FileInputStream(file)));
    DigestUtils.md5Hex(org.apache.commons.io.IOUtils.toByteArray(new java.io.FileReader(file)));
    DigestUtils.md5Hex(org.apache.commons.io.FileUtils.readFileToByteArray(file));
    DigestUtils.md5Hex(java.nio.file.Files.newInputStream(path).readAllBytes());
    DigestUtils.md5Hex(com.google.common.io.Files.asByteSource(file).read());
    DigestUtils.md5Hex(java.nio.channels.Channels.newInputStream(java.nio.channels.FileChannel.open(path)));
  }

  void uploads(org.springframework.web.multipart.MultipartFile multipartFile, javax.servlet.http.Part javaxPart, jakarta.servlet.http.Part jakartaPart) throws IOException {
    DigestUtils.md5Hex(multipartFile.getBytes());
    DigestUtils.md5Hex(multipartFile.getInputStream()); // Noncompliant
    DigestUtils.sha1Hex(javaxPart.getInputStream()); // Noncompliant
    DigestUtils.sha1Hex(jakartaPart.getInputStream()); // Noncompliant
  }

  void variables(java.nio.file.Path path) throws IOException {
    try (java.io.InputStream resource = java.nio.file.Files.newInputStream(path)) {
      DigestUtils.sha1Hex(resource);
    }
  }

  void chained(java.nio.file.Path path) throws IOException, NoSuchAlgorithmException {
    MessageDigest.getInstance("MD5").digest(java.nio.file.Files.readAllBytes(path));
    MessageDigest.getInstance("SHA-1").digest(java.nio.file.Files.readAllBytes(path));
    DigestUtils.getMd5Digest().digest(java.nio.file.Files.readAllBytes(path));
    com.google.common.hash.Hashing.md5().hashBytes(java.nio.file.Files.readAllBytes(path));
  }

  void streaming(java.nio.file.Path path) throws IOException, NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    try (java.io.InputStream is = new java.security.DigestInputStream(java.nio.file.Files.newInputStream(path), md)) {
      is.transferTo(java.io.OutputStream.nullOutputStream());
    }
    md.digest();
  }

  void updates(java.nio.file.Path first, java.nio.file.Path second) throws IOException, NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-1");
    md.update(java.nio.file.Files.readAllBytes(first));
    md.update(java.nio.file.Files.readAllBytes(second));
    md.digest();
  }

  void mac(java.nio.file.Path path, javax.crypto.SecretKey key) throws IOException, java.security.GeneralSecurityException {
    javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
    mac.init(key);
    mac.doFinal(java.nio.file.Files.readAllBytes(path));
  }

  void guavaByteSource(java.io.File file) throws IOException {
    com.google.common.io.Files.asByteSource(file).hash(com.google.common.hash.Hashing.md5());
  }
}

class NotProvenToBeFileData {

  private final MessageDigest fieldDigest = DigestUtils.getMd5Digest(); // Noncompliant

  static byte[] concat(byte[] first, byte[] second) {
    return first;
  }

  void consume(MessageDigest digest) {
  }

  void mixedData(java.nio.file.Path path, String password) throws IOException, NoSuchAlgorithmException {
    DigestUtils.md5Hex(concat(java.nio.file.Files.readAllBytes(path), password.getBytes())); // Noncompliant
    MessageDigest md = MessageDigest.getInstance("MD5"); // Noncompliant
    md.update(java.nio.file.Files.readAllBytes(path));
    md.update(password.getBytes());
    md.digest();
  }

  void escapingDigest(java.nio.file.Path path) throws IOException, NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5"); // Noncompliant
    md.update(java.nio.file.Files.readAllBytes(path));
    consume(md);
  }

  void neverFed() throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("MD5"); // Noncompliant
    md.digest();
  }

  void notFileData(byte[] bytes, java.io.InputStream stream, java.net.Socket socket) throws IOException {
    DigestUtils.md5Hex(bytes); // Noncompliant
    DigestUtils.md5Hex(new java.io.ByteArrayInputStream(bytes)); // Noncompliant
    DigestUtils.md5Hex(new java.io.BufferedInputStream(stream)); // Noncompliant
    DigestUtils.md5Hex(org.apache.commons.io.IOUtils.toByteArray(socket.getInputStream())); // Noncompliant
    DigestUtils.md5Hex(java.nio.channels.Channels.newInputStream(java.nio.channels.Channels.newChannel(stream))); // Noncompliant
    com.google.common.hash.Hashing.md5().hashBytes(bytes); // Noncompliant
    com.google.common.io.ByteSource.wrap(bytes).hash(com.google.common.hash.Hashing.md5()); // Noncompliant
  }

  void reassigned(java.nio.file.Path path, byte[] bytes) throws IOException {
    java.io.InputStream stream = java.nio.file.Files.newInputStream(path);
    stream = new java.io.ByteArrayInputStream(bytes);
    DigestUtils.md5Hex(stream); // Noncompliant
  }

  void otherAlgorithms(java.nio.file.Path path) throws IOException, NoSuchAlgorithmException {
    DigestUtils.md2Hex(java.nio.file.Files.readAllBytes(path)); // Noncompliant
    MessageDigest.getInstance("MD2").digest(java.nio.file.Files.readAllBytes(path)); // Noncompliant
  }
}
