package com.whatsappbot.whatsappservice.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3Service {

    private final S3Client s3;

    @Value("${aws.bucketName}")
    private String bucketName;

    public S3Service(
        @Value("${aws.accessKeyId}") String accessKey,
        @Value("${aws.secretAccessKey}") String secretKey,
        @Value("${aws.region}") String region
    ) {
        AwsBasicCredentials creds = AwsBasicCredentials.create(accessKey, secretKey);
        this.s3 = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(creds))
            .build();
    }

    // ✅ Método principal: subir comanda desde byte[]
    public String subirComanda(String nombreArchivo, byte[] contenido) {
        try {
            System.out.println("📤 Subiendo a S3: bucket=" + bucketName + ", archivo=" + nombreArchivo);

            PutObjectRequest request = PutObjectRequest.builder()
    .bucket(bucketName)
    .key(nombreArchivo + ".pdf")
    .contentType("application/pdf")
    .build(); 
            s3.putObject(request, RequestBody.fromBytes(contenido));

            String url = "https://" + bucketName + ".s3.amazonaws.com/" +
                URLEncoder.encode(nombreArchivo + ".pdf", StandardCharsets.UTF_8);

            System.out.println("✅ Archivo subido a S3 correctamente: " + url);
            return url;
        } catch (Exception e) {
            System.err.println("❌ Error al subir a S3:");
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Método sobrecargado: subir comanda desde InputStream
    public String subirComanda(String nombreArchivo, InputStream contenido) {
        try {
            byte[] bytes = contenido.readAllBytes();
            return subirComanda(nombreArchivo, bytes);
        } catch (IOException e) {
            System.err.println("❌ Error al leer InputStream para subir a S3:");
            e.printStackTrace();
            return null;
        }
    }
}