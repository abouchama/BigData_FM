package com.abouchama;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.xml.sax.SAXException;

public class ActiveMQMsgSender {
  private final String queue;
  private final ActiveMQConnectionFactory connectionFactory;

  public ActiveMQMsgSender(String brokerUrl, String outputQueue) {
    queue = outputQueue;
    connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
  }

  public void send(Message m) throws JMSException {
    ActiveMQConnection connection = null;
    try {
      connection = (ActiveMQConnection) connectionFactory.createConnection();
      final Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
      final MessageProducer producer = session.createProducer(session.createQueue(queue));
      connection.start();
      producer.send(m);
      session.commit();
    }
    finally {
      // from the activemq doc: There is no need to close the sessions,
      // producers, and consumers of a closed connection.
      if (connection != null) {
        connection.stop();
        connection.close();
      }
    }
  }

  public void send(File msg) throws ParserConfigurationException, SAXException, IOException,
      JMSException, TransformerException {
    send(MessageHelper.createMsgFromXmlFile(msg));
  }

  public void sendAsPersistent(File msg) throws ParserConfigurationException, SAXException,
      IOException, JMSException, TransformerException {
    final Message m = MessageHelper.createMsgFromXmlFile(msg);
    m.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
    send(m);
  }

  public boolean sendAsPersistentWithTimeout(final File msgFile, long timeout, TimeUnit u) {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final Future<Boolean> f = executor.submit(new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        sendAsPersistent(msgFile);
        return Boolean.TRUE;
      }
    });

    try {
      return f.get(timeout, u).booleanValue();
    }
    catch (InterruptedException | ExecutionException | TimeoutException e) {
      f.cancel(true);
      executor.shutdownNow();
      return false;
    }
  }
}
