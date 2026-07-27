package com.example.docsuriserver.guide.repository;

import com.example.docsuriserver.common.DisclaimerPosition;
import com.example.docsuriserver.guide.entity.Disclaimer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisclaimerRepository extends JpaRepository<Disclaimer, Long> {

    List<Disclaimer> findAllByActiveTrueOrderByDisclaimerIdAsc();

    Optional<Disclaimer> findFirstByDisplayPositionAndActiveTrue(DisclaimerPosition displayPosition);
}
