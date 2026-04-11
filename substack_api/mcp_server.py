from mcp.server.fastmcp import FastMCP
import requests
import os

mcp = FastMCP("Substacker")

# Default base URL; allows overriding via environment variable
BASE_URL = os.getenv("API_BASE_URL", "https://substacker-umber.vercel.app")

@mcp.tool()
def get_substack_info(username: str) -> str:
    """Get metadata information about a given Substack publication via API.
    
    Args:
        username: The Substack username (the part before .substack.com).
    """
    try:
        response = requests.get(f"{BASE_URL}/substack/{username}/info")
        response.raise_for_status()
        data = response.json()
        
        return f"Title: {data.get('title')}\nSubtitle: {data.get('subtitle')}\nLink: {data.get('link')}\nDescription: {data.get('description')}"
    except requests.exceptions.HTTPError as e:
        status_code = e.response.status_code
        detail = e.response.json().get("detail", "Unknown error")
        return f"HTTP Error {status_code}: {detail}"
    except Exception as e:
        return f"Error: {e}"

@mcp.tool()
def get_substack_posts(username: str, limit: int = 10, search: str = None) -> str:
    """Get posts from a given Substack publication via API.
    
    Args:
        username: The Substack username (the part before .substack.com).
        limit: Maximum number of posts to return (default 10).
        search: Optional string to filter posts by content or title.
    """
    try:
        params = {"limit": limit}
        if search:
            params["search"] = search
            
        response = requests.get(f"{BASE_URL}/substack/{username}", params=params)
        response.raise_for_status()
        data = response.json()
        posts = data.get("posts", [])
        
        if not posts:
            return "No posts matching the given criteria."
            
        result = []
        for p in posts:
            snippet = p.get('content', '')[:200]
            result.append(f"Title: {p.get('title')}\nPublished: {p.get('published')}\nLink: {p.get('link')}\nContent Snippet: {snippet}...")
            
        return "\n\n---\n\n".join(result)
    except requests.exceptions.HTTPError as e:
        status_code = e.response.status_code
        try:
            detail = e.response.json().get("detail", "Unknown error")
        except ValueError:
            detail = "Unknown error"
        return f"HTTP Error {status_code}: {detail}"
    except Exception as e:
        return f"Error: {e}"

if __name__ == "__main__":
    mcp.run()
