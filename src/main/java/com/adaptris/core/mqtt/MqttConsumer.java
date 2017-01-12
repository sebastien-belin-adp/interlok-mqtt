package com.adaptris.core.mqtt;

import javax.validation.Valid;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adaptris.annotation.AdapterComponent;
import com.adaptris.annotation.AdvancedConfig;
import com.adaptris.annotation.ComponentProfile;
import com.adaptris.annotation.DisplayOrder;
import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageConsumerImp;
import com.adaptris.core.ConsumeDestination;
import com.adaptris.core.CoreException;
import com.adaptris.core.licensing.License;
import com.adaptris.core.licensing.License.LicenseType;
import com.adaptris.core.licensing.LicenseChecker;
import com.adaptris.core.licensing.LicensedComponent;
import com.adaptris.util.TimeInterval;
import com.thoughtworks.xstream.annotations.XStreamAlias;

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
    recommended = {MqttConnection.class})
@DisplayOrder(order = {"destination", "timeToWait"})
public class MqttConsumer extends AdaptrisMessageConsumerImp implements /*LicensedComponent,*/ MqttCallbackExtended {

  @Valid
  @AdvancedConfig
  private TimeInterval timeToWait;

  private transient final Logger log = LoggerFactory.getLogger(this.getClass());
  private transient MqttClient mqttClient;
  private transient String topic;

  public MqttConsumer() {}

  public MqttConsumer(ConsumeDestination destination) {
    this();
    setDestination(destination);
  }

  @Override
  public void init() throws CoreException {
    if (retrieveConnection(MqttConnection.class) == null) {
      throw new CoreException("PahoMqttConnection is required");
    }

    if (getDestination() == null) {
      throw new CoreException("Destination is required");
    }
  }

  @Override
  public void prepare() throws CoreException {
    /*LicenseChecker.newChecker().checkLicense(this);*/
  }

  @Override
  public void start() throws CoreException {
    mqttClient = getMqtt();
    mqttClient.setCallback(this);
    topic = mqttClient.getTopic(getDestination().getDestination()).getName();
    if (timeToWait != null) {
      long timeToWaitInMillis = timeToWait.toMilliseconds();
      mqttClient.setTimeToWait(timeToWaitInMillis);
    }
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
      mqttClient.unsubscribe(topic);
      retrieveConnection(MqttConnection.class).stopSyncClientConnection(mqttClient);
    } catch (MqttException mqtte) {
      log.error("Could not unsuscribe from topic {}", topic, mqtte);
    }
  }

  @Override
  public void close() {
    try {
      mqttClient.unsubscribe(topic);
      retrieveConnection(MqttConnection.class).closeSyncClientConnection(mqttClient);
    } catch (MqttException mqtte) {
      log.error("Could not unsuscribe from topic {}", topic, mqtte);
    }
  }

  /*@Override
  public boolean isEnabled(License license) {
    return license.isEnabled(LicenseType.Standard);
  }*/

  @Override
  public void connectComplete(boolean reconnect, String serverURI) {
    try {
      log.debug("Connection comple. Subscribe to topic [{}]", topic);
      mqttClient.subscribe(topic);
    } catch (MqttException mqtte) {
      log.error("Failed to subscribe to topic [{}] after connetion complete.", topic, mqtte);
    }
  }

  @Override
  public void connectionLost(Throwable arg0) {
    log.debug("connectionLost called", arg0);
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken arg0) {
    log.debug("deliveryComplete called");
  }

  @Override
  public void messageArrived(String arg0, MqttMessage message) throws Exception {
    log.debug("messageArrived called");
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

}
