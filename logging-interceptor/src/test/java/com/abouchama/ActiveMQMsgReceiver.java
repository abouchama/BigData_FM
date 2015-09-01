package com.abouchama;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

public class ActiveMQMsgReceiver {

  private final String url;

  private final static long TIMEOUT = 1000;

  public ActiveMQMsgReceiver(String url) {
    this.url = url;
  }

  public List<Message> receive(String queue) throws URISyntaxException, JMSException {
    // connects and consumes any msg received on the queue
    return sendWithCommitOrRollback(queue, new SessionAction() {

      @Override
      public void call(Session s) throws JMSException {
        s.commit();
      }
    });
  }

  public int receiveWithRollBack(String queue) throws URISyntaxException, JMSException {
    // connects and rolls back any msg received on the queue

    return sendWithCommitOrRollback(queue, new SessionAction() {

      @Override
      public void call(Session s) throws JMSException {
        s.rollback();
      }
    }).size();
  }

  private List<Message> sendWithCommitOrRollback(String queue, SessionAction action)
      throws JMSException, URISyntaxException {
    // connects and does an action on any msg received on the queue
    final URI brokerUrl = new URI(url);
    final ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
    ActiveMQConnection connection = null;
    final List<Message> messages = new ArrayList<>();

    try {
      connection = (ActiveMQConnection) connectionFactory.createConnection();
      connection.getRedeliveryPolicy().setMaximumRedeliveries(0);
      connection.start();
      final Session session = connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
      final MessageConsumer consumer = session.createConsumer(session.createQueue(queue));
      Message message = null;

      while (null != (message = consumer.receive(TIMEOUT))) {
        messages.add(message);
      }

      action.call(session);
    }
    finally {
      // from the activemq doc: There is no need to close the sessions,
      // producers, and consumers of a closed connection.
      if (null != connection) {
        connection.stop();
        connection.close();
      }
    }
    return messages;
  }

  private static interface SessionAction {
    public void call(Session session) throws JMSException;
  }
}
