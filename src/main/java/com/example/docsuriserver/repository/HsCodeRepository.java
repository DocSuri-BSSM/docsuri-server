package com.example.docsuriserver.repository;

import com.example.docsuriserver.domain.HsCode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HsCodeRepository extends JpaRepository<HsCode, String> {

    List<HsCode> findByKoreanNameContaining(String keyword);

    List<HsCode> findByEnglishNameContainingIgnoreCase(String keyword);
}
