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

import javax.validation.Valid;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.annotation.Removal;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageConsumerImp;
import com.adaptris.core.ConsumeDestination;
import com.adaptris.core.CoreException;
import com.adaptris.core.util.DestinationHelper;
import com.adaptris.core.util.LoggingHelper;
import com.adaptris.interlok.util.Args;
import com.adaptris.util.TimeInterval;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * Paho MQTT implementation of <code>AdaptrisMessageConsumer</code>.
 * </p>
 *
 * @config mqtt-consumer
 * @license STANDARD
 * @since 3.5.0
 */
@XStreamAlias("mqtt-consumer")
@AdapterComponent
@ComponentProfile(summary = "Listen for MQTT messages on the specified topic", tag = "consumer,mqtt",
    recommended = {MqttConnection.class}, since = "3.5.0")
@DisplayOrder(order = {"topic", "destination", "timeToWait"})
@NoArgsConstructor
public class MqttConsumer extends AdaptrisMessageConsumerImp implements MqttCallbackExtended {

  @Valid
  @AdvancedConfig
  private TimeInterval timeToWait;

  /**
   * The consume destination is the MQTT topic.
   */
  @Getter
  @Setter
  @Deprecated
  @Valid
  @Removal(version = "4.0.0", message = "Use 'topic' instead")
  private ConsumeDestination destination;

  /**
   * The MQTT Topic
   *
   */
  @Getter
  @Setter
  // Needs to be @NotBlank when destination is removed.
  private String topic;


  private transient MqttClient mqttClient;
  private transient String topicName;
  private transient boolean destinationWarningLogged;

  @Override
  public void init() throws CoreException {
    Args.notNull(retrieveConnection(MqttConnection.class), "mqtt-connection");
    mqttClient = getMqtt();
    mqttClient.setCallback(this);
    topicName = mqttClient.getTopic(topicName()).getName();
    if (timeToWait != null) {
      long timeToWaitInMillis = timeToWait.toMilliseconds();
      mqttClient.setTimeToWait(timeToWaitInMillis);
    }
    startConnection();
  }

  @Override
  public void prepare() throws CoreException {
    DestinationHelper.logConsumeDestinationWarning(destinationWarningLogged,
        () -> destinationWarningLogged = true, getDestination(),
        "{} uses destination, use topic instead", LoggingHelper.friendlyName(this));
    DestinationHelper.mustHaveEither(getTopic(), getDestination());
  }

  @Override
  public void start() throws CoreException {
    startConnection();
    subscribeToTopic();
  }

  private void startConnection() throws CoreException {
    if (!mqttClient.isConnected()) {
      log.debug("Connection is not started so we start it");
      retrieveConnection(MqttConnection.class).startSyncClientConnection(mqttClient);
    }
  }

  private MqttClient getMqtt() throws CoreException {
    return retrieveConnection(MqttConnection.class).getOrCreateSyncClient(null);
  }

  @Override
  public void stop() {
    try {
      mqttClient.unsubscribe(topicName);
    } catch (MqttException mqtte) {
      log.error("Could not unsuscribe from topic {}", topicName, mqtte);
    }
    retrieveConnection(MqttConnection.class).stopSyncClientConnection(mqttClient);
  }

  @Override
  public void close() {
    try {
      if (mqttClient.isConnected()) {
        mqttClient.unsubscribe(topicName);
      }
    } catch (MqttException mqtte) {
      log.error("Could not unsuscribe from topic {}", topicName, mqtte);
    }
    retrieveConnection(MqttConnection.class).closeSyncClientConnection(mqttClient);
    mqttClient = null;
  }


  @Override
  public void connectComplete(boolean reconnect, String serverURI) {
    log.debug("Connection to server [{}] complete", serverURI);
    if (reconnect) {
      subscribeToTopic();
    }
  }

  private void subscribeToTopic() {
    try {
      log.debug("Subscribe to topic [{}]", topicName);
      mqttClient.subscribe(topicName);
    } catch (MqttException mqtte) {
      log.error("Failed to subscribe to topic [{}]", topicName, mqtte);
    }
  }

  @Override
  public void connectionLost(Throwable arg0) {
    log.debug("Connection Lost", arg0);
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken arg0) {
    log.debug("Message Delivery Complete");
  }

  @Override
  public void messageArrived(String arg0, MqttMessage message) throws Exception {
    log.debug("Message Arrived");
    AdaptrisMessage adaptrisMessage = decode(message.getPayload());
    retrieveAdaptrisMessageListener().onAdaptrisMessage(adaptrisMessage);
  }

  /**
   * Return the maximum time to wait for an action to complete.
   *
   * @return timeToWait
   */
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

  @Override
  protected String newThreadName() {
    return DestinationHelper.threadName(retrieveAdaptrisMessageListener(), getDestination());
  }

  public MqttConsumer withTopic(String s) {
    setTopic(s);
    return this;
  }

  private String topicName() {
    return DestinationHelper.consumeDestination(getTopic(), getDestination());
  }
}
