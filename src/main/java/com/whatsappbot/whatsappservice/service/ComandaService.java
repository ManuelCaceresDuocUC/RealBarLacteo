package com.whatsappbot.whatsappservice.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.whatsappbot.whatsappservice.model.PedidoEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;

@Service
public class ComandaService {

    private static final String DIRECTORIO = "comandas"; // carpeta local donde se guardan

    public void generarPDF(PedidoEntity pedido) {
        try {
            // Crear carpeta si no existe
            File carpeta = new File(DIRECTORIO);
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }

            // Ruta del archivo
            String archivo = DIRECTORIO + "/COMANDA_" + pedido.getPedidoId() + ".pdf";

            // Crear documento
            Document documento = new Document();
            PdfWriter.getInstance(documento, new FileOutputStream(archivo));
            documento.open();

            // Título
            documento.add(new Paragraph("🧾 COMANDA DEL BAR LÁCTEO"));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Pedido ID: " + pedido.getPedidoId()));
            documento.add(new Paragraph("Cliente: " + pedido.getTelefono()));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Detalle del pedido:"));
            documento.add(new Paragraph(pedido.getDetalle()));

            documento.close();

            System.out.println("✅ Comanda PDF generada en: " + archivo);

        } catch (Exception e) {
            System.err.println("❌ Error al generar PDF de comanda:");
            e.printStackTrace();
        }
    }
}
