/*
 * Copyright 2015 Adaptris Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adaptris.core.mqtt;

import static com.adaptris.core.PortManager.nextUnusedPort;
import static com.adaptris.core.PortManager.release;
import static com.adaptris.core.jms.JmsConfig.DEFAULT_PAYLOAD;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.jms.JMSException;
//import javax.naming.Context;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.Destination;
import org.apache.activemq.command.ActiveMQDestination;
//import org.apache.activemq.jndi.ActiveMQInitialContextFactory;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.commons.io.FileUtils;

import com.adaptris.core.AdaptrisMessage;
import com.adaptris.core.AdaptrisMessageFactory;
//import com.adaptris.core.jms.activemq.BasicActiveMqImplementation;
//import com.adaptris.core.jms.jndi.StandardJndiImplementation;
import com.adaptris.core.stubs.ExternalResourcesHelper;
import com.adaptris.util.GuidGenerator;
import com.adaptris.util.IdGenerator;
//import com.adaptris.util.KeyValuePair;
import com.adaptris.util.PlainIdGenerator;

public class EmbeddedActiveMqMqtt {

  private static final String HOST = "localhost";
  private static final String DEF_URL_PREFIX = "mqtt://" + HOST + ":";
  private BrokerService broker = null;
  private final String brokerName;
  private File brokerDataDir;
  private final Integer port;

  private static IdGenerator nameGenerator;
  static {
    try {
      nameGenerator = new GuidGenerator();
    }
    catch (Exception e) {
      nameGenerator = new PlainIdGenerator("-");
    }
  }

  public EmbeddedActiveMqMqtt() throws Exception {
    brokerName = createSafeUniqueId(this);
    port = nextUnusedPort(61616);
  }

  public String getName() {
    return brokerName;
  }

  public void start() throws Exception {
    brokerDataDir = createTempFile(true);
    broker = createBroker();
    broker.start();
    while (!broker.isStarted()) {
      Thread.sleep(100);
    }
  }

  public BrokerService createBroker() throws Exception {
    BrokerService br = new BrokerService();
    br.setBrokerName(brokerName);
    br.addConnector(DEF_URL_PREFIX + port);
    br.setUseJmx(false);
    br.setPersistent(false);
    br.setDeleteAllMessagesOnStartup(true);
    br.setDataDirectoryFile(brokerDataDir);
    br.setPersistent(false);
    br.setPersistenceAdapter(new MemoryPersistenceAdapter());
    br.getSystemUsage().getMemoryUsage().setLimit(1024L * 1024 * 20);
    br.getSystemUsage().getTempUsage().setLimit(1024L * 1024 * 20);
    br.getSystemUsage().getStoreUsage().setLimit(1024L * 1024 * 20);
    br.getSystemUsage().getJobSchedulerUsage().setLimit(1024L * 1024 * 20);
    return br;
  }

  private File createTempFile(boolean isDir) throws IOException {
    File result = File.createTempFile("AMQ-", "");
    result.delete();
    if (isDir) {
      result.mkdirs();
    }
    return result;
  }

  public void destroy() {
    new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          stop();
          release(port);
        }
        catch (Exception e) {

        }
      }
    }).start();
  }

  public void stop() throws Exception {
    if (broker != null) {
      broker.stop();
      broker.waitUntilStopped();
      FileUtils.deleteDirectory(brokerDataDir);
    }
  }

  public MqttConnection getMqttConnection() {
    return applyCfg(new MqttConnection());
  }

  private MqttConnection applyCfg(MqttConnection con) {
    con.setServerUri("tcp://" + HOST + ":" + port);
    return con;
  }

  public static AdaptrisMessage createMessage(AdaptrisMessageFactory fact) {
    AdaptrisMessage msg = fact == null ? AdaptrisMessageFactory.getDefaultInstance().newMessage(DEFAULT_PAYLOAD) : fact
        .newMessage(DEFAULT_PAYLOAD);
    return msg;
  }

  public static AdaptrisMessage addBlobUrlRef(AdaptrisMessage msg, String key) {
    msg.addMetadata(key, ExternalResourcesHelper.createUrl());
    return msg;
  }

  public ActiveMQConnection createConnection() throws JMSException {
    ActiveMQConnectionFactory fact = new ActiveMQConnectionFactory("tcp://" + getName());
    return (ActiveMQConnection) fact.createConnection();
  }

  public long messagesOnQueue(String queueName) throws Exception {
    Broker b = broker.getBroker();

    Map<ActiveMQDestination, Destination> map = b.getDestinationMap();
    for (ActiveMQDestination dest : map.keySet()) {
      if (dest.isQueue() && dest.getPhysicalName().equals(queueName)) {
        Destination queueDest = map.get(dest);
        return queueDest.getDestinationStatistics().getMessages().getCount();
      }
    }
    return -1;
  }

  public static String createSafeUniqueId(Object o) {
    return nameGenerator.create(o).replaceAll(":", "").replaceAll("-", "");
  }

}
