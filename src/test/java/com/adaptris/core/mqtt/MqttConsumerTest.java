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

import static com.adaptris.core.jms.JmsProducerCase.assertMessages;
import org.junit.Test;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.ConsumerCase;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.stubs.MockMessageListener;

public class MqttConsumerTest extends ConsumerCase {

  public MqttConsumerTest() {
    super();
  }

  @Test
  public void testSingleConsume() throws Exception {
    EmbeddedActiveMqMqtt activeMqBroker = new EmbeddedActiveMqMqtt();
    String topicName = getTopicName();

    try {
      activeMqBroker.start();

      StandaloneConsumer standaloneConsumer = buildStandaloneMqttConsumer(activeMqBroker, topicName);

      MockMessageListener messageListener = new MockMessageListener();
      standaloneConsumer.registerAdaptrisMessageListener(messageListener);

      StandaloneProducer standaloneProducer = buildStandaloneMqttProducer(activeMqBroker, topicName, false);

      execute(standaloneConsumer, standaloneProducer, EmbeddedActiveMqMqtt.createMessage(null), messageListener);
      assertMessages(messageListener, 1);
    } finally {
      activeMqBroker.destroy();
    }
  }

  @Test
  public void testSingleConsumeRetainedMessage() throws Exception {
    EmbeddedActiveMqMqtt activeMqBroker = new EmbeddedActiveMqMqtt();
    String topicName = getTopicName();

    try {
      activeMqBroker.start();

      StandaloneConsumer standaloneConsumer = buildStandaloneMqttConsumer(activeMqBroker, topicName);

      MockMessageListener messageListener = new MockMessageListener();
      standaloneConsumer.registerAdaptrisMessageListener(messageListener);

      StandaloneProducer standaloneProducer = buildStandaloneMqttProducer(activeMqBroker, topicName, true);

      execute(standaloneConsumer, standaloneProducer, EmbeddedActiveMqMqtt.createMessage(null), messageListener);
      assertMessages(messageListener, 1);

      StandaloneConsumer standaloneConsumerTwo = buildStandaloneMqttConsumer(activeMqBroker, topicName);

      MockMessageListener messageListenerTwo = new MockMessageListener();
      standaloneConsumerTwo.registerAdaptrisMessageListener(messageListenerTwo);

      try {
        start(standaloneConsumerTwo);
        waitForMessages(messageListenerTwo, 1);
        assertMessages(messageListenerTwo, 1);
      } finally {
        stop(standaloneConsumerTwo);
      }
    } finally {
      activeMqBroker.destroy();
    }
  }

  @Test
  public void testSingleConsumeRetainedMessageFalse() throws Exception {
    EmbeddedActiveMqMqtt activeMqBroker = new EmbeddedActiveMqMqtt();
    String topicName = getTopicName();

    try {
      activeMqBroker.start();

      StandaloneConsumer standaloneConsumer = buildStandaloneMqttConsumer(activeMqBroker, topicName);

      MockMessageListener messageListener = new MockMessageListener();
      standaloneConsumer.registerAdaptrisMessageListener(messageListener);

      StandaloneProducer standaloneProducer = buildStandaloneMqttProducer(activeMqBroker, topicName, false);

      execute(standaloneConsumer, standaloneProducer, EmbeddedActiveMqMqtt.createMessage(null), messageListener);
      assertMessages(messageListener, 1);

      StandaloneConsumer standaloneConsumerTwo = buildStandaloneMqttConsumer(activeMqBroker, topicName);

      MockMessageListener messageListenerTwo = new MockMessageListener();
      standaloneConsumerTwo.registerAdaptrisMessageListener(messageListenerTwo);

      try {
        start(standaloneConsumerTwo);
        Thread.sleep(DEFAULT_WAIT_INTERVAL);
        assertMessages(messageListenerTwo, 0);
      } finally {
        stop(standaloneConsumerTwo);
      }
    } finally {
      activeMqBroker.destroy();
    }
  }

  private String getTopicName() {
    return "mqtt/topic/" + getName();
  }

  private StandaloneConsumer buildStandaloneMqttConsumer(EmbeddedActiveMqMqtt activeMqBroker, String topicName) {
    MqttConsumer mqttConsumer = new MqttConsumer().withTopic(topicName);
    StandaloneConsumer standaloneConsumer = new StandaloneConsumer(activeMqBroker.getMqttConnection(), mqttConsumer);
    return standaloneConsumer;
  }

  private StandaloneProducer buildStandaloneMqttProducer(EmbeddedActiveMqMqtt activeMqBroker, String topicName, boolean retained) {
    MqttProducer mqttProducer = new MqttProducer(new ConfiguredProduceDestination(topicName));
    mqttProducer.setRetained(retained);
    StandaloneProducer standaloneProducer = new StandaloneProducer(activeMqBroker.getMqttConnection(), mqttProducer);
    return standaloneProducer;
  }

  @Override
  protected Object retrieveObjectForSampleConfig() {
    MqttConsumer mqttConsumer = new MqttConsumer().withTopic(getTopicName());
    MqttConnection conn = new MqttConnection();
    conn.setServerUri("tcp://localhost:1883");
    conn.setUsername("My Access Key");
    conn.setPassword("My Security Key");
    StandaloneConsumer result = new StandaloneConsumer(conn, mqttConsumer);
    return result;
  }

  @Override
  public boolean isAnnotatedForJunit4() {
    return true;
  }

}
