package com.whatsappbot.whatsappservice.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

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
            // Generar PDF en memoria
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document documento = new Document();
            PdfWriter.getInstance(documento, baos);
            documento.open();

            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            documento.add(new Paragraph("🧾 COMANDA DEL BAR LÁCTEO", tituloFont));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Pedido ID: " + pedido.getPedidoId(), normalFont));
            documento.add(new Paragraph("Cliente: " + pedido.getTelefono(), normalFont));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Detalle del pedido:", normalFont));
            documento.add(new Paragraph(pedido.getDetalle(), normalFont));
            documento.add(new Paragraph(" "));

            // Buscar y mostrar total si está en el detalle
            double total = extraerTotalDesdeDetalle(pedido.getDetalle());
            if (total > 0) {
                documento.add(new Paragraph("TOTAL: $" + String.format("%.0f", total), tituloFont));
            }

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

    private double extraerTotalDesdeDetalle(String detalle) {
        double total = 0;
        if (detalle == null) return total;

        String[] lineas = detalle.split("\n");
        for (String linea : lineas) {
            try {
                String[] partes = linea.split(" x ");
                if (partes.length == 2) {
                    String cantidadStr = partes[0].replaceAll("[^0-9]", "");
                    String nombreYPrecio = partes[1];
                    int cantidad = Integer.parseInt(cantidadStr);
                    // Si el precio está entre paréntesis al final del nombre
                    if (nombreYPrecio.matches(".*\\(\\$\\d+\\)$")) {
                        int precio = Integer.parseInt(nombreYPrecio.replaceAll(".*\\(\\$(\\d+)\\)", "$1"));
                        total += cantidad * precio;
                    }
                }
            } catch (Exception e) {
                // Silenciar errores por líneas mal formateadas
            }
        }
        return total;
    }
}
