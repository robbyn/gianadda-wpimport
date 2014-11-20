package org.tastefuljava.gianadda.wpimport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class ContentProcessor {
    private final String baseUrl;

    public ContentProcessor(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String process(String input) throws IOException {
        String body = input;
        body = cleanup(body, "[caption", "[/caption]");
        body = cleanup(body, "<iframe", "</iframe>");
        body = splitParas(body);
        return body;
    }

    // remove averyzhing between tagStart and tagEnf
    private String cleanup(String content, String tagStart,
            String tagEnd) {
        StringBuilder buf = new StringBuilder();
        int start = 0;
        while (true) {
            int ix = content.indexOf(tagStart, start);
            if (ix < 0) {
                break;
            }
            buf.append(content, start, ix);
            ix = content.indexOf(tagEnd, ix);
            if (ix < 0) {
                break;
            }
            start = ix + tagEnd.length();
        }
        if (start < content.length()) {
            buf.append(content.substring(start));
        }
        return buf.toString();
    }

    // convert paragraphs separated by a blank line into <p></p> paragraphes
    private String splitParas(String body) throws IOException {
        StringBuilder buf = new StringBuilder();
        try (Reader reader = new StringReader(body);
                BufferedReader in = new BufferedReader(reader)) {
            int paraStart = 0;
            for (String s = in.readLine(); s != null; s = in.readLine()) {
                if (s.trim().length() == 0) {
                    if (buf.length() > paraStart) {
                        buf.insert(paraStart, "<p>");
                        buf.append("</p>\n");
                        paraStart = buf.length();
                    }
                } else {
                    if (buf.length() > paraStart) {
                        buf.append('\n');
                    }
                    buf.append(s);
                }
            }
            if (buf.length() > paraStart) {
                buf.insert(paraStart, "<p>");
                buf.append("</p>");
            }
        }
        return buf.toString();
    }
}
