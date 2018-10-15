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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.InputFieldHint;
import com.adaptris.core.AdaptrisConnection;
import com.adaptris.core.AdaptrisConnectionImp;
import com.adaptris.core.CoreException;
import com.adaptris.security.exc.PasswordException;
import com.adaptris.security.password.Password;
import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;
import com.adaptris.util.TimeInterval;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@linkplain AdaptrisConnection} implementation for Paho MQTT.
 *
 * @config mqtt-connection
 * @license STANDARD
 * @since 3.5.0
 */
@XStreamAlias("mqtt-connection")
@AdapterComponent
@ComponentProfile(summary = "Connect to a MQTT broker", tag = "connections,mqtt", since = "3.5.0")
@DisplayOrder(order = {"username", "password", "serverUri"})
public class MqttConnection extends AdaptrisConnectionImp /*implements LicensedComponent*/ {

  private static final String PERSISTENCE_LOCATION = ".interlok-mqtt";

  public enum MqttProtocolVersion {
    V_3_1(MqttConnectOptions.MQTT_VERSION_3_1),
    V_3_1_1(MqttConnectOptions.MQTT_VERSION_3_1_1),
    DEFAULT(MqttConnectOptions.MQTT_VERSION_DEFAULT);

    private int versionValue;
    private MqttProtocolVersion(int versionValue) {
      this.versionValue = versionValue;
    }

    public int getVersionValue() {
      return versionValue;
    }
  }

  @NotNull
  private String serverUri;
  private String username;
  @InputFieldHint(style="PASSWORD")
  private String password;
  @AdvancedConfig
  private MqttProtocolVersion protocolVersion = MqttProtocolVersion.DEFAULT;
  @AdvancedConfig
  private boolean cleanSession = MqttConnectOptions.CLEAN_SESSION_DEFAULT;
  @Valid
  @AdvancedConfig
  private TimeInterval connectionTimeout;
  @Valid
  @AdvancedConfig
  private TimeInterval keepAliveInterval;
  @NotNull
  @Valid
  @AutoPopulated
  @AdvancedConfig
  private KeyValuePairSet sslProperties;
  @Valid
  @AdvancedConfig
  private MqttLastWill lastWill;

  private transient MqttConnectOptions options;
  // TODO
  // private transient DisconnectedBufferOptions disconnectedBufferOptions;

  private transient Map<String, MqttClient> mqttClients = new ConcurrentHashMap<>();
  private transient Map<String, MqttAsyncClient> mqttAsyncClients = new ConcurrentHashMap<>();

  public MqttConnection() {
    setSslProperties(new KeyValuePairSet());
  }

  /*@Override
  public boolean isEnabled(License license) {
    return license.isEnabled(LicenseType.Standard);
  }*/

  @Override
  protected void prepareConnection() throws CoreException {
    /*LicenseChecker.newChecker().checkLicense(this);*/
  }

  @Override
  protected synchronized void initConnection() throws CoreException {
    log.debug("Init Mqtt Connection");
    try {
      initMqttConnectOptions();
    } catch (Exception e) {
      throw new CoreException(e);
    }
  }

  @Override
  protected void startConnection() throws CoreException {
    log.debug("Start Mqtt Connection");
  }

