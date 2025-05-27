package com.whatsappbot.whatsappservice.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.whatsappbot.whatsappservice.model.PedidoEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComandaService {

    private final S3Service s3Service;

    public String generarPDF(PedidoEntity pedido) {
        try {
            // Generar PDF en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document documento = new Document();
            PdfWriter.getInstance(documento, baos);
            documento.open();

            documento.add(new Paragraph("🧾 COMANDA DEL BAR LÁCTEO"));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Pedido ID: " + pedido.getPedidoId()));
            documento.add(new Paragraph("Cliente: " + pedido.getTelefono()));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Detalle del pedido:"));
            documento.add(new Paragraph(pedido.getDetalle()));

            documento.close();

            // Subir a S3
            InputStream input = new ByteArrayInputStream(baos.toByteArray());
            String nombreArchivo = "comandas/COMANDA_" + pedido.getPedidoId() + ".pdf";
            String urlPublica = s3Service.subirComanda(nombreArchivo, input);

            System.out.println("✅ Comanda PDF generada en S3: " + urlPublica);
            return urlPublica;
        } catch (Exception e) {
            System.err.println("❌ Error al generar y subir PDF:");
            e.printStackTrace();
            return null;
        }
    }
}
