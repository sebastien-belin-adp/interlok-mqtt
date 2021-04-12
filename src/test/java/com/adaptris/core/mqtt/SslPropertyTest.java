/*
    Copyright Adaptris

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */

package com.adaptris.core.mqtt;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import com.adaptris.interlok.junit.scaffolding.BaseCase;
import com.adaptris.security.exc.PasswordException;


public class SslPropertyTest extends BaseCase {

  @Test
  public void testProtocol() throws PasswordException {
    testProperty("protocol", "TLSv1");
  }

  @Test
  public void testContextProvider() throws PasswordException {
    testProperty("contextProvider", "SunJSSE");
  }

  @Test
  public void testKeyStore() throws PasswordException {
    testProperty("keyStore", "/path/to/file");
  }

  @Test
  public void testKeyStorePassword() throws PasswordException {
    testProperty("keyStorePassword", "password");
  }

  @Test
  public void testKeyStorePasswordEncoded() throws PasswordException {
    Properties sslContextProperties = new Properties();

    SslProperty.getIgnoreCase("keyStorePassword").applyProperty(sslContextProperties,
        "PW:AAAAEIQEEHnv1daAPYIREwI2HmgAAAAQPTFQmeDBImH30M6WfgMBKwAAABApTlYrXAKk1xeM2Jiinx+I");

    assertProperty(sslContextProperties, "com.ibm.ssl.keyStorePassword", "password");
  }

  @Test
  public void testKeyStoreType() throws PasswordException {
    testProperty("keyStoreType", "JKS");
  }

  @Test
  public void testKeyStoreProvider() throws PasswordException {
    testProperty("keyStoreProvider", "IBMJCE");
  }

  @Test
  public void testTrustStore() throws PasswordException {
    testProperty("trustStore", "/path/to/file");
  }

  @Test
  public void testTrustStorePassword() throws PasswordException {
    testProperty("trustStorePassword", "password");
  }

  @Test
  public void testTrustStorePasswordEncoded() throws PasswordException {
    Properties sslContextProperties = new Properties();

    SslProperty.getIgnoreCase("trustStorePassword").applyProperty(sslContextProperties,
        "PW:AAAAEIQEEHnv1daAPYIREwI2HmgAAAAQPTFQmeDBImH30M6WfgMBKwAAABApTlYrXAKk1xeM2Jiinx+I");

    assertProperty(sslContextProperties, "com.ibm.ssl.trustStorePassword", "password");
  }

  @Test
  public void testTrustStoreType() throws PasswordException {
    testProperty("trustStoreType", "JKS");
  }

  @Test
  public void testTrustStoreProvider() throws PasswordException {
    testProperty("trustStoreProvider", "IBMJCE");
  }

  @Test
  public void testEnabledCipherSuites() throws PasswordException {
    testProperty("enabledCipherSuites", "SSL_RSA_WITH_AES_128_CBC_SHA;SSL_RSA_WITH_3DES_EDE_CBC_SHA");
  }

  @Test
  public void testKeyManager() throws PasswordException {
    testProperty("keyManager", "IbmX509");
  }

  @Test
  public void testTrustManager() throws PasswordException {
    testProperty("trustManager", "PKIX");
  }

  @Test
  public void testDefault() throws PasswordException {
    Properties sslContextProperties = new Properties();

    SslProperty.getIgnoreCase("doesntExist").applyProperty(sslContextProperties, "value");

    Assert.assertEquals(0, sslContextProperties.size());
  }

  private void testProperty(String key, String value) throws PasswordException {
    Properties sslContextProperties = new Properties();

    SslProperty.getIgnoreCase(key).applyProperty(sslContextProperties, value);

    assertProperty(sslContextProperties, "com.ibm.ssl." + key, value);
  }

  private void assertProperty(Properties sslContextProperties, String key, String expectedValue) {
    Assert.assertEquals(1, sslContextProperties.size());
    Assert.assertEquals(expectedValue, sslContextProperties.getProperty(key));
  }

}
