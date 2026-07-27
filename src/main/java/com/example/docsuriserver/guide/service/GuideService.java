package com.example.docsuriserver.guide.service;

import com.example.docsuriserver.guide.dto.TradeTermListSearchResponse;
import com.example.docsuriserver.guide.repository.TradeTermRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuideService {

    private final TradeTermRepository tradeTermRepository;

    public GuideService(TradeTermRepository tradeTermRepository) {
        this.tradeTermRepository = tradeTermRepository;
    }

    @Transactional(readOnly = true)
    public TradeTermListSearchResponse searchTradeTerms(String keyword) {
        var terms = (keyword == null || keyword.isBlank())
                ? tradeTermRepository.findAllByOrderByTermAsc()
                : tradeTermRepository.searchByKeyword(keyword);
        return TradeTermListSearchResponse.from(terms);
    }
}
