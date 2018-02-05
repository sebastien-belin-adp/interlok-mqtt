package com.adaptris.core.mqtt;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.Ignore;
import org.junit.Test;

import com.adaptris.util.KeyValuePair;
import com.adaptris.util.KeyValuePairSet;

public class MqttConnectionTest {

  @Test
  public void testInit() throws Exception {
    MqttConnection mqttConnection = initMqttConnectionOptions();

    mqttConnection.init();

    MqttConnectOptions retrieveOptions = mqttConnection.retrieveOptions();
    assertEquals("username", retrieveOptions.getUserName());
    assertArrayEquals("password".toCharArray(), retrieveOptions.getPassword());
  }

  @Test
  public void testInitWithSsl() throws Exception {
    MqttConnection mqttConnection = new MqttConnection();
    KeyValuePairSet sslProperties = new KeyValuePairSet();
    sslProperties.add(new KeyValuePair("trustStore", "/path/to/file"));
    sslProperties.add(new KeyValuePair("trustStorePassword", "password"));
    mqttConnection.setSslProperties(sslProperties);

    mqttConnection.init();

    MqttConnectOptions retrieveOptions = mqttConnection.retrieveOptions();
    assertEquals(2, retrieveOptions.getSSLProperties().size());
    assertEquals("/path/to/file", retrieveOptions.getSSLProperties().get("com.ibm.ssl.trustStore"));
    assertEquals("password", retrieveOptions.getSSLProperties().get("com.ibm.ssl.trustStorePassword"));
  }

  // We're not really testing anything here
  @Ignore
  @Test
  public void testStart() throws Exception {
    MqttConnection mqttConnectionOptions = initMqttConnectionOptions();

    try {
      mqttConnectionOptions.init();
      mqttConnectionOptions.start();
      // assert
      // verify
    } finally {
      mqttConnectionOptions.stop();
    }
  }

  @Test
  public void testGetNullSyncClient() throws Exception {
    MqttConnection mqttConnection = initMqttConnectionOptions();

    MqttClient syncClient = mqttConnection.getOrCreateSyncClient(null);
    assertNotNull(syncClient);
    assertEquals(syncClient.getServerURI(), "tcp://127.0.0.1:1883");

    MqttClient sameSyncClient = mqttConnection.getOrCreateSyncClient(syncClient.getClientId());
    assertEquals(syncClient, sameSyncClient);
  }

  @Test
  public void testGetNullASyncClient() throws Exception {
    MqttConnection mqttConnection = initMqttConnectionOptions();

    MqttAsyncClient asyncClient = mqttConnection.getOrCreateAsyncClient(null);
    assertNotNull(asyncClient);
    assertEquals(asyncClient.getServerURI(), "tcp://127.0.0.1:1883");

    MqttAsyncClient sameAsyncClient = mqttConnection.getOrCreateAsyncClient(asyncClient.getClientId());
    assertEquals(asyncClient, sameAsyncClient);
  }

  private MqttConnection initMqttConnectionOptions() {
    MqttConnection mqttConnection = new MqttConnection();
    mqttConnection.setUsername("username");
    mqttConnection.setPassword("password");
    mqttConnection.setServerUri("tcp://127.0.0.1:1883");
    return mqttConnection;
  }

}
