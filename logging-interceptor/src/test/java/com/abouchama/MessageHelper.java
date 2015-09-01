package com.abouchama;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class MessageHelper {
  private final static DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

  public static Message createMsgFromXmlFile(File file) throws ParserConfigurationException,
      SAXException, IOException, JMSException, TransformerException {
    final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    final Document xmlDoc = dBuilder.parse(file);
    xmlDoc.getDocumentElement().normalize();
    final Element root = xmlDoc.getDocumentElement();
    final TextMessage msg = new ActiveMQTextMessage();
    // clone the message header properties
    msg.setJMSCorrelationID(UUID.randomUUID().toString());
    msg.setStringProperty("MessageIdentifier", getExfHeaderAttribute(root, "messageIdentifier"));
    msg.setStringProperty("ProtocolId", getExfHeaderAttribute(root, "protocolID"));
    msg.setStringProperty("Sender", getExfHeaderAttribute(root, "sender"));
    msg.setStringProperty("Receiver", getExfHeaderAttribute(root, "receiver"));
    msg.setStringProperty("ProtocolVersion", getExfHeaderAttribute(root, "protocolVersion"));
    msg.setStringProperty("Action", getExfHeaderAttribute(root, "action"));
    // clone the message body
    final TransformerFactory tf = TransformerFactory.newInstance();
    final Transformer transformer = tf.newTransformer();
    final StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(root.getElementsByTagName("body").item(0)),
        new StreamResult(writer));
    msg.setText(writer.getBuffer().toString());
    return msg;
  }

  private static String getExfHeaderAttribute(Element e, String tagName) {
    final Element elem = (Element) e.getElementsByTagName("header").item(0);
    final Node n = elem.getElementsByTagName(tagName).item(0);
    return (null != n) ? n.getTextContent() : null;
  }
}
