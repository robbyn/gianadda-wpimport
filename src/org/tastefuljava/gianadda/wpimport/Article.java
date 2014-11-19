package org.tastefuljava.gianadda.wpimport;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Article {
    private String name;
    private String pubDate;
    private String pubDateGmt;
    private String title;
    private final Set<String> tags = new HashSet<>();
    private String summary;
    private String content;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getPubDateGmt() {
        return pubDateGmt;
    }

    public void setPubDateGmt(String pubDateGmt) {
        this.pubDateGmt = pubDateGmt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Set<String> getTags() {
        return new HashSet<>(tags);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }
}
