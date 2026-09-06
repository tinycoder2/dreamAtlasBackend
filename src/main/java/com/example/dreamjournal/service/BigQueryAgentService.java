package com.example.dreamjournal.service;

import com.example.dreamjournal.config.BigQueryAgentConfig;
import com.google.api.gax.rpc.ServerStream;
import com.google.cloud.geminidataanalytics.v1.ChatRequest;
import com.google.cloud.geminidataanalytics.v1.DataAgentContext;
import com.google.cloud.geminidataanalytics.v1.DataChatServiceClient;
import com.google.cloud.geminidataanalytics.v1.Message;
import com.google.cloud.geminidataanalytics.v1.UserMessage;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class BigQueryAgentService {

    private final BigQueryAgentConfig config;

    public BigQueryAgentService(BigQueryAgentConfig config) {
        this.config = config;
    }

    public String testChat(String question) throws IOException {

        DataAgentContext agentContext =
                DataAgentContext.newBuilder()
                        .setDataAgent(config.getAgentResourceName())
                        .setContextVersion(
                                DataAgentContext.ContextVersion.PUBLISHED
                        )
                        .build();

        UserMessage userMessage =
                UserMessage.newBuilder()
                        .setText(question)
                        .build();

        Message message =
                Message.newBuilder()
                        .setUserMessage(userMessage)
                        .build();

        ChatRequest request =
                ChatRequest.newBuilder()
                        .setParent(config.getParent())
                        .setDataAgentContext(agentContext)
                        .addMessages(message)
                        .build();

        StringBuilder answer = new StringBuilder();

        try (DataChatServiceClient client =
                     DataChatServiceClient.create()) {

            ServerStream<Message> stream =
                    client.chatCallable().call(request);

            for (Message response : stream) {

                if (response.hasSystemMessage()
                        && response.getSystemMessage().hasText()) {

                    var textMessage =
                            response.getSystemMessage().getText();

                    answer.append(
                            String.join("", textMessage.getPartsList())
                    );
                }
            }
        }

        return answer.toString();
    }
}