package com.abouchama;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.activemq.broker.BrokerPluginSupport;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageDispatch;
import org.apache.commons.lang.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs messages received and sent.
 */
// disable warning due to BrokerPluginSupport
@SuppressWarnings("unchecked")
public class LoggingInterceptorPlugin extends BrokerPluginSupport {
  private static final Logger log = LoggerFactory.getLogger(LoggingInterceptorPlugin.class);
  private static final String LOG_ERR_STRING = "Error logging message where messageId={}, error={}";
  // the locale is not used as date format is provided
  private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(
      "yyyy-MM-dd HH:mm:ss.SSS", TimeZone.getTimeZone("UTC"), new Locale("en", "US"));
  // estimation of the mean length of log string
  private static final int INITIAL_CAPACITY = 248;
  private String brokerName;

  @Override
  public void start() throws Exception {
    super.start();
    brokerName = super.getBrokerName();
  }

  /**
   * Called when the broker receives a message.
   */
  @Override
  public void send(ProducerBrokerExchange producerExchange, Message msg) throws Exception {
    super.send(producerExchange, msg);
    final ActiveMQDestination d = msg.getDestination();

    if (null != d) {
      final String destination = d.getPhysicalName();

      if ((null != destination) && destination.endsWith(".OUT")) {
        printLog(msg, destination);
      }
    }
  }

  /**
   * Called when the broker sends a message.
   */
  @Override
  public void postProcessDispatch(MessageDispatch messageDispatch) {
    super.postProcessDispatch(messageDispatch);
    final Message msg = messageDispatch.getMessage();
    final ActiveMQDestination d = msg.getDestination();

    if (null != d) {
      final String destination = d.getPhysicalName();

      if ((null != destination) && destination.contains(".IN.")) {
        printLog(msg, destination);
      }
    }
  }

  private void printLog(Message msg, String destination) {
    try {
      final StringBuilder sb = new StringBuilder(INITIAL_CAPACITY);
      sb.append(DATE_FORMAT.format(new Date())).append(',');
      sb.append(msg.getMessageId()).append(',');
      sb.append(msg.getProperty("Action")).append(',');
      sb.append(msg.getProperty("ProtocolId")).append(',');
      sb.append(msg.getProperty("ProtocolVersion")).append(',');
      sb.append(msg.getProperty("Sender")).append(',');
      sb.append(msg.getProperty("Receiver")).append(',');
      sb.append(brokerName).append(',');
      sb.append(msg.getProperty("MessageIdentifier")).append(',');
      sb.append(msg.getCorrelationId()).append(',');
      sb.append(destination);
      log.info(sb.toString());
    }
    catch (IOException e) {
      log.error(LOG_ERR_STRING, msg.getMessageId(), e.getMessage());
    }
  }
}