  protected int timeIntervalToSecond(TimeInterval timeInteval) {
    if (timeInteval != null) {
      return Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(timeInteval.toMilliseconds())).intValue();
    }
    return -1;
  }

  @Override
  protected void stopConnection() {
    log.debug("Disconnect All Mqtt Clients");
    for (MqttClient mqttClient : mqttClients.values()) {
      stopSyncClientConnection(mqttClient);
    }
    for (MqttAsyncClient mqttAsyncClient : mqttAsyncClients.values()) {
      stopAsyncClientConnection(mqttAsyncClient);
    }
  }

  @Override
  protected void closeConnection() {
    log.debug("Close All Mqtt Clients");
    for (MqttClient mqttClient : mqttClients.values()) {
      closeSyncClientConnection(mqttClient);
    }
    for (MqttAsyncClient mqttAsyncClient : mqttAsyncClients.values()) {
      closeAsyncClientConnection(mqttAsyncClient);
    }
  }

  /**
   * Access method for getting a new synchronous MqttClient for producer/consumer
   */
  private MqttClient newSyncClient() throws CoreException {
    try {
      String clientId = getUniqueId() + "-" + MqttClient.generateClientId();
      MqttClient mqttClient = new MqttClient(serverUri, clientId, createMqttClientPersistence());
      mqttClients.put(clientId, mqttClient);
      return mqttClient;
    } catch (MqttException mqtte) {
      throw new CoreException("Mqtt Client could not be initialized", mqtte);
    }
  }

  public void startSyncClientConnection(MqttClient mqttClient) throws CoreException {
    log.debug("Connect Mqtt Client");
    try {
      if (!mqttClient.isConnected()) {
        mqttClient.connect(initMqttConnectOptions());
      }
    } catch (MqttException | PasswordException | UnsupportedEncodingException mqtte) {
      throw new CoreException(mqtte);
    }
  }

  public void stopSyncClientConnection(MqttClient mqttClient) {
    try {
      if (mqttClient != null && mqttClient.isConnected()) {
        log.debug("Disconnect Mqtt Client [{}]", mqttClient.getClientId());
        mqttClient.disconnect();
      }
    } catch (MqttException mqtte) {
      log.error("Could not stop connection", mqtte);
    }
  }

  public void closeSyncClientConnection(MqttClient mqttClient) {
    try {
      if (mqttClient != null) {
        log.debug("Close Mqtt Client [{}]", mqttClient.getClientId());
        if (mqttClient.isConnected()) {
          mqttClient.disconnect();
        }
        doSyncClientClose(mqttClient);
      }
    } catch (MqttException mqtte) {
      log.error("Could not close connection", mqtte);
      forceCloseSyncClientConnection(mqttClient);
    }
  }

  public void forceCloseSyncClientConnection(MqttClient mqttClient) {
    log.debug("Force Close Mqtt Client");
    try {
      if (mqttClient != null) {
        if (mqttClient.isConnected()) {
          mqttClient.disconnectForcibly();
        }
        doSyncClientClose(mqttClient);
      }
    } catch (Exception expt) {
      log.trace("Could not force close connection", expt);
    }
  }

  private void doSyncClientClose(MqttClient mqttClient) throws MqttException {
    mqttClient.setCallback(null);
    mqttClient.close();
    mqttClients.remove(mqttClient.getClientId());
  }

  /**
   * Access method for getting a synchronous MqttClient for producer/consumer
   */
  MqttClient getOrCreateSyncClient(String clientId) throws CoreException {
    if (clientId == null || !mqttClients.containsKey(clientId)) {
      return newSyncClient();
    }
    return getSyncClient(clientId);
  }

  MqttClient getSyncClient(String clientId) throws CoreException {
    return mqttClients.get(clientId);
  }

  /**
   * Access method for getting a new asynchronous MqttAsyncClient for producer/consumer
   */
  private MqttAsyncClient newAsyncClient() throws CoreException {
    try {
      String clientId = getUniqueId() + "-" + MqttAsyncClient.generateClientId();
      MqttAsyncClient mqttAsyncClient = new MqttAsyncClient(serverUri, clientId, createMqttClientPersistence());
      mqttAsyncClients.put(clientId, mqttAsyncClient);
      return mqttAsyncClient;
    } catch (MqttException mqtte) {
      throw new CoreException("Mqtt Async Client could not be initialized", mqtte);
    }
  }

  public void startAsyncClientConnection(MqttAsyncClient mqttAsyncClient) throws CoreException {
    log.debug("Connect Mqtt Client");
    try {
      if (!mqttAsyncClient.isConnected()) {
        mqttAsyncClient.connect(initMqttConnectOptions());
      }
    } catch (MqttException | PasswordException | UnsupportedEncodingException mqtte) {
      throw new CoreException(mqtte);
    }
  }

  public void stopAsyncClientConnection(MqttAsyncClient mqttAsyncClient) {
    try {
      if (mqttAsyncClient != null && mqttAsyncClient.isConnected()) {
        log.debug("Disconnect Async Mqtt Client [{}]", mqttAsyncClient.getClientId());
        mqttAsyncClient.disconnect();
      }
    } catch (MqttException mqtte) {
      log.error("Could not stop connection", mqtte);
    }
  }

  public void closeAsyncClientConnection(MqttAsyncClient mqttAsyncClient) {
    try {
      if (mqttAsyncClient != null) {
        log.debug("Close Async Mqtt Client [{}]", mqttAsyncClient.getClientId());
        if (mqttAsyncClient.isConnected()) {
          mqttAsyncClient.disconnect();
        }
        doAsyncClientClose(mqttAsyncClient);
      }
    } catch (MqttException mqtte) {
      log.error("Could not close connection", mqtte);
      forceCloseAsyncClientConnection(mqttAsyncClient);
    }
  }

  public void forceCloseAsyncClientConnection(MqttAsyncClient mqttAsyncClient) {
    log.debug("Force Close Async Mqtt Client");
    try {
      if (mqttAsyncClient != null) {
        mqttAsyncClient.disconnectForcibly();
        doAsyncClientClose(mqttAsyncClient);
      }
    } catch (Exception expt) {
      log.trace("Could not force close connection", expt);
    }
  }

  private void doAsyncClientClose(MqttAsyncClient mqttAsyncClient) throws MqttException {
    mqttAsyncClient.setCallback(null);
    mqttAsyncClient.close();
    mqttAsyncClients.remove(mqttAsyncClient.getClientId());
  }

  /**
   * Access method for getting an asynchronous MqttClient for producer/consumer
   */
  MqttAsyncClient getOrCreateAsyncClient(String clientId) throws CoreException {
    if (clientId == null || !mqttAsyncClients.containsKey(clientId)) {
      return newAsyncClient();
    }
    return getAsyncClient(clientId);
  }

  MqttAsyncClient getAsyncClient(String clientId) throws CoreException {
    return mqttAsyncClients.get(clientId);
  }

  private MqttConnectOptions initMqttConnectOptions() throws PasswordException, UnsupportedEncodingException {
    if (options == null) {
      options = new MqttConnectOptions();
      if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
        options.setUserName(username);
        options.setPassword(Password.decode(password).toCharArray());
      }
      options.setMqttVersion(protocolVersion.getVersionValue());
      options.setCleanSession(cleanSession);

      int connectionTimeoutSeconds = timeIntervalToSecond(getConnectionTimeout());
      if (connectionTimeoutSeconds > -1) {
        options.setConnectionTimeout(connectionTimeoutSeconds);
      }

      int keepAliveIntervalSeconds = timeIntervalToSecond(getKeepAliveInterval());
      if (keepAliveIntervalSeconds > -1) {
        options.setKeepAliveInterval(keepAliveIntervalSeconds);
      }

      Properties sslContextProperties = createSslContextProperties(sslProperties);
      if (sslContextProperties.size() > 0) {
        options.setSSLProperties(sslContextProperties);
      }

      if (lastWill != null) {
        byte[] willPayload = lastWill.getPayload() != null ? lastWill.getPayload().getBytes(lastWill.getPayloadCharEncoding()) : null;
        options.setWill(lastWill.getTopic(), willPayload, lastWill.getQos(), lastWill.getRetained());
      }
      options.setAutomaticReconnect(true);
    }
    return options;
  }

  private MqttClientPersistence createMqttClientPersistence() {
    String userDir = System.getProperty("user.dir");
    if (!userDir.endsWith(File.separator)) {
      userDir = userDir + File.separator;
    }
    return new MqttDefaultFilePersistence(userDir + PERSISTENCE_LOCATION);
  }

  Properties createSslContextProperties(KeyValuePairSet sslProperties) throws PasswordException {
    Properties sslContextProperties = new Properties();
    for (KeyValuePair kvp : sslProperties.getKeyValuePairs()) {
      SslProperty sslProperty = SslProperty.getIgnoreCase(kvp.getKey());
      sslProperty.applyProperty(sslContextProperties, kvp.getValue());
    }
    return sslContextProperties;
  }

  /**
   * The MQTT endpoint
   *
   * @return serverUri
   */
  public String getServerUri() {
    return serverUri;
  }

  /**
   * The MQTT endpoint
   *
   * @param serverUri
   */
  public void setServerUri(String serverUri) {
    this.serverUri = serverUri;
  }

  /**
   * The username for the MQTT endpoint
   *
   * @return username
   */
  public String getUsername() {
    return username;
  }

  /**
   * The username for the MQTT endpoint
   *
   * @param username
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * The password for the MQTT endpoint. Can be encoded.
   *
   * @return password
   */
  public String getPassword() {
    return password;
  }

  /**
   * The password for the MQTT endpoint. Can be encoded.
   *
   * @param password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  public MqttProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  /**
   * Sets the MQTT version. The default action is to connect with version 3.1.1, and to fall back to
   * 3.1 if that fails. Version 3.1.1 or 3.1 can be selected specifically, with no fall back, by
   * using the MQTT_VERSION_3_1_1 or MQTT_VERSION_3_1 options respectively.
   *
   * @param protocolVersion
   */
  public void setProtocolVersion(MqttProtocolVersion protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  /**
   * Returns whether the client and server should remember state for the client across reconnects.
   *
   * @return cleanSession
   */
  public boolean getCleanSession() {
    return cleanSession;
  }

  /**
   * Sets whether the client and server should remember state across restarts and reconnects.
   * <ul>
   * <li>If set to false both the client and server will maintain state across restarts of the
   * client, the server and the connection. As state is maintained:
   * <ul>
   * <li>Message delivery will be reliable meeting the specified QOS even if the client, server or
   * connection are restarted.
   * <li>The server will treat a subscription as durable.
   * </ul>
   * <lI>If set to true the client and server will not maintain state across restarts of the client,
   * the server or the connection. This means
   * <ul>
   * <li>Message delivery to the specified QOS cannot be maintained if the client, server or
   * connection are restarted
   * <li>The server will treat a subscription as non-durable
   * </ul>
   *
   * @param cleanSession
   */
  public void setCleanSession(boolean cleanSession) {
    this.cleanSession = cleanSession;
  }

  /**
   * Returns the connection timeout value.
   *
   * @return the connection timeout value.
   */
  public TimeInterval getConnectionTimeout() {
    return connectionTimeout;
  }

  /**
   * Sets the connection timeout value. This value, measured in seconds, defines the maximum time
   * interval the client will wait for the network connection to the MQTT server to be established.
   * The default timeout is 30 seconds. A value of 0 disables timeout processing meaning the client
   * will wait until the network connection is made successfully or fails.
   *
   * @param connectionTimeout
   */
  public void setConnectionTimeout(TimeInterval connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  /**
   * Returns the "keep alive" interval.
   *
   * @return the keep alive interval.
   */
  public TimeInterval getKeepAliveInterval() {
    return keepAliveInterval;
  }

  /**
   * Sets the "keep alive" interval. This value, measured in seconds, defines the maximum time
   * interval between messages sent or received. It enables the client to detect if the server is no
   * longer available, without having to wait for the TCP/IP timeout. The client will ensure that at
   * least one message travels across the network within each keep alive period. In the absence of a
   * data-related message during the time period, the client sends a very small "ping" message,
   * which the server will acknowledge. A value of 0 disables keepalive processing in the client.
   * <p>
   * The default value is 60 seconds
   * </p>
   *
   * @param keepAliveInterval the interval.
   */
  public void setKeepAliveInterval(TimeInterval keepAliveInterval) {
    this.keepAliveInterval = keepAliveInterval;
  }

  /**
   * Returns the SSL properties for the connection.
   *
   * @return the properties for the SSL connection
   */
  public KeyValuePairSet getSslProperties() {
    return sslProperties;
  }

  /**
   * Sets the SSL properties for the connection. Note that these properties are only valid if an
   * implementation of the Java Secure Socket Extensions (JSSE) is available. These properties are
   * <em>not</em> used if a SocketFactory has been set using
   * {@link MqttConnectOptions#setSocketFactory(SocketFactory)}. The following properties can be
   * used:
   * </p>
   * <dl>
   * <dt>protocol</dt>
   * <dd>One of: SSL, SSLv3, TLS, TLSv1, SSL_TLS.</dd>
   * <dt>contextProvider
   * <dd>Underlying JSSE provider. For example "IBMJSSE2" or "SunJSSE"</dd>
   *
   * <dt>keyStore</dt>
   * <dd>The name of the file that contains the KeyStore object that you want the KeyManager to use.
   * For example /mydir/etc/key.p12</dd>
   *
   * <dt>keyStorePassword</dt>
   * <dd>The password for the KeyStore object that you want the KeyManager to use. The password can
   * either be in plain-text, or may be obfuscated using the static method:
   * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>. This obfuscates the
   * password using a simple and insecure XOR and Base64 encoding mechanism. Note that this is only
   * a simple scrambler to obfuscate clear-text passwords.</dd>
   *
   * <dt>keyStoreType</dt>
   * <dd>Type of key store, for example "PKCS12", "JKS", or "JCEKS".</dd>
   *
   * <dt>keyStoreProvider</dt>
   * <dd>Key store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
   *
   * <dt>trustStore</dt>
   * <dd>The name of the file that contains the KeyStore object that you want the TrustManager to
   * use.</dd>
   *
   * <dt>trustStorePassword</dt>
   * <dd>The password for the TrustStore object that you want the TrustManager to use. The password
   * can either be in plain-text, or may be obfuscated using the static method:
   * <code>com.ibm.micro.security.Password.obfuscate(char[] password)</code>. This obfuscates the
   * password using a simple and insecure XOR and Base64 encoding mechanism. Note that this is only
   * a simple scrambler to obfuscate clear-text passwords.</dd>
   *
   * <dt>trustStoreType</dt>
   * <dd>The type of KeyStore object that you want the default TrustManager to use. Same possible
   * values as "keyStoreType".</dd>
   *
   * <dt>trustStoreProvider</dt>
   * <dd>Trust store provider, for example "IBMJCE" or "IBMJCEFIPS".</dd>
   *
   * <dt>enabledCipherSuites</dt>
   * <dd>A list of which ciphers are enabled. Values are dependent on the provider, for example:
   * SSL_RSA_WITH_AES_128_CBC_SHA;SSL_RSA_WITH_3DES_EDE_CBC_SHA.</dd>
   *
   * <dt>keyManager</dt>
   * <dd>Sets the algorithm that will be used to instantiate a KeyManagerFactory object instead of
   * using the default algorithm available in the platform. Example values: "IbmX509" or
   * "IBMJ9X509".</dd>
   *
   * <dt>trustManager</dt>
   * <dd>Sets the algorithm that will be used to instantiate a TrustManagerFactory object instead of
   * using the default algorithm available in the platform. Example values: "PKIX" or
   * "IBMJ9X509".</dd>
   * </dl>
   */
  public void setSslProperties(KeyValuePairSet sslProperties) {
    this.sslProperties = sslProperties;
  }

  public MqttLastWill getLastWill() {
    return lastWill;
  }

  /**
   * Sets the "Last Will and Testament" (LWT) for the connection. In the event that this client
   * unexpectedly loses its connection to the server, the server will publish a message to itself
   * using the supplied details.
   *
   * @param lastWill
   */
  public void setLastWill(MqttLastWill lastWill) {
    this.lastWill = lastWill;
  }

  MqttConnectOptions retrieveOptions() {
    return options;
  }

}
