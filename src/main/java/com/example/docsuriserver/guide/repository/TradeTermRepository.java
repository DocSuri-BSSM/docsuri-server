package com.example.docsuriserver.guide.repository;

import com.example.docsuriserver.guide.entity.TradeTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TradeTermRepository extends JpaRepository<TradeTerm, String> {

    List<TradeTerm> findAllByOrderByTermAsc();

    @Query("""
            SELECT t FROM TradeTerm t
            WHERE LOWER(t.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(t.koreanName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(t.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY t.term ASC
            """)
    List<TradeTerm> searchByKeyword(@Param("keyword") String keyword);
}
