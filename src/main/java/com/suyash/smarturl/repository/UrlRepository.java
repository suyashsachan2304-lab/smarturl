package com.suyash.smarturl.repository;

import com.suyash.smarturl.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findByOriginalUrl(String originalUrl);

    boolean existsByShortCode(String shortCode);

    Optional<UrlMapping> findByShortCodeAndActiveTrue(String shortCode);

    long countByActiveTrue();

    void deleteByExpiresAtBefore(LocalDateTime now);
}