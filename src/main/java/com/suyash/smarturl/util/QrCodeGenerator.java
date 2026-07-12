package com.suyash.smarturl.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.suyash.smarturl.exception.QrCodeGenerationException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Component
public class QrCodeGenerator {

    public byte[] generate(
            String text,
            int width,
            int height,
            String format) {

        try {

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    text,
                    BarcodeFormat.QR_CODE,
                    width,
                    height,
                    Map.of(
                            EncodeHintType.MARGIN, 2));

            BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            ImageIO.write(
                    image,
                    format,
                    outputStream);

            return outputStream.toByteArray();

        } catch (Exception ex) {

            throw new QrCodeGenerationException(
                    "Failed to generate QR Code.",
                    ex);
        }

    }

}