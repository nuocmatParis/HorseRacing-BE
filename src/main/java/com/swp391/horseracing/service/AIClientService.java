package com.swp391.horseracing.service;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIClientService {

    @Value("${ai.model}")
    private String model;

    private final OpenAIClient client;

    public String predict(String prompt) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(model)
                .input(prompt)
                .build();

        Response response = client.responses().create(params);

        if (response.output().isEmpty()) {
            log.error("AI response has no output");
            throw new RuntimeException("AI response has no output");
        }

        ResponseOutputItem outputItem = response.output().get(0);
        if (!outputItem.isMessage()) {
            log.error("AI response first output is not a message: {}", outputItem);
            throw new RuntimeException("AI response first output is not a message");
        }

        ResponseOutputMessage message = outputItem.asMessage();
        if (message.content().isEmpty()) {
            log.error("AI response message has no content");
            throw new RuntimeException("AI response message has no content");
        }

        ResponseOutputText outputText = message.content().get(0).asOutputText();
        return outputText.text();
    }
}
