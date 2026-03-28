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

@app.get("/substack/{username}")
def get_substack_posts(username: str):
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

        post = {
            "title": entry.get("title", "No Title"),
            "link": entry.get("link", "No Link"),
            "published": entry.get("published", "No Published Date"),
            "content": content_text
        }
        posts.append(post)

    return {"posts": posts}