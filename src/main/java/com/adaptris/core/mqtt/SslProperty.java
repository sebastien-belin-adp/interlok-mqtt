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

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.security.exc.PasswordException;
import com.adaptris.security.password.Password;

/**
 * Properties for {@link MqttConnectOptions#setSSLProperties(java.util.Properties)}.
 */
public enum SslProperty {

  /**
   * <dl>
   * <dt>com.ibm.ssl.protocol</dt>
   * <dd>One of: SSL, SSLv3, TLS, TLSv1, SSL_TLS.</dd>
   * </dl>
   */
  Protocol {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.protocol", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.contextProvider
   * <dd>Underlying JSSE provider. For example "IBMJSSE2" or "SunJSSE"</dd>
   * </dl>
   */
  ContextProvider {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.contextProvider", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.keyStore</dt>
   * <dd>The name of the file that contains the KeyStore object that you want the KeyManager to use.
   * For example /mydir/etc/key.p12</dd>
   * </dl>
   */
  KeyStore {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.keyStore", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.keyStorePassword</dt>
   * <dd>The password for the KeyStore object that you want the KeyManager to use. The password can
   * either be in plain-text, or may be obfuscated using the static method:
   * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>. This obfuscates the
   * password using a simple and insecure XOR and Base64 encoding mechanism. Note that this is only
   * a simple scrambler to obfuscate clear-text passwords.</dd>
   * </dl>
   */
  KeyStorePassword {
    @Override
    void applyProperty(Properties sslContextProperties, String value) throws PasswordException {
      sslContextProperties.setProperty("com.ibm.ssl.keyStorePassword", Password.decode(value));
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.keyStoreType</dt>
   * <dd>Type of key store, for example "PKCS12", "JKS", or "JCEKS".</dd>
   * </dl>
   */
  KeyStoreType {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.keyStoreType", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.keyStoreProvider</dt>
   * <dd>Key store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
   * </dl>
   */
  KeyStoreProvider {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.keyStoreProvider", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.trustStore</dt>
   * <dd>The name of the file that contains the KeyStore object that you want the TrustManager to
   * use.</dd>
   * </dl>
   */
  TrustStore {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.trustStore", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.trustStorePassword</dt>
   * <dd>The password for the TrustStore object that you want the TrustManager to use. The password
   * can either be in plain-text, or may be obfuscated using the static method:
   * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>. This obfuscates the
   * password using a simple and insecure XOR and Base64 encoding mechanism. Note that this is only
   * a simple scrambler to obfuscate clear-text passwords.</dd>
   * </dl>
   */
  TrustStorePassword {
    @Override
    void applyProperty(Properties sslContextProperties, String value) throws PasswordException {
      sslContextProperties.setProperty("com.ibm.ssl.trustStorePassword", Password.decode(value));
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.trustStoreType</dt>
   * <dd>The type of KeyStore object that you want the default TrustManager to use. Same possible
   * values as "keyStoreType".</dd>
   * </dl>
   */
  TrustStoreType {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.trustStoreType", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.trustStoreProvider</dt>
   * <dd>Trust store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
   * </dl>
   */
  TrustStoreProvider {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.trustStoreProvider", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.enabledCipherSuites</dt>
   * <dd>A list of which ciphers are enabled. Values are dependent on the provider, for example:
   * SSL_RSA_WITH_AES_128_CBC_SHA;SSL_RSA_WITH_3DES_EDE_CBC_SHA.</dd>
   * </dl>
   */
  EnabledCipherSuites {
    @Override
    void applyProperty(Properties sslContextProperties, String value) {
      sslContextProperties.setProperty("com.ibm.ssl.enabledCipherSuites", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.keyManager</dt>
   * <dd>Sets the algorithm that will be used to instantiate a KeyManagerFactory object instead of
   * using the default algorithm available in the platform. Example values: "IbmX509" or
   * "IBMJ9X509".</dd>
   * </dl>
   */
  KeyManager {
    @Override
    void applyProperty(Properties sslContextProperties, String value) throws PasswordException {
      sslContextProperties.setProperty("com.ibm.ssl.keyManager", value);
    }
  },
  /**
   * <dl>
   * <dt>com.ibm.ssl.trustManager</dt>
   * <dd>Sets the algorithm that will be used to instantiate a TrustManagerFactory object instead of
   * using the default algorithm available in the platform. Example values: "PKIX" or "IBMJ9X509".
   * </dd>
   * </dl>
   */
  TrustManager {
    @Override
    void applyProperty(Properties sslContextProperties, String value) throws PasswordException {
      sslContextProperties.setProperty("com.ibm.ssl.trustManager", value);
    }
  },
  /**
   * Default value to prevent returning a null
   */
  Default {
    @Override
    void applyProperty(Properties sslContextProperties, String value) throws PasswordException {
      // Don't add anything
    }
  };

  abstract void applyProperty(Properties sslContextProperties, String value) throws PasswordException;

  private static Logger LOG = LoggerFactory.getLogger(SslProperty.class.getName());

  public static SslProperty getIgnoreCase(String key) {
    for (SslProperty sslProperty : values()) {
      if (key.equalsIgnoreCase(sslProperty.toString())) {
        return sslProperty;
      }
    }
    LOG.trace("Unsupported SSL Property {}", key);
    return Default;
  }

}
