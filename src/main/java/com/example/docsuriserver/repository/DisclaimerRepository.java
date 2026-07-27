package com.example.docsuriserver.repository;

import com.example.docsuriserver.domain.Disclaimer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisclaimerRepository extends JpaRepository<Disclaimer, Long> {

    List<Disclaimer> findByIsActiveTrue();

    List<Disclaimer> findByDisplayPositionAndIsActiveTrue(String displayPosition);
}
