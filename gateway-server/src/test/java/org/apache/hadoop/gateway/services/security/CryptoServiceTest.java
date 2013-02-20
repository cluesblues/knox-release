package org.apache.hadoop.gateway.services.security;

import static org.junit.Assert.*;

import java.util.Map;

import org.apache.hadoop.gateway.config.GatewayConfig;
import org.apache.hadoop.gateway.services.ServiceLifecycleException;
import org.apache.hadoop.gateway.services.security.impl.AESEncryptor;
import org.apache.hadoop.gateway.services.security.impl.DefaultCryptoService;
import org.apache.hadoop.test.category.ManualTests;
import org.apache.hadoop.test.category.MediumTests;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category( { ManualTests.class, MediumTests.class } )
public class CryptoServiceTest {
  static CryptoService cs = null;
  static AliasService as = null;
  
  @BeforeClass
  public static void setupSuite() throws Exception {
    as = new AliasService() {
      @Override
      public void init(GatewayConfig config, Map<String, String> options)
          throws ServiceLifecycleException {
      }

      @Override
      public void start() throws ServiceLifecycleException {
      }

      @Override
      public void stop() throws ServiceLifecycleException {
      }

      @Override
      public void addAliasForCluster(String clusterName, String alias,
          String value) {
      }

      @Override
      public char[] getPasswordFromAliasForCluster(String clusterName,
          String alias) {
        return "password".toCharArray();
      }

      @Override
      public char[] getPasswordFromAliasForCluster(String clusterName,
          String alias, boolean generate) {
        return null;
      }

      @Override
      public void generateAliasForCluster(String clusterName, String alias) {
      }
    };
    cs = new DefaultCryptoService().setAliasService(as);
  }
  
  @Test
  public void testAESEncryptor() throws Exception {
    // password to create key - same Encryptor
    String queryString = "url=http://localhost:50070/api/v1/blahblah";
    AESEncryptor aes0 = new AESEncryptor("password");
    EncryptionResult result0 = cs.encryptForCluster("Test", "encrypt_url", queryString.getBytes("UTF8"));
    byte[] decrypted0 = aes0.decrypt(result0.salt, result0.iv, result0.cipher);
    assertEquals(queryString, new String(decrypted0, "UTF8"));
    assertEquals(queryString.getBytes("UTF8").length, decrypted0.length);
    assertEquals(queryString.getBytes("UTF8").length, new String(decrypted0, "UTF8").toCharArray().length);
    
    // password to create key - same Encryptor
    AESEncryptor aes = new AESEncryptor("Test");
    EncryptionResult result = aes.encrypt("larry".getBytes("UTF8"));
    byte[] decrypted = aes.decrypt(result.salt, result.iv, result.cipher);
    assertEquals(new String(decrypted, "UTF8"), "larry");

    // password to create key - different Encryptor
    AESEncryptor aes2 = new AESEncryptor("Test");
    decrypted = aes2.decrypt(result.salt, result.iv, result.cipher);
    assertEquals(new String(decrypted, "UTF8"), "larry");

    
    // password to create key resolved from alias - same Encryptor
    AESEncryptor aes3 = new AESEncryptor(new String(as.getPasswordFromAliasForCluster("test", "encrypt_url")));
    result = aes3.encrypt("larry".getBytes("UTF8"));
    decrypted = aes3.decrypt(result.salt, result.iv, result.cipher);
    assertEquals(new String(decrypted, "UTF8"), "larry");

    // password to create key resolved from alias - different Encryptor
    AESEncryptor aes4 = new AESEncryptor(new String(as.getPasswordFromAliasForCluster("test", "encrypt_url")));
    decrypted = aes4.decrypt(result.salt, result.iv, result.cipher);
    assertEquals(new String(decrypted, "UTF8"), "larry");
  }
  
  @Test
  //@Ignore
  public void testEncryptionOfQueryStrings() throws Exception {
    String alias = "encrypt-url";
    String queryString = "url=http://localhost:50070/api/v1/blahblah";    
    
    EncryptionResult result = cs.encryptForCluster("Test", alias, queryString.getBytes("UTF8"));
    assertTrue("Resulted cipertext length should be a multiple of 16", (result.cipher.length % 16) == 0);
    byte[] decryptedQueryString = cs.decryptForCluster("Test", alias, result.cipher, result.iv, result.salt);
    assertEquals(queryString.getBytes("UTF8").length, decryptedQueryString.length);
  }
}