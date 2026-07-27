package com.example.docsuriserver.guide.dto;

import com.example.docsuriserver.guide.entity.Disclaimer;

import java.util.List;

public record DisclaimerListSearchResponse(
        List<DisclaimerItem> disclaimers
) {

    public record DisclaimerItem(
            String title,
            String content,
            String displayPosition
    ) {
    }

    public static DisclaimerListSearchResponse from(List<Disclaimer> disclaimers) {
        List<DisclaimerItem> items = disclaimers.stream()
                .map(d -> new DisclaimerItem(d.getTitle(), d.getContent(), d.getDisplayPosition().name()))
                .toList();
        return new DisclaimerListSearchResponse(items);
    }
}
