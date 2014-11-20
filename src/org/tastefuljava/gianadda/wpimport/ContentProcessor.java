package org.tastefuljava.gianadda.wpimport;

public class ContentProcessor {
    private final String baseUrl;

    public ContentProcessor(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String process(String input) {
        String body = input;
        body = cleanup(body, "[caption", "[/caption]");
        body = cleanup(body, "<iframe", "</iframe>");
        return body;
    }

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
}
