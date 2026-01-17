package org.example.service;

import org.example.dto.EmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailConsumer {

    @Autowired
    private JavaMailSender mailSender;

    @KafkaListener(topics = "mail-tasks", groupId = "notification-group")
    public void consume(EmailRequest request) {
        System.out.println("TEST LOG: Received message for " + request.getTo());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.getTo());
        message.setSubject(request.getSubject());
        message.setText(request.getBody());

        mailSender.send(message);
        System.out.println("Email sent to: " + request.getTo());
    }
}