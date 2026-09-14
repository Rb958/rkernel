/*
 * Copyright (c) 2021-2026 Richie Akawa
 * Licensed under the Apache License, Version 2.0. See LICENSE.
 */

package rkernel.utils.file;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import rkernel.exception.FileManagerException;
import rkernel.signal.SignalRegistry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.XMLConstants;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * File access for a working folder: the JARs to load, and the signal registry
 * persisted as XML.
 *
 * <p>The registry is written with the JDK's DOM API rather than JAXB. Version 1.0
 * depended on {@code javax.xml.bind} 2.3 and {@code jakarta.xml.bind} 3.0 at the
 * same time, two incompatible namespaces; the JDK can read and write this XML
 * with no dependency at all. The produced format is identical to 1.0.1, so
 * existing registries remain readable.</p>
 *
 * <p>One instance per folder. Version 1.0 kept a single static instance bound to
 * the first folder requested and silently ignored every folder after it.</p>
 */
public class FileManager {

    private static final Map<Path, FileManager> INSTANCES = new ConcurrentHashMap<>();

    protected final File targetFolder;

    private FileManager(File folder) throws FileManagerException {
        if (folder.exists() && !folder.isDirectory()) {
            throw new FileManagerException(700, "The parameter should be a folder");
        }
        this.targetFolder = folder;
    }

    /** Returns the instance bound to this folder, creating it if needed. */
    public static FileManager getInstance(File folder) throws FileManagerException {
        Path key = folder.toPath().toAbsolutePath().normalize();
        FileManager existing = INSTANCES.get(key);
        if (existing != null) {
            return existing;
        }
        FileManager created = new FileManager(folder);
        FileManager concurrent = INSTANCES.putIfAbsent(key, created);
        return concurrent != null ? concurrent : created;
    }

    /** The {@code .jar} files of the folder, creating the folder if it does not exist. */
    public File[] getFiles() {
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        File[] jars = targetFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        return jars == null ? new File[0] : jars;
    }

    public boolean pathExist(Path path) {
        return Files.exists(path);
    }

    /**
     * Reads a registry from its XML file. A missing or empty file yields an
     * empty registry rather than {@code null}.
     */
    public SignalRegistry getFileContent(Path filePath) throws IOException {
        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            return new SignalRegistry();
        }
        try {
            Document doc = documentBuilder().parse(filePath.toFile());
            Element root = doc.getDocumentElement();
            SignalRegistry registry = new SignalRegistry(textOf(root, "Kernel"));
            NodeList entries = root.getElementsByTagName("SignalTypeEntries");
            for (int i = 0; i < entries.getLength(); i++) {
                Element e = (Element) entries.item(i);
                registry.addEntry(attributeOrNull(e, "type"),
                                  attributeOrNull(e, "componentName"),
                                  attributeOrNull(e, "kernelName"));
            }
            return registry;
        } catch (ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IOException("Unreadable registry: " + filePath, e);
        }
    }

    /** Writes the registry as XML, in the 1.0.1 format. */
    public void writeFileContent(SignalRegistry registry, Path filePath) throws IOException {
        try {
            Document doc = documentBuilder().newDocument();
            Element root = doc.createElement("signalRegistry");
            doc.appendChild(root);

            if (registry.getKernelName() != null) {
                Element kernel = doc.createElement("Kernel");
                kernel.setTextContent(registry.getKernelName());
                root.appendChild(kernel);
            }
            for (SignalRegistry.SignalTypeEntry entry : registry.getSignalTypeEntries()) {
                Element e = doc.createElement("SignalTypeEntries");
                e.setAttribute("type", entry.getType());
                if (entry.getComponentName() != null) e.setAttribute("componentName", entry.getComponentName());
                if (entry.getKernelName() != null) e.setAttribute("kernelName", entry.getKernelName());
                root.appendChild(e);
            }

            Path parent = filePath.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);

            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.transform(new DOMSource(doc), new StreamResult(filePath.toFile()));
        } catch (ParserConfigurationException | TransformerException e) {
            throw new IOException("Cannot write registry: " + filePath, e);
        }
    }

    private static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        // The registry is a local file we wrote ourselves; external entities are
        // refused anyway, on principle.
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return f.newDocumentBuilder();
    }

    private static String textOf(Element parent, String tag) {
        NodeList n = parent.getElementsByTagName(tag);
        return n.getLength() == 0 ? null : n.item(0).getTextContent();
    }

    private static String attributeOrNull(Element e, String name) {
        return e.hasAttribute(name) ? e.getAttribute(name) : null;
    }
}
