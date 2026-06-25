package com.substacker.api.service;

import com.substacker.api.model.SubstackInfo;
import com.substacker.api.model.SubstackPost;
import com.substacker.api.model.SubstackPostsResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubstackService {

    private String getFeedUrl(String username) {
        return "https://" + username + ".substack.com/feed";
    }

    private Document fetchFeed(String username) {
        String url = getFeedUrl(username);
        try {
            // Fetch the feed as XML
            return Jsoup.connect(url)
                    .parser(Parser.xmlParser())
                    .timeout(10000)
                    .get();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Substack feed or connection error: " + e.getMessage());
        }
    }

    public SubstackInfo getSubstackInfo(String username) {
        Document doc = fetchFeed(username);
        Element channel = doc.selectFirst("rss > channel");
        if (channel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Substack feed structure");
        }

        String title = getElementText(channel, "title", "No Title");
        String subtitle = getElementText(channel, "description", "No Subtitle");
        String link = getElementText(channel, "link", "No Link");
        String description = getElementText(channel, "description", "No Description");

        return new SubstackInfo(title, subtitle, link, description);
    }

    public SubstackPostsResponse getSubstackPosts(String username, int limit, String search) {
        Document doc = fetchFeed(username);
        Element channel = doc.selectFirst("rss > channel");
        if (channel == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Substack feed structure");
        }

        Elements items = channel.select("item");
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No posts found");
        }

        List<SubstackPost> posts = new ArrayList<>();
        for (Element item : items) {
            String title = getElementText(item, "title", "No Title");
            String link = getElementText(item, "link", "No Link");
            String published = getElementText(item, "pubDate", "No Published Date");

            // Extract content: content:encoded or description
            String contentHtml = "";
            Element contentEncoded = item.selectFirst("content|encoded");
            if (contentEncoded != null) {
                contentHtml = contentEncoded.text();
            } else {
                Element desc = item.selectFirst("description");
                if (desc != null) {
                    contentHtml = desc.text();
                }
            }

            // Strip HTML to get plain text, similar to BeautifulSoup's get_text()
            String contentText = Jsoup.parse(contentHtml).text();

            // Apply search filter if present (case insensitive check in content or title)
            if (search != null && !search.isEmpty()) {
                String searchLower = search.toLowerCase();
                boolean matchesContent = contentText.toLowerCase().contains(searchLower);
                boolean matchesTitle = title.toLowerCase().contains(searchLower);
                if (!matchesContent && !matchesTitle) {
                    continue;
                }
            }

            posts.add(new SubstackPost(title, link, published, contentText));

            if (posts.size() >= limit) {
                break;
            }
        }

        return new SubstackPostsResponse(posts);
    }

    private String getElementText(Element parent, String query, String defaultValue) {
        Element el = parent.selectFirst(query);
        if (el != null) {
            String text = el.text().trim();
            return text.isEmpty() ? defaultValue : text;
        }
        return defaultValue;
    }
}
