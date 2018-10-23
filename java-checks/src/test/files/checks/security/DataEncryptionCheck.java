import javax.crypto.Cipher;
import java.util.Properties;
import org.apache.commons.crypto.utils.Utils;
import org.apache.commons.crypto.cipher.CryptoCipherFactory;
import org.apache.commons.crypto.cipher.CryptoCipherFactory.CipherProvider;

class Test {
  void test() {
    Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding"); // Noncompliant [[sc=23;ec=34]] {{Make sure that encrypting data is safe here.}}
    Properties properties = new Properties();
    properties.setProperty(CryptoCipherFactory.CLASSES_KEY, CipherProvider.OPENSSL.getClassName());
    Utils.getCipherInstance("AES/CBC/PKCS5Padding", properties);  // Noncompliant
  }
}
