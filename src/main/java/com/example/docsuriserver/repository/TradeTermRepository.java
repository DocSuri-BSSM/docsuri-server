package com.example.docsuriserver.repository;

import com.example.docsuriserver.domain.TradeTerm;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeTermRepository extends JpaRepository<TradeTerm, String> {

    List<TradeTerm> findByKoreanNameContaining(String keyword);
}
