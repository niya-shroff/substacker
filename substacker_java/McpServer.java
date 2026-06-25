import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McpServer {

    private static final String BASE_URL = System.getenv().getOrDefault("API_BASE_URL", "http://localhost:8080");
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        System.err.println("Substacker Java MCP Server started. Base URL: " + BASE_URL);
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processMessage(line.trim());
            }
        } catch (Exception e) {
            System.err.println("Error in MCP Server loop: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static void processMessage(String message) {
        if (message.isEmpty()) return;
        
        try {
            String id = extractId(message);
            String method = extractString(message, "method");
            
            if (method == null) {
                // If it's a response or notification we don't handle
                return;
            }
            
            switch (method) {
                case "initialize":
                    handleInitialize(id);
                    break;
                case "notifications/initialized":
                    // No response required for notifications
                    break;
                case "tools/list":
                    handleToolsList(id);
                    break;
                case "tools/call":
                    handleToolsCall(message, id);
                    break;
                default:
                    sendError(id, -32601, "Method not found: " + method);
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
        }
    }

    private static void handleInitialize(String id) {
        String response = "{"
            + "\"jsonrpc\":\"2.0\","
            + "\"id\":" + id + ","
            + "\"result\":{"
            + "  \"protocolVersion\":\"2024-11-05\","
            + "  \"capabilities\":{"
            + "    \"tools\":{}"
            + "  },"
            + "  \"serverInfo\":{"
            + "    \"name\":\"SubstackerJava\","
            + "    \"version\":\"1.0\""
            + "  }"
            + "}"
            + "}";
        sendResponse(response);
    }

    private static void handleToolsList(String id) {
        String response = "{"
            + "\"jsonrpc\":\"2.0\","
            + "\"id\":" + id + ","
            + "\"result\":{"
            + "  \"tools\":["
            + "    {"
            + "      \"name\":\"get_substack_info\","
            + "      \"description\":\"Get metadata information about a given Substack publication via the Java API.\","
            + "      \"inputSchema\":{"
            + "        \"type\":\"object\","
            + "        \"properties\":{"
            + "          \"username\":{"
            + "            \"type\":\"string\","
            + "            \"description\":\"The Substack username (the part before .substack.com).\""
            + "          }"
            + "        },"
            + "        \"required\":[\"username\"]"
            + "      }"
            + "    },"
            + "    {"
            + "      \"name\":\"get_substack_posts\","
            + "      \"description\":\"Get posts from a given Substack publication via the Java API.\","
            + "      \"inputSchema\":{"
            + "        \"type\":\"object\","
            + "        \"properties\":{"
            + "          \"username\":{"
            + "            \"type\":\"string\","
            + "            \"description\":\"The Substack username (the part before .substack.com).\""
            + "          },"
            + "          \"limit\":{"
            + "            \"type\":\"integer\","
            + "            \"description\":\"Maximum number of posts to return (default 10).\""
            + "          },"
            + "          \"search\":{"
            + "            \"type\":\"string\","
            + "            \"description\":\"Optional string to filter posts by content or title.\""
            + "          }"
            + "        },"
            + "        \"required\":[\"username\"]"
            + "      }"
            + "    }"
            + "  ]"
            + "}"
            + "}";
        sendResponse(response);
    }

    private static void handleToolsCall(String message, String id) {
        String toolName = extractString(message, "name");
        if (toolName == null) {
            sendError(id, -32602, "Missing tool name");
            return;
        }

        // Extract arguments
        String paramsBlock = extractNestedObject(message, "arguments");
        if (paramsBlock == null) {
            paramsBlock = message; // Fallback
        }

        String username = extractString(paramsBlock, "username");
        if (username == null) {
            sendError(id, -32602, "Missing required argument: username");
            return;
        }

        if ("get_substack_info".equals(toolName)) {
            callGetSubstackInfo(username, id);
        } else if ("get_substack_posts".equals(toolName)) {
            Integer limit = extractInt(paramsBlock, "limit");
            if (limit == null) limit = 10;
            String search = extractString(paramsBlock, "search");
            callGetSubstackPosts(username, limit, search, id);
        } else {
            sendError(id, -32601, "Unknown tool: " + toolName);
        }
    }

    private static void callGetSubstackInfo(String username, String id) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/substack/" + URLEncoder.encode(username, StandardCharsets.UTF_8) + "/info"))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                sendToolResponse(id, "HTTP Error " + response.statusCode() + ": " + response.body(), true);
                return;
            }

            String body = response.body();
            String title = extractString(body, "title");
            String subtitle = extractString(body, "subtitle");
            String link = extractString(body, "link");
            String description = extractString(body, "description");

            String textResult = String.format("Title: %s\nSubtitle: %s\nLink: %s\nDescription: %s",
                    title, subtitle, link, description);
            
            sendToolResponse(id, textResult, false);
        } catch (Exception e) {
            sendToolResponse(id, "Error fetching Substack info: " + e.getMessage(), true);
        }
    }

    private static void callGetSubstackPosts(String username, int limit, String search, String id) {
        try {
            String url = BASE_URL + "/substack/" + URLEncoder.encode(username, StandardCharsets.UTF_8) + "?limit=" + limit;
            if (search != null && !search.isEmpty()) {
                url += "&search=" + URLEncoder.encode(search, StandardCharsets.UTF_8);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                sendToolResponse(id, "HTTP Error " + response.statusCode() + ": " + response.body(), true);
                return;
            }

            List<String[]> posts = parsePosts(response.body());
            if (posts.isEmpty()) {
                sendToolResponse(id, "No posts matching the given criteria.", false);
                return;
            }

            List<String> results = new ArrayList<>();
            for (String[] post : posts) {
                String title = post[0];
                String link = post[1];
                String published = post[2];
                String content = post[3];
                String snippet = content.length() > 200 ? content.substring(0, 200) + "..." : content;
                
                results.add(String.format("Title: %s\nPublished: %s\nLink: %s\nContent Snippet: %s",
                        title, published, link, snippet));
            }

            sendToolResponse(id, String.join("\n\n---\n\n", results), false);
        } catch (Exception e) {
            sendToolResponse(id, "Error fetching Substack posts: " + e.getMessage(), true);
        }
    }

    private static void sendToolResponse(String id, String text, boolean isError) {
        String escapedText = escapeJsonString(text);
        String response = "{"
            + "\"jsonrpc\":\"2.0\","
            + "\"id\":" + id + ","
            + "\"result\":{"
            + "  \"content\":["
            + "    {"
            + "      \"type\":\"text\","
            + "      \"text\":\"" + escapedText + "\""
            + "    }"
            + "  ],"
            + "  \"isError\":" + isError
            + "}"
            + "}";
        sendResponse(response);
    }

    private static void sendError(String id, int code, String message) {
        String response = "{"
            + "\"jsonrpc\":\"2.0\","
            + "\"id\":" + id + ","
            + "\"error\":{"
            + "  \"code\":" + code + ","
            + "  \"message\":\"" + escapeJsonString(message) + "\""
            + "}"
            + "}";
        sendResponse(response);
    }

    private static void sendResponse(String json) {
        System.out.println(json);
        System.out.flush();
    }

    // --- JSON Utility Helpers ---

    private static String extractId(String json) {
        Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+|\"[^\"]+\")");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "null";
    }

    private static String extractString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"(.*?)(?<!\\\\)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return null;
    }

    private static Integer extractInt(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    private static String extractNestedObject(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx == -1) return null;
        int start = json.indexOf("{", idx);
        if (start == -1) return null;
        int braceCount = 1;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') braceCount++;
            else if (c == '}') braceCount--;
            if (braceCount == 0) {
                return json.substring(start, i + 1);
            }
        }
        return null;
    }

    private static String unescapeJson(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                switch (next) {
                    case '"': sb.append('"'); i += 2; break;
                    case '\\': sb.append('\\'); i += 2; break;
                    case 'n': sb.append('\n'); i += 2; break;
                    case 'r': sb.append('\r'); i += 2; break;
                    case 't': sb.append('\t'); i += 2; break;
                    case 'b': sb.append('\b'); i += 2; break;
                    case 'f': sb.append('\f'); i += 2; break;
                    case 'u':
                        if (i + 5 < input.length()) {
                            try {
                                int code = Integer.parseInt(input.substring(i + 2, i + 6), 16);
                                sb.append((char) code);
                                i += 6;
                            } catch (NumberFormatException e) {
                                sb.append("\\u");
                                i += 2;
                            }
                        } else {
                            sb.append("\\u");
                            i += 2;
                        }
                        break;
                    default:
                        sb.append(c);
                        i++;
                        break;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String escapeJsonString(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    private static List<String[]> parsePosts(String json) {
        List<String[]> posts = new ArrayList<>();
        int postsIdx = json.indexOf("\"posts\"");
        if (postsIdx == -1) return posts;
        
        Pattern pattern = Pattern.compile("\\{\\s*\"title\"\\s*:\\s*\"(.*?)(?<!\\\\)\"\\s*,\\s*\"link\"\\s*:\\s*\"(.*?)(?<!\\\\)\"\\s*,\\s*\"published\"\\s*:\\s*\"(.*?)(?<!\\\\)\"\\s*,\\s*\"content\"\\s*:\\s*\"(.*?)(?<!\\\\)\"\\s*\\}");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String title = unescapeJson(matcher.group(1));
            String link = unescapeJson(matcher.group(2));
            String published = unescapeJson(matcher.group(3));
            String content = unescapeJson(matcher.group(4));
            posts.add(new String[]{title, link, published, content});
        }
        return posts;
    }
}
