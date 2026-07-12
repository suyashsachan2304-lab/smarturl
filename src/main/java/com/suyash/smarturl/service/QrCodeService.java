package com.suyash.smarturl.service;

public interface QrCodeService {

    byte[] generateQrCode(String shortCode);

}