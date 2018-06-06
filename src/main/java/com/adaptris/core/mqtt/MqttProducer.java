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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.AutoPopulated;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageProducer;
import com.adaptris.core.CoreException;
import com.adaptris.core.ProduceDestination;
import com.adaptris.core.ProduceException;
import com.adaptris.core.ProduceOnlyProducerImp;
import com.adaptris.core.licensing.License;
import com.adaptris.core.licensing.License.LicenseType;
import com.adaptris.core.licensing.LicenseChecker;
import com.adaptris.core.licensing.LicensedComponent;
import com.adaptris.util.TimeInterval;
import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * {@link AdaptrisMessageProducer} implementation that sends messages to a MQTT topic.
 * <p>
 * MQTT topic receives only text therefore only the message payload is sent as a string.
 * </p>
 * 
 * @config mqtt-producer
 * @license STANDARD
 * @since 3.5.0
 */
@XStreamAlias("mqtt-producer")
@AdapterComponent
@ComponentProfile(summary = "Place message on a MQTT topic", tag = "producer,mqtt",
    recommended = {MqttConnection.class}, since = "3.5.0")
@DisplayOrder(order = {"destination", "qos", "retained", "timeToWait"})
public class MqttProducer extends ProduceOnlyProducerImp /*implements LicensedComponent*/ {

  private transient Map<String, String> cachedTopicURLs = new ConcurrentHashMap<String, String>();

  @NotNull
  @AutoPopulated
  // @Pattern(regexp = "[0-2]+")
  @Min(0)
  @Max(2)
  @AdvancedConfig
  private int qos = MqttConstants.QOS_DEFAULT;
  @AdvancedConfig
  private boolean retained = MqttConstants.RETAINED_DEFAULT;
  @Valid
  @AdvancedConfig
  private TimeInterval timeToWait;

  private transient MqttClient mqttClient;

  public MqttProducer() {}

  public MqttProducer(ProduceDestination destination) {
    this();
    setDestination(destination);
  }

  @Override
  public void init() throws CoreException {
    if(retrieveConnection(MqttConnection.class) == null) {
      throw new CoreException("PahoMqttConnection is required");
    }

    if(getDestination() == null) {
      throw new CoreException("Destination is required");
    }

    mqttClient = getMqtt();
    if (timeToWait != null) {
      long timeToWaitInMillis = timeToWait.toMilliseconds();
      mqttClient.setTimeToWait(timeToWaitInMillis);
    }
    startConnection();
  }

  private MqttClient getMqtt() throws CoreException {
    return retrieveConnection(MqttConnection.class).getOrCreateSyncClient(null);
  }

  @Override
  public void start() throws CoreException {
    startConnection();
  }

  private void startConnection() throws CoreException {
    if (!mqttClient.isConnected()) {
      log.debug("Connection is not started so we start it");
      retrieveConnection(MqttConnection.class).startSyncClientConnection(mqttClient);
    }
  }

  @Override
  public void stop() {
    retrieveConnection(MqttConnection.class).stopSyncClientConnection(mqttClient);
  }

  @Override
  public void close() {
    retrieveConnection(MqttConnection.class).closeSyncClientConnection(mqttClient);
    mqttClient = null;
  }

  @Override
  public void produce(AdaptrisMessage msg, ProduceDestination destination) throws ProduceException {
    try {
      // Resolve the Topic URL from the destination and the message (in case of metadata destinations for example)
      String topic = resolveTopic(msg);

      MqttMessage sendMessageRequest = new MqttMessage(encode(msg));
      applyExtraOptions(sendMessageRequest);
      log.debug("Publish message to topic [{}]", topic);
      mqttClient.publish(topic, sendMessageRequest);
      log.debug("Message published");
    } catch (Exception e) {
      throw new ProduceException(e);
    }
  }

  private void applyExtraOptions(MqttMessage sendMessageRequest) {
    sendMessageRequest.setQos(qos);
    sendMessageRequest.setRetained(retained);
  }

  private String resolveTopic(AdaptrisMessage msg) throws CoreException {
    // Get destination (possibly from message)
    final String topicName = getDestination().getDestination(msg);

    String topicURL = cachedTopicURLs.get(topicName);

    // It's not in the cache. Look up the topic url and cache it.
    if(topicURL == null) {
      topicURL = retrieveTopicFromMqtt(topicName);
      cachedTopicURLs.put(topicName, topicURL);
    }

    return topicURL;
  }

  private String retrieveTopicFromMqtt(String topicName) throws CoreException {
    return mqttClient.getTopic(topicName).getName();
  }

  @Override
  public void prepare() throws CoreException {
    /*LicenseChecker.newChecker().checkLicense(this);*/
  }

  /*@Override
  public boolean isEnabled(License license) {
    return license.isEnabled(LicenseType.Standard);
  }*/

  /**
   * Returns the quality of service for this message.
   * 
   * @return the quality of service to use, either 0, 1, or 2.
   */
  public int getQos() {
    return qos;
  }

  /**
   * Sets the quality of service for this message.
   * <ul>
   * <li>Quality of Service 0 - indicates that a message should be delivered at most once (zero or
   * one times). The message will not be persisted to disk, and will not be acknowledged across the
   * network. This QoS is the fastest, but should only be used for messages which are not valuable -
   * note that if the server cannot process the message (for example, there is an authorization
   * problem), then an {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)}. Also known as
   * "fire and forget".</li>
   * 
   * <li>Quality of Service 1 - indicates that a message should be delivered at least once (one or
   * more times). The message can only be delivered safely if it can be persisted, so the
   * application must supply a means of persistence using <code>MqttConnectOptions</code>. If a
   * persistence mechanism is not specified, the message will not be delivered in the event of a
   * client failure. The message will be acknowledged across the network. This is the default QoS.</li>
   * 
   * <li>Quality of Service 2 - indicates that a message should be delivered once. The message will
   * be persisted to disk, and will be subject to a two-phase acknowledgement across the network.
   * The message can only be delivered safely if it can be persisted, so the application must supply
   * a means of persistence using <code>MqttConnectOptions</code>. If a persistence mechanism is not
   * specified, the message will not be delivered in the event of a client failure.</li>
   * 
   * If persistence is not configured, QoS 1 and 2 messages will still be delivered in the event of
   * a network or server problem as the client will hold state in memory. If the MQTT client is
   * shutdown or fails and persistence is not configured then delivery of QoS 1 and 2 messages can
   * not be maintained as client-side state will be lost.
   * 
   * @param qos the "quality of service" to use. Set to 0, 1, 2.
   * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
   * @throws IllegalStateException if this message cannot be edited
   */
  public void setQos(int qos) {
    this.qos = qos;
  }

  /**
   * Returns whether or not messages should be retained by the server. For messages received from
   * the server, this method returns whether or not the message was from a current publisher, or was
   * "retained" by the server as the last message published on the topic.
   * 
   * @return <code>true</code> if the message should be retained by the server.
   */
  public boolean getRetained() {
    return retained;
  }

  /**
   * Whether or not the publish message should be retained by the messaging engine. Sending a
   * message with the retained set to <code>false</code> will clear the retained message from the
   * server. The default value is <code>false</code>
   * 
   * @param retained whether or not the messaging engine should retain the message.
   * @throws IllegalStateException if this message cannot be edited
   */
  public void setRetained(boolean retained) {
    this.retained = retained;
  }

  public TimeInterval getTimeToWait() {
    return timeToWait;
  }

  /**
   * Set the maximum time to wait for an action to complete.
   * <p>
   * Set the maximum time to wait for an action to complete before returning control to the invoking
   * application. Control is returned when:
   * <ul>
   * <li>the action completes
   * <li>or when the timeout if exceeded
   * <li>or when the client is disconnect/shutdown
   * <ul>
   * The default value is -1 which means the action will not timeout. In the event of a timeout the
   * action carries on running in the background until it completes. The timeout is used on methods
   * that block while the action is in progress.
   * </p>
   * 
   * @param timeToWait before the action times out. A value or 0 will wait until the action finishes
   *        and not timeout.
   */
  public void setTimeToWait(TimeInterval timeToWait) {
    this.timeToWait = timeToWait;
  }

}
