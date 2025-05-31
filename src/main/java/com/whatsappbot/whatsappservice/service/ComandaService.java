package com.whatsappbot.whatsappservice.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
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
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document documento = new Document();
            PdfWriter.getInstance(documento, baos);
            documento.open();

            Font bold = FontFactory.getFont(FontFactory.COURIER_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.COURIER, 9);
            DecimalFormat df = new DecimalFormat("#,##0");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            documento.add(new Paragraph("==========================================", bold));
            documento.add(new Paragraph("Bartolo Apolinav", bold));
            documento.add(new Paragraph("Jorge Montt s/n, Viña del Mar", normal));
            documento.add(new Paragraph("RUT: 72.262.419-2", normal));
            documento.add(new Paragraph("Teléfono: 983947568", normal));
            documento.add(new Paragraph("Estado del pedido: PAGADO", normal));
            documento.add(new Paragraph("Fecha: " + pedido.getFechaCreacion().format(formatter), normal));
            documento.add(new Paragraph("==========================================", bold));

            documento.add(new Paragraph("DETALLE DEL PEDIDO", bold));
            documento.add(new Paragraph(pedido.getDetalle(), normal));
            documento.add(new Paragraph("------------------------------------------", normal));

            documento.add(new Paragraph("TOTAL: $" + df.format(pedido.getMonto()), bold));
            documento.add(new Paragraph("==========================================", bold));
            documento.add(new Paragraph("¡Gracias por tu preferencia!", normal));

            documento.close();

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
