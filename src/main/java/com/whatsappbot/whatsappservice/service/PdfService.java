package com.whatsappbot.whatsappservice.service;

import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.whatsappbot.whatsappservice.model.PedidoEntity;

@Service
public class PdfService {

    public byte[] generarComandaPDF(PedidoEntity pedido) throws Exception {
        Document document = new Document();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

        document.add(new Paragraph("COMANDA DE PEDIDO", titleFont));
        document.add(new Paragraph("Pedido ID: " + pedido.getPedidoId(), normalFont));
        document.add(new Paragraph("Teléfono: " + pedido.getTelefono(), normalFont));
        document.add(new Paragraph("Estado: " + pedido.getEstado(), normalFont));
        document.add(new Paragraph("Detalle:\n" + pedido.getDetalle(), normalFont));

        document.close();
        return baos.toByteArray();
    }
}
