package com.whatsappbot.whatsappservice.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.whatsappbot.whatsappservice.dto.ProductoCarritoDTO;
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
    public byte[] generarComandaPDF(String pedidoId, List<ProductoCarritoDTO> productos, double total) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
        Document document = new Document(PageSize.A7);
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("🧾 COMANDA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        document.add(new Paragraph("Pedido ID: " + pedidoId));
        document.add(new Paragraph(" "));

        for (ProductoCarritoDTO producto : productos) {
            String linea = String.format("%dx %s - $%.0f", 
                producto.getQuantity(), 
                producto.getName(), 
                producto.getPrice() * producto.getQuantity());
            document.add(new Paragraph(linea));
        }

        document.add(new Paragraph(" "));
        document.add(new Paragraph("Total: $" + Math.round(total), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));

        document.close();
        return out.toByteArray();

    } catch (Exception e) {
        throw new RuntimeException("❌ Error al generar PDF de comanda", e);
    }
}

}
