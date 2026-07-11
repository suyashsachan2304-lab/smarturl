package com.suyash.smarturl.repository;

import com.suyash.smarturl.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findByOriginalUrl(String originalUrl);

    Optional<UrlMapping> findByOriginalUrlAndActiveTrue(String originalUrl);

    boolean existsByShortCode(String shortCode);

    boolean existsByShortCodeAndActiveTrue(String shortCode);

    Optional<UrlMapping> findByShortCodeAndActiveTrue(String shortCode);

    long countByActiveTrue();

    List<UrlMapping> findByActiveTrueAndExpiresAtBefore(LocalDateTime now);

    boolean existsByShortCodeIgnoreCase(String shortCode);

}