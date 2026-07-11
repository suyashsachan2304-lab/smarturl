package com.suyash.smarturl.scheduler;

import com.suyash.smarturl.entity.UrlMapping;
import com.suyash.smarturl.repository.UrlRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Transactional
public class UrlExpirationScheduler {

    private final UrlRepository urlRepository;

    public UrlExpirationScheduler(
            UrlRepository urlRepository) {

        this.urlRepository = urlRepository;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void deactivateExpiredUrls() {

        List<UrlMapping> expiredUrls =
                urlRepository.findByActiveTrueAndExpiresAtBefore(
                        LocalDateTime.now());

        expiredUrls.forEach(url -> url.setActive(false));

        urlRepository.saveAll(expiredUrls);
    }

}