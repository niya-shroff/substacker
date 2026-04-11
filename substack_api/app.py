import feedparser
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from bs4 import BeautifulSoup

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {"message": "Substacker is running :)"}

@app.get("/substack/{username}/info")
def get_substack_info(username: str):
    feed_url = f"https://{username}.substack.com/feed"
    feed = feedparser.parse(feed_url)

    if feed.bozo:
        raise HTTPException(status_code=400, detail="Invalid Substack feed")

    return {
        "title": feed.feed.get("title", "No Title"),
        "subtitle": feed.feed.get("subtitle", "No Subtitle"),
        "link": feed.feed.get("link", "No Link"),
        "description": feed.feed.get("description", "No Description")
    }

@app.get("/substack/{username}")
def get_substack_posts(username: str, limit: int = 10, search: str = None):
    feed_url = f"https://{username}.substack.com/feed"
    
    feed = feedparser.parse(feed_url)

    if feed.bozo:
        raise HTTPException(status_code=400, detail="Invalid Substack feed")

    if not feed.entries:
        raise HTTPException(status_code=404, detail="No posts found")
        
    posts = []
    for entry in feed.entries:
        content = entry.get("content", [{}])[0].get("value", "")
        content_text = BeautifulSoup(content, "html.parser").get_text()

        if search and search.lower() not in content_text.lower() and search.lower() not in entry.get("title", "").lower():
            continue

        post = {
            "title": entry.get("title", "No Title"),
            "link": entry.get("link", "No Link"),
            "published": entry.get("published", "No Published Date"),
            "content": content_text
        }
        posts.append(post)

        if len(posts) >= limit:
            break

    return {"posts": posts}