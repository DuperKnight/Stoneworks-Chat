package com.duperknight.stoneworksChat.client;

import java.util.List;

public record Channel(
    String display,
    String color,
    List<String> aliases,
    String uiName
) {}


