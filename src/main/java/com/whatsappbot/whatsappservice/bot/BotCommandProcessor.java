package com.whatsappbot.whatsappservice.bot;

public class BotCommandProcessor {

    public static String procesar(String texto) {
        String mensaje = texto.toLowerCase();

        if (contieneAlguna(mensaje, "hola", "buenas", "holi")) {
            return "¡Hola! Bienvenido al Bar Lácteo 🍦\nEscribe:\n1️⃣ *catálogo* para ver productos\n2️⃣ *pagar* para generar un link de pago";
        }

        if (contieneAlguna(mensaje, "catalogo", "catálogo")) {
            return "Aquí tienes nuestro catálogo 🧀🍰:\n1. Leche - $1.000\n2. Yogur - $1.200\n3. Helado - $2.500\n\nResponde con el número del producto para ordenarlo.";
        }

        if (contieneAlguna(mensaje, "pagar", "pago", "pago con tarjeta")) {
            return "Genial. Para pagar con tarjeta, accede a este enlace seguro de WebPay:\nhttps://barlacteo.cl/pago?id=PEDIDO123";
        }

        return "No entendí tu mensaje 🤔. Escribe *catálogo* o *pagar* para continuar.";
    }

    private static boolean contieneAlguna(String mensaje, String... palabrasClave) {
        for (String palabra : palabrasClave) {
            if (mensaje.contains(palabra)) return true;
        }
        return false;
    }
}
