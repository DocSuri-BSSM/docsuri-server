package com.example.docsuriserver.hscode.repository;

import com.example.docsuriserver.hscode.entity.HsCodeRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HsCodeRecommendationRepository extends JpaRepository<HsCodeRecommendation, UUID> {
}
