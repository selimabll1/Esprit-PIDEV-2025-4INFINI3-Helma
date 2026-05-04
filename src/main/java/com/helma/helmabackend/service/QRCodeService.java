package com.helma.helmabackend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.helma.helmabackend.entity.Voucher;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class QRCodeService {

    public byte[] generateVoucherQR(Voucher voucher) throws Exception {

        String data =
                " CODE: " + voucher.getCode() +
                        " |VALUE: " + voucher.getValue() +
                        " |DEADLINE: " + voucher.getDeadline();

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, 300, 300);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);

        return output.toByteArray();
    }
}