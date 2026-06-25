package com.substacker.api.model;

public record SubstackPost(
    String title,
    String link,
    String published,
    String content
) {}
