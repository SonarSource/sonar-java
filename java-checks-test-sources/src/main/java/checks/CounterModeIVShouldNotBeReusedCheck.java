package checks;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class CounterModeIVShouldNotBeReusedCheck {

  Charset utf8 = StandardCharsets.UTF_8;
  
  void testJca(String unknownString, byte[] unkownBytes) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      SecretKeySpec skeySpec = new SecretKeySpec(new byte[] {}, "AES");

      char[] chars;
      chars = "testme".toCharArray();
      byte[] bytes = String.valueOf(chars).getBytes(utf8);
      GCMParameterSpec params5 = new GCMParameterSpec(128, bytes);
      cipher.init(1, skeySpec, params5); // Noncompliant [[sc=14;ec=18;secondary=+0,-1,-2,-3]]
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, params5); // Compliant decrypt mode
      
      byte[] KEY_BYTES = {0x41, 0x70, 0x61, 0x63, 0x68, 0x65, 0x43, 0x6F, 0x6D, 0x6D, 0x6F, 0x6E, 0x73, 0x56, 0x46, 0x53};
      GCMParameterSpec params4 = new GCMParameterSpec(128, KEY_BYTES);
      cipher.init(1, skeySpec, params4); // Noncompliant [[sc=14;ec=18;secondary=+0,-1,-2]]
      
      byte[] src = "7cVgr5cbdCZV".getBytes(utf8);
      GCMParameterSpec params = new GCMParameterSpec(128, src);
      cipher.init(1, skeySpec, params); // Noncompliant [[secondary=+0,-1,-2]] {{Use a dynamically-generated initialization vector (IV) to avoid IV-key pair reuse.}}
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new GCMParameterSpec(128, src)); // Noncompliant [[secondary=+0,-3]]
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new GCMParameterSpec(128, new byte[]{}));
      
      String staticPiece1 = "imjustthefirsthalf";
      String staticPiece2 = "secondhalf";
      byte[] bytes1 = (staticPiece1+staticPiece2).getBytes();
      GCMParameterSpec gcm = new GCMParameterSpec(128, bytes1);
      cipher.init(1, skeySpec, gcm); // Noncompliant [[secondary=+0,-1,-2,-3,-4]]
      
      GCMParameterSpec gcm3 = new GCMParameterSpec(128, unkownBytes);
      cipher.init(1, skeySpec, gcm3);
      
      
      
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
      outputStream.write( "7cVgr5cbdCZV".getBytes(utf8) );
      byte[] c = outputStream.toByteArray();
      GCMParameterSpec gcm2 = new GCMParameterSpec(128, c);
      cipher.init(1, skeySpec, gcm2); // FALSE NEGATIVE 
      
    } catch (Exception e) {

    }
  }
  
  void bouncyCastle(byte[] byteArrayFromElsewhere, String stringFromSomewhere) {
    try {
      
      byte[] key      = "qqqqqqqqqqqqqqqq".getBytes(utf8);
      
      /* Encryption AES CCM */
      BlockCipher engine      = new AESEngine();
      CCMBlockCipher ccmCipher   = new CCMBlockCipher(engine);
      GCMBlockCipher gcmCipher = new GCMBlockCipher(engine);
      byte[] nonce    = "7cVgr5cbdCZV".getBytes(utf8); // Secondary location: The initialization vector is a static value
      AEADParameters params   = new AEADParameters(new KeyParameter(key), 128, nonce); // Secondary location: The initialization vector is configured here.
      ccmCipher.init(true, params); // Noncompliant
      gcmCipher.init(true, params); // Noncompliant [[sc=17;ec=21;secondary=+0,-2,-3]]
      gcmCipher.init(false, params); // Compliant
      
      AEADParameters staticParams = new AEADParameters(new KeyParameter(key), 0, byteArrayFromElsewhere);
      gcmCipher.init(true, staticParams); // Compliant  We cannot define if the method param 'byteArrayFromElsewhere' is static or not
      
      byte[] staticByteArray = stringFromSomewhere.getBytes();
      AEADParameters staticParams2 = new AEADParameters(new KeyParameter(key), 0, staticByteArray);
      gcmCipher.init(true, staticParams2); // Compliant  We cannot define if the method param 'stringFromSomewhere' is static or not
      
    } catch (Exception e) {
      
    }
  }

}
