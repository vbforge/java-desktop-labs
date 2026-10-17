package com.vbforge.homeshelf.io;

import com.vbforge.homeshelf.MediaItem;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XmlProcess implements DataProcess {
    private final File file;

    public XmlProcess(File file) { this.file = file; }

    @Override
    public void save(List<MediaItem> items) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("homeShelf");
        doc.appendChild(root);

        for (MediaItem item : items) {
            Element el = doc.createElement("item");
            el.setAttribute("title", item.title());
            el.setAttribute("creator", item.creator());
            el.setAttribute("year", String.valueOf(item.year()));
            el.setAttribute("type", item.type());
            el.setAttribute("rating", String.valueOf(item.rating()));
            root.appendChild(el);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(doc), new StreamResult(file));
    }

    @Override
    public List<MediaItem> load() throws Exception {
        List<MediaItem> items = new ArrayList<>();
        if (!file.exists()) return items;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        
        NodeList nodes = doc.getElementsByTagName("item");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            items.add(new MediaItem(
                    el.getAttribute("title"),
                    el.getAttribute("creator"),
                    Integer.parseInt(el.getAttribute("year")),
                    el.getAttribute("type"),
                    Integer.parseInt(el.getAttribute("rating"))
            ));
        }
        return items;
    }
}