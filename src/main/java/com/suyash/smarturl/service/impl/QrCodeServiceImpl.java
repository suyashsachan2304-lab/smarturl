package com.suyash.smarturl.service.impl;

import com.suyash.smarturl.config.AppProperties;
import com.suyash.smarturl.entity.UrlMapping;
import com.suyash.smarturl.service.QrCodeService;
import com.suyash.smarturl.service.UrlService;
import com.suyash.smarturl.util.QrCodeGenerator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional(readOnly = true)
public class QrCodeServiceImpl implements QrCodeService {

    private final UrlService urlService;
    private final QrCodeGenerator qrCodeGenerator;
    private final AppProperties appProperties;

    public QrCodeServiceImpl(
            UrlService urlService,
            QrCodeGenerator qrCodeGenerator,
            AppProperties appProperties) {

        this.urlService = urlService;
        this.qrCodeGenerator = qrCodeGenerator;
        this.appProperties = appProperties;
    }

    @Override
    public byte[] generateQrCode(String shortCode) {

        UrlMapping entity = urlService.getActiveUrlMapping(shortCode);

        return qrCodeGenerator.generate(
                entity.getShortUrl(),
                appProperties.getQr().getWidth(),
                appProperties.getQr().getHeight(),
                appProperties.getQr().getImageFormat());
    }

}