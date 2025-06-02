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
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
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

            // 📏 Solo definimos el ancho: 226 puntos (80mm), altura ilimitada
            Rectangle pageSize = new Rectangle(226, PageSize.A4.getHeight()); // se adapta automáticamente
            Document documento = new Document(pageSize, 5, 5, 10, 10);

            PdfWriter.getInstance(documento, baos);
            documento.open();

            Font bold = FontFactory.getFont(FontFactory.COURIER_BOLD, 8);
            Font normal = FontFactory.getFont(FontFactory.COURIER, 7);
            DecimalFormat df = new DecimalFormat("#,##0");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            documento.add(new Paragraph("==============================", bold));
            documento.add(new Paragraph("Bartolo Apolinav", bold));
            documento.add(new Paragraph("Jorge Montt s/n, Viña del Mar", normal));
            documento.add(new Paragraph("RUT: 76.262.419-2", normal));
            documento.add(new Paragraph("Teléfono local: 983947568", normal));
            documento.add(new Paragraph("Estado del pedido: PAGADO", normal));
            documento.add(new Paragraph("Fecha: " + pedido.getFechaCreacion().format(formatter), normal));
            documento.add(new Paragraph("Cliente: " + pedido.getTelefono(), normal));
            documento.add(new Paragraph("==============================", bold));

            documento.add(new Paragraph("DETALLE DEL PEDIDO", bold));
            documento.add(new Paragraph(pedido.getDetalle(), normal));
            documento.add(new Paragraph("--------------------------------", normal));

            if (pedido.getIndicaciones() != null && !pedido.getIndicaciones().isBlank()) {
                documento.add(new Paragraph("INDICACIONES:", bold));
                documento.add(new Paragraph(pedido.getIndicaciones(), normal));
                documento.add(new Paragraph("--------------------------------", normal));
            }

            documento.add(new Paragraph("TOTAL: $" + df.format(pedido.getMonto()), bold));
            documento.add(new Paragraph("==============================", bold));
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
