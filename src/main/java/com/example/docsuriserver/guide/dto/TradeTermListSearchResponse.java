package com.example.docsuriserver.guide.dto;

import com.example.docsuriserver.guide.entity.TradeTerm;

import java.util.List;

public record TradeTermListSearchResponse(
        List<TermItem> terms
) {

    public record TermItem(
            String term,
            String fullName,
            String koreanName,
            String description
    ) {
    }

    public static TradeTermListSearchResponse from(List<TradeTerm> terms) {
        List<TermItem> items = terms.stream()
                .map(t -> new TermItem(t.getTerm(), t.getFullName(), t.getKoreanName(), t.getDescription()))
                .toList();
        return new TradeTermListSearchResponse(items);
    }
}
