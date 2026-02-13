package de.pamunda.nexusfin.exchange.ex_world_engine.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient.Builder chatclientBuilder(OpenAiChatModel openAiChatModel){
        return ChatClient.builder(openAiChatModel);
    }

    @Bean("newsChatClient")
    public ChatClient chatClientGPT( ChatClient.Builder builder) {
        ChatOptions chatOptions = ChatOptions.builder().model("gpt-oss-120b-sovereign").build();

        return builder
                .defaultOptions(chatOptions)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor()))
                .defaultSystem("""
                        Regardless of the question, just answer something like "i don't know"
                        """)
                .build();
    }

}
