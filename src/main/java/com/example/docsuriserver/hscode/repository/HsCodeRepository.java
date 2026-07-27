package com.example.docsuriserver.hscode.repository;

import com.example.docsuriserver.hscode.entity.HsCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HsCodeRepository extends JpaRepository<HsCode, String> {

    /**
     * keyword(품명+설명)는 대개 korean_name/english_name보다 훨씬 긴 문장이라, 전체 문자열끼리
     * 비교하는 similarity()/word_similarity()는 신뢰할 수 없다 (word_similarity는 짧은 이름을
     * 긴 무관한 텍스트와 비교할 때도 우연히 높은 점수를 주는 경우가 많다 — 예: "Toys"가 완전
     * 무관한 문장과 0.4 유사도로 매칭됨). 그래서 keyword를 단어 단위로 쪼갠 뒤, 각 단어와
     * korean_name/english_name을 개별적으로 비교한다.
     */
    @Query(value = """
            SELECT * FROM hs_codes h
            WHERE h.korean_name ILIKE CONCAT('%', :keyword, '%')
               OR (h.english_name IS NOT NULL AND h.english_name ILIKE CONCAT('%', :keyword, '%'))
               OR (h.description IS NOT NULL AND h.description ILIKE CONCAT('%', :keyword, '%'))
               OR EXISTS (
                   SELECT 1 FROM unnest(regexp_split_to_array(trim(:keyword), '[\\s,]+')) AS kw(word)
                   WHERE length(kw.word) >= 2
                     AND (h.korean_name ILIKE CONCAT('%', kw.word, '%')
                          OR (h.english_name IS NOT NULL AND h.english_name ILIKE CONCAT('%', kw.word, '%'))
                          OR similarity(h.korean_name, kw.word) > 0.3
                          OR (h.english_name IS NOT NULL AND similarity(h.english_name, kw.word) > 0.3))
               )
            ORDER BY GREATEST(
                (SELECT MAX(similarity(h.korean_name, kw.word))
                 FROM unnest(regexp_split_to_array(trim(:keyword), '[\\s,]+')) AS kw(word)),
                (SELECT MAX(similarity(COALESCE(h.english_name, ''), kw.word))
                 FROM unnest(regexp_split_to_array(trim(:keyword), '[\\s,]+')) AS kw(word))
            ) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<HsCode> searchCandidates(@Param("keyword") String keyword, @Param("limit") int limit);
}
