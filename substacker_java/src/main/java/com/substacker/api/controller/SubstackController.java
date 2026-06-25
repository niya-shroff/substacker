package com.substacker.api.controller;

import com.substacker.api.model.SubstackInfo;
import com.substacker.api.model.SubstackPostsResponse;
import com.substacker.api.service.SubstackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.OPTIONS})
@RequestMapping("/")
public class SubstackController {

    private final SubstackService substackService;

    @Autowired
    public SubstackController(SubstackService substackService) {
        this.substackService = substackService;
    }

    @GetMapping
    public Map<String, String> root() {
        return Map.of("message", "Substacker is running :)");
    }

    @GetMapping("/substack/{username}/info")
    public SubstackInfo getSubstackInfo(@PathVariable String username) {
        return substackService.getSubstackInfo(username);
    }

    @GetMapping("/substack/{username}")
    public SubstackPostsResponse getSubstackPosts(
            @PathVariable String username,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search
    ) {
        return substackService.getSubstackPosts(username, limit, search);
    }
}
