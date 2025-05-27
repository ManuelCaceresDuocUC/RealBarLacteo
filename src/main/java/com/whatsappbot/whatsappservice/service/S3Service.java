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

    public String subirComanda(String nombreArchivo, InputStream contenido) {
    try {
        byte[] bytes = contenido.readAllBytes(); // Lee todo el contenido primero

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(nombreArchivo)
            .contentType("application/pdf")
            .acl("public-read")
            .build();

        s3.putObject(request, RequestBody.fromBytes(bytes));

        return "https://" + bucketName + ".s3.amazonaws.com/" +
            URLEncoder.encode(nombreArchivo, StandardCharsets.UTF_8);
    } catch (IOException e) {
        e.printStackTrace();
        return null;
    }
}

}
