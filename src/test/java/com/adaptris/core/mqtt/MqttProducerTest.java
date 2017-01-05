package com.adaptris.core.mqtt;

import static com.adaptris.core.jms.JmsProducerCase.assertMessages;

import com.adaptris.core.ConfiguredConsumeDestination;
import com.adaptris.core.ConfiguredProduceDestination;
import com.adaptris.core.ProducerCase;
import com.adaptris.core.StandaloneConsumer;
import com.adaptris.core.StandaloneProducer;
import com.adaptris.core.stubs.MockMessageListener;

public class MqttProducerTest extends ProducerCase {

  public MqttProducerTest(String params) {
    super(params);
  }

  public void testSingleProduce() throws Exception {
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

  private String getTopicName() {
    return "mqtt/topic/" + getName();
  }

  private StandaloneConsumer buildStandaloneMqttConsumer(EmbeddedActiveMqMqtt activeMqBroker, String topicName) {
    MqttConsumer mqttConsumer = new MqttConsumer(new ConfiguredConsumeDestination(topicName));
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
    MqttProducer producer = new MqttProducer();
    producer.setDestination(new ConfiguredProduceDestination("SampleQueue"));

    MqttConnection conn = new MqttConnection();
    conn.setServerUri("tcp://localhost:1883");
    conn.setUsername("My Access Key");
    conn.setPassword("My Security Key");
    StandaloneProducer result = new StandaloneProducer(conn, producer);
    return result;
  }


}
