package com.substacker.api.model;

import java.util.List;

public record SubstackPostsResponse(
    List<SubstackPost> posts
) {}
