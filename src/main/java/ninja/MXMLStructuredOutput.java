package ninja;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import sirius.kernel.xml.*;

/**
 * Represents a {@link StructuredOutput} emitting XML data.
 * <p>
 * Can be used to construct XML using the <tt>StructuredOutput</tt> interface.
 */
public class MXMLStructuredOutput extends AbstractStructuredOutput {

    private final TransformerHandler transformerHandler2;
    protected OutputStream out;
    private int opensCalled = 0;

    public MXMLStructuredOutput(@Nonnull OutputStream out) {
        this(out, null);
    }

    public MXMLStructuredOutput(@Nonnull OutputStream output, @Nullable String doctype) {
        this(output, StandardCharsets.UTF_8, doctype);
    }

    public MXMLStructuredOutput(@Nonnull OutputStream output, @Nonnull Charset encoding, @Nullable String doctype) {
//        super(output, encoding, doctype);
        try {
            this.out = output;
            StreamResult streamResult = new StreamResult(out);
            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
            transformerHandler2 = tf.newTransformerHandler();
            Transformer serializer = transformerHandler2.getTransformer();
            if (doctype != null) {
                serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype);
            }
            serializer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
            serializer.setOutputProperty(OutputKeys.INDENT, "no");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformerHandler2.setResult(streamResult);
            transformerHandler2.startDocument();
        } catch (Exception exception) {
            throw handleOutputException(exception);
        }
    }


    /**
     * Provides a convenient way for {@link #beginArray(String)} prepending a namespace.
     *
     * @param namespace the namespace
     * @param name      the name of the array
     * @return the output itself for fluent method calls
     */
    public StructuredOutput beginNamespacedArray(@Nonnull String namespace, @Nonnull String name) {
        return beginArray(namespace + ":" + name);
    }

    @Override
    protected void endArray(String name) {
        try {
            transformerHandler2.endElement("", "", name);
        } catch (SAXException exception) {
            throw handleOutputException(exception);
        }
    }

    /**
     * Provides a convenient way for {@link #beginObject(String, Attribute...)} prepending a namespace.
     *
     * @param namespace  the namespace
     * @param name       the name of the object to create
     * @param attributes the attributes to add to the object
     * @return the output itself for fluent method calls
     */
    public StructuredOutput beginNamespacedObject(@Nonnull String namespace,
                                                  @Nonnull String name,
                                                  Attribute... attributes) {
        return beginObject(namespace + ":" + name, attributes);
    }

    @Override
    protected void endObject(String name) {
        try {
            transformerHandler2.endElement("", "", name);
        } catch (SAXException exception) {
            throw handleOutputException(exception);
        }
    }

    @Override
    public StructuredOutput beginResult() {
        return beginOutput("result");
    }

    @Override
    public StructuredOutput beginResult(String name) {
        return beginOutput(name);
    }

    /**
     * Provides a convenient way for {@link #beginResult(String)} prepending a namespace.
     *
     * @param namespace the namespace
     * @param name      the unqualified name
     * @return the output itself for fluent method calls
     */
    public StructuredOutput beginNamespacedResult(@Nonnull String namespace, @Nonnull String name) {
        return beginResult(namespace + ":" + name);
    }

    /**
     * Starts the output with the given root element and attributes
     *
     * @param rootElement the name of the root element of the generated document.
     * @param attr        the attributes for the root element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput beginOutput(@Nonnull String rootElement, Attribute... attr) {
        if (opensCalled == 0) {
            try {
                transformerHandler2.startDocument();
            } catch (SAXException exception) {
                throw handleOutputException(exception);
            }
        }
        opensCalled++;
        beginObject(rootElement, attr);

        return this;
    }

    /**
     * Provides a convenient way for {@link #beginOutput(String, Attribute...)} prepending a namespace.
     *
     * @param namespace   the namespace
     * @param rootElement the name of the root element of the generated document
     * @param attr        the attributes for the root element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput beginNamespacedOutput(@Nonnull String namespace,
                                                  @Nonnull String rootElement,
                                                  Attribute... attr) {
        return beginOutput(namespace + ":" + rootElement, attr);
    }

    /**
     * Creates a {@link AbstractStructuredOutput.TagBuilder} used to fluently create the root element.
     *
     * @param rootElement name of the root element
     * @return a tag builder which can be used to build the root element
     */
    @CheckReturnValue
    public TagBuilder buildBegin(@Nonnull String rootElement) {
        if (opensCalled == 0) {
            try {
                transformerHandler2.startDocument();
            } catch (SAXException exception) {
                throw handleOutputException(exception);
            }
        }
        opensCalled++;
        return buildObject(rootElement);
    }

    /**
     * Closes the output and this XML document.
     */
    public void endOutput() {
        endObject();
        if (opensCalled-- == 1) {
            super.endResult();
            try {
                transformerHandler2.endDocument();
                out.close();
            } catch (SAXException | IOException exception) {
                throw handleOutputException(exception);
            }
        }
    }

    @Override
    public void endResult() {
        endOutput();
    }

    @Override
    protected void startArray(String name) {
        try {
            transformerHandler2.startElement("", "", name, null);
        } catch (SAXException exception) {
            throw handleOutputException(exception);
        }
    }

    @Override
    protected void startObject(String name, Attribute... attributes) {
        try {
            AttributesImpl attrs = null;
            if (attributes != null) {
                attrs = new AttributesImpl();
                for (Attribute attr : attributes) {
                    attrs.addAttribute("", "", attr.getName(), "CDATA", String.valueOf(attr.getValue()));
                }
            }
            transformerHandler2.startElement("", "", name, attrs);
        } catch (SAXException exception) {
            throw handleOutputException(exception);
        }
    }

    @Override
    protected void writeProperty(String name, Object value) {
        try {
            transformerHandler2.startElement("", "", name, null);
            if (value != null) {
                String val = transformToStringRepresentation(value);
                transformerHandler2.characters(val.toCharArray(), 0, val.length());
            }
            transformerHandler2.endElement("", "", name);
        } catch (SAXException exception) {
            throw handleOutputException(exception);
        }
    }

    @Override
    protected void writeAmountProperty(String name, String amount) {
        writeProperty(name, amount);
    }

    /**
     * Provides a convenient way for {@link #property(String, Object, Attribute...)} prepending a namespace.
     *
     * @param namespace  the namespace
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes
     * @return the output itself for fluent method calls
     */
    public StructuredOutput namespacedProperty(@Nonnull String namespace,
                                               @Nonnull String name,
                                               @Nullable Object data,
                                               Attribute... attributes) {
        return property(namespace + ":" + name, data, attributes);
    }

    /**
     * Provides a convenient way for {@link #property(String, Object, Attribute...)} prepending a namespace.
     * <p>
     * This will create a property only if the specified data object is not null.
     * Else no property is created.
     *
     * @param namespace  the namespace
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes
     * @return the output itself for fluent method calls
     */
    public StructuredOutput namespacedPropertyIfFilled(@Nonnull String namespace,
                                                       @Nonnull String name,
                                                       @Nullable Object data,
                                                       Attribute... attributes) {
        if (data != null) {
            property(namespace + ":" + name, data, attributes);
        }
        return this;
    }

    /**
     * Adds a property to the current object.
     * <p>
     * This will create a property only if the specified data object is not null.
     * Else no property is created.
     *
     * @param name the name of the property
     * @param data the value of the property
     * @return the output itself for fluent method calls
     */
    public StructuredOutput propertyIfFilled(@Nonnull String name, @Nullable Object data) {
        if (data != null) {
            property(name, data);
        }
        return this;
    }

    /**
     * Adds a property containing attributes to the current object.
     *
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes for the element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput property(String name, Object data, Attribute... attributes) {
        startObject(name, attributes);
        text(data);
        endObject(name);
        return this;
    }

    /**
     * Adds a property containing attributes to the current object.
     * <p>
     * This will create a property only if the specified data object is not null.
     * Else no property is created.
     *
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes for the element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput propertyIfFilled(String name, Object data, Attribute... attributes) {
        if (data != null) {
            property(name, data, attributes);
        }
        return this;
    }

    /**
     * Adds a property containing attributes to the current object.
     * <p>
     * This will create a property with the specified data as value or empty string if the value is null.
     *
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes for the element
     * @return the output itself for fluent method calls
     */
    public StructuredOutput nullsafeProperty(String name, Object data, Attribute... attributes) {
        property(name, data != null ? data : "", attributes);
        return this;
    }

    /**
     * Provides a convenient way for {@link #nullsafeProperty(String, Object)} prepending a namespace.
     *
     * @param namespace the namespace
     * @param name      the name of the property
     * @param data      the value of the property
     * @return the output itself for fluent method calls
     */
    public StructuredOutput nullsafeProperty(@Nonnull String namespace, @Nonnull String name, @Nullable Object data) {
        return nullsafeProperty(namespace + ":" + name, data);
    }

    /**
     * Adds a property containing attributes to the current object.
     * <p>
     * In contrast to {@link #property(String, Object, Attribute...)}, this method will use a CDATA section for the data.
     *
     * @param name       the name of the property
     * @param data       the value of the property
     * @param attributes the attributes for the element
     * @return the output itself for fluent method calls
     */
    public MXMLStructuredOutput cdataProperty(String name, Object data, Attribute... attributes) {
        try {
            beginObject(name, attributes);
            transformerHandler2.startCDATA();
            text(data);
            transformerHandler2.endCDATA();
            endObject();
        } catch (SAXException exception) {
            throw handleOutputException(exception);
        }
        return this;
    }

    /**
     * Creates a text node for the current node.
     *
     * @param text the text to be added to the current node
     * @return the output itself for fluent method calls
     */
    public StructuredOutput text(Object text) {
        try {
            if (text != null) {
                String val = transformToStringRepresentation(text);
                transformerHandler2.characters(val.toCharArray(), 0, val.length());
            }
        } catch (SAXException exception) {
            throw handleOutputException(exception);
        }

        return this;
    }

    @Override
    public String toString() {
        return out.toString();
    }

    /**
     * Closes the underlying stream
     *
     * @throws IOException if an IO error occurs while closing the stream
     */
    public void close() throws IOException {
        out.close();
    }
}