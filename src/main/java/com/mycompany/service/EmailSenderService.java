package com.mycompany.service;

public interface EmailSenderService {
    void sendHtmlEmail(String to, String subject, String htmlBody);
}
