/*
 * Copyright (c) 2022 Uber Technologies, Inc.
 *
 * MODIFIED TO REUSE IN THIS PROJECT.
 */

package edu.ucr.cs.css;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/** Helper for class for parsing/writing xml files. */
public class XMLUtil {

  /**
   * Helper method for reading attributes of node located at /key_1/key_2/.../key_n (in the form of
   * {@code Xpath} query) from a {@link Document}.
   *
   * @param doc XML object to read values from.
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2:.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public static <T> DefaultXMLValueProvider<T> getValueFromAttribute(
      Document doc, String key, String attr, Class<T> klass) {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      Node node = (Node) xPath.compile(key).evaluate(doc, XPathConstants.NODE);
      if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
        Element eElement = (Element) node;
        return new DefaultXMLValueProvider<>(eElement.getAttribute(attr), klass);
      }
    } catch (XPathExpressionException ignored) {
      return new DefaultXMLValueProvider<>(null, klass);
    }
    return new DefaultXMLValueProvider<>(null, klass);
  }

  /**
   * Helper method for reading value of a node located at /key_1/key_2/.../key_n (in the form of
   * {@code Xpath} query) from a {@link Document}.
   *
   * @param doc XML object to read values from.
   * @param key Key to locate the value, can be nested in the form of {@code Xpath} query (e.g.
   *     /key1/key2/.../key_n).
   * @param klass Class type of the value in doc.
   * @return The value in the specified keychain cast to the class type given in parameter.
   */
  public static <T> DefaultXMLValueProvider<T> getValueFromTag(
      Document doc, String key, Class<T> klass) {
    try {
      XPath xPath = XPathFactory.newInstance().newXPath();
      Node node = (Node) xPath.compile(key).evaluate(doc, XPathConstants.NODE);
      if (node != null && node.getNodeType() == Node.ELEMENT_NODE) {
        Element eElement = (Element) node;
        return new DefaultXMLValueProvider<>(eElement.getTextContent(), klass);
      }
    } catch (XPathExpressionException ignored) {
      return new DefaultXMLValueProvider<>(null, klass);
    }
    return new DefaultXMLValueProvider<>(null, klass);
  }

  /** Helper class for setting default values when the key is not found. */
  static class DefaultXMLValueProvider<T> {
    final Object value;
    final Class<T> klass;

    DefaultXMLValueProvider(Object value, Class<T> klass) {
      this.klass = klass;
      if (value == null) {
        this.value = null;
      } else {
        String content = value.toString();
        switch (klass.getSimpleName()) {
          case "Integer":
            this.value = Integer.valueOf(content);
            break;
          case "Boolean":
            this.value = Boolean.valueOf(content);
            break;
          case "String":
            this.value = String.valueOf(content);
            break;
          default:
            throw new IllegalArgumentException(
                "Cannot extract values of type: "
                    + klass
                    + ", only Double|Boolean|String accepted.");
        }
      }
    }

    T orElse(T other) {
      return value == null ? other : klass.cast(this.value);
    }
  }
}