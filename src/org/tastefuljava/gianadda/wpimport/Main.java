package org.tastefuljava.gianadda.wpimport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class Main {
    private static final Logger LOG
            = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            File inputFile = new File(args[0]);
            File outputDir = new File(args[1]);
            processFile(inputFile, outputDir);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private static void processFile(File file, File outputDir)
            throws IOException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(false);
            SAXParser parser = factory.newSAXParser();
            ParserHandler handler = new ParserHandler(outputDir);
            parser.parse(file, handler);
        } catch (SAXException | ParserConfigurationException e) {
            LOG.log(Level.SEVERE, "Error reading project", e);
            throw new IOException(e.getMessage());
        }
    }

    private static class ParserHandler extends DefaultHandler {
        private static final String DTD_SYSTEM_ID = "folder-meta.dtd";
        private static final String DTD_PUBLIC_ID
                = "-//tastefuljava.org//Gianadda Folder Metadata File 1.0//EN";

        private final DateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        private final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        private final StringBuilder buf = new StringBuilder();
        private final File outputDir;
        private Article article;
        private boolean keepArticle = false;
        private ContentProcessor processor;

        public ParserHandler(File outputDir) {
            this.outputDir = outputDir;
            TimeZone gmt = TimeZone.getTimeZone("GMT");
            dateFormat.setTimeZone(gmt);
            formatter.setTimeZone(gmt);
        }

        private void writeArticle(Article article) {
            try {
                Date localDate = dateFormat.parse(article.getPubDate());
                Date gmtDate = dateFormat.parse(article.getPubDateGmt());
                TimeZone tz = timeZone(localDate, gmtDate);
                String fileName = formatter.format(localDate);
                File dir = new File(outputDir, fileName);
                if (dir.isDirectory()) {
                    char c = 'a';
                    do {
                        dir = new File(outputDir, fileName + c);
                        ++c;
                    } while (dir.isDirectory());
                }
                if (!dir.mkdirs()) {
                    throw new IOException("Could not create directory " + dir);
                }
                File file = new File(dir, "folder-meta.xml");
                try (OutputStream stream = new FileOutputStream(file);
                        Writer writer = new OutputStreamWriter(stream, "UTF-8");
                        PrintWriter out = new PrintWriter(writer);
                        XMLWriter xml = new XMLWriter(out)) {
                    out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    out.println("<!DOCTYPE folder-meta PUBLIC "
                            + "\"-//tastefuljava.org//Gianadda Folder Metadata"
                            + " File 1.0//EN\" \"folder-meta.dtd\">");
                    xml.startTag("folder-meta");
                    xml.attribute("version", "1");
                    xml.startTag("title");
                    xml.data(article.getTitle());
                    xml.endTag();
                    xml.startTag("pubDate");
                    xml.data(Util.formatXsdDateTime(localDate, tz));
                    xml.endTag();
                    for (String tag: article.getTags()) {
                        xml.startTag("tag");
                        xml.data(tag);
                        xml.endTag();
                    }
                    if (article.getSummary() != null) {
                        xml.startTag("summary");
                        xml.attribute("type", "text/plain");
                        xml.cdata(article.getSummary());
                        xml.endTag();
                    }
                    xml.startTag("content");
                    xml.attribute("type", "text/html");
                    String body = article.getContent();
                    xml.cdata(processor.process(body));
                    xml.endTag();
                    xml.endTag();
                }
            } catch (IOException | ParseException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId)
                throws IOException, SAXException {
            if (DTD_PUBLIC_ID.equals(publicId)
                    || DTD_SYSTEM_ID.equals(systemId)) {
                InputSource source = new InputSource(
                        getClass().getResourceAsStream("folder-meta.dtd"));
                source.setPublicId(publicId);
                source.setSystemId(systemId);
                return source;
            }
            return super.resolveEntity(publicId, systemId);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new SAXException(e.getMessage());
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            throw new SAXException(e.getMessage());
        }

        @Override
        public void warning(SAXParseException e) throws SAXException {
            LOG.log(Level.WARNING, e.getMessage(), e);
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                Attributes attrs) throws SAXException {
            buf.setLength(0);
            switch (qName) {
                case "item":
                    article = new Article();
                    keepArticle = false;
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException {
            switch (qName) {
                case "item":
                    if (keepArticle && article != null) {
                        writeArticle(article);
                    }
                    article = null;
                    break;
                case "link": {
                        String link = buf.toString().trim();
                        if (article == null) {
                            if (!link.endsWith("/")) {
                                link += "/";
                            }
                            processor = new ContentProcessor(link);
                        }
                    }
                    break;
                case "wp:post_type":
                    if (article != null) {
                        keepArticle = buf.toString().trim().equals("post");
                    }
                    break;
                case "wp:post_name":
                    if (article != null) {
                        article.setName(buf.toString().trim());
                    }
                    break;
                case "title":
                    if (article != null) {
                        article.setTitle(buf.toString().trim());
                    }
                    break;
                case "wp:post_date":
                    if (article != null) {
                        article.setPubDate(buf.toString().trim());
                    }
                    break;
                case "wp:post_date_gmt":
                    if (article != null) {
                        article.setPubDateGmt(buf.toString().trim());
                    }
                    break;
                case "category":
                    if (article != null) {
                        article.addTag(buf.toString().trim());
                    }
                    break;
                case "content:encoded":
                    if (article != null && buf.length() > 0) {
                        article.setContent(buf.toString());
                    }
                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            buf.append(ch, start, length);
        }

        private static TimeZone timeZone(Date localDate, Date gmtDate) {
            long diff = localDate.getTime()-gmtDate.getTime();
            int mins = (int)(diff/1000/60);
            boolean neg = mins < 0;
            if (neg) {
                mins = - mins;
            }
            int hours = mins/60;
            mins %= 60;
            StringBuilder buf = new StringBuilder();
            buf.append("GMT");
            buf.append(neg ? '-' : '+');
            buf.append((char)('0' + (hours/10)));
            buf.append((char)('0' + (hours%10)));
            buf.append(':');
            buf.append((char)('0' + (mins/10)));
            buf.append((char)('0' + (mins%10)));
            return TimeZone.getTimeZone(buf.toString());
        }
    }
}
