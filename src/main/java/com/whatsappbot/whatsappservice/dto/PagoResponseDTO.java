package com.whatsappbot.whatsappservice.dto;

public class PagoResponseDTO {
    private String url;
    private String token;

    
    public PagoResponseDTO(String url, String token) {
        this.url = url;
        this.token = token;
    }

    public String getUrl() {
        return url;
    }

    public String getToken() {
        return token;
    }
}