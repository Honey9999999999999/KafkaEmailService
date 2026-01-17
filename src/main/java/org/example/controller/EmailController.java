package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.example.dto.EmailRequest;
import org.example.service.EmailConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailController {
    @Autowired
    EmailConsumer emailConsumer;

    @Operation(summary = "Отправить письмо в очередь", description = "Принимает объект EmailRequest и пересылает его в топик Kafka")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешно отправлено в брокер"),
            @ApiResponse(responseCode = "400", description = "Ошибка в формате JSON"),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера или Kafka")
    })
    @PostMapping("/send")
    public String send(@RequestBody EmailRequest request){
        emailConsumer.consume(request);
        return "Отправлено в очередь";
    }
}
