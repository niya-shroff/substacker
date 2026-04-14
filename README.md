# Substacker API

A simple API built with **FastAPI** to fetch posts from any public Substack newsletter using their RSS feed. This allows you to retrieve Substack posts in JSON format, including titles, links, published dates, and full content.

---

## Features

- Fetch posts from any Substack newsletter by username
- Returns post metadata: title, link, published date, and content
- HTML content is converted to plain text
- CORS enabled for all origins

---

## Usage

```bash
Fetch Substack posts:
GET substacker-umber.vercel.app/substack/{username}
```
Response:
```bash
{
  "posts": [
    {
      "title": "Latest Tech Trends",
      "link": "https://techcrunch.substack.com/p/latest-tech-trends",
      "published": "Tue, 28 Mar 2023 12:00:00 GMT",
      "content": "Plain text content of the post..."
    },
    {
      "title": "Another Post",
      "link": "https://techcrunch.substack.com/p/another-post",
      "published": "Mon, 27 Mar 2023 10:00:00 GMT",
      "content": "Plain text content of another post..."
    }
  ]
}
```
