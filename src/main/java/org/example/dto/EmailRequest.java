package org.example.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter @Setter
public class EmailRequest {
    private String to;
    private String subject;
    private String body;

    public EmailRequest(String mail, String testTitle, String testBody) {
        to = mail;
        subject = testTitle;
        body = testBody;
    }
}
