package com.acschat.app;

import com.azure.core.credential.AzureKeyCredential;
import com.microsoft.semantickernel.*;
import com.microsoft.semantickernel.ai.chatcompletion.ChatCompletionService;
import com.microsoft.semantickernel.ai.chatcompletion.openai.OpenAIAsyncClient;
import com.microsoft.semantickernel.ai.chatcompletion.openai.OpenAIClientBuilder;
import com.microsoft.semantickernel.plugins.KernelPlugin;
import com.microsoft.semantickernel.plugins.KernelPluginFactory;
import com.google.gson.Gson;

import java.util.List;
import java.util.Scanner;

public class App {
    private static final String AZURE_CLIENT_KEY = "your-key-here";
    private static final String CLIENT_ENDPOINT = "your-endpoint-here";
    private static final String MODEL_ID = "your-model-id-here";

    public static void main(String[] args) {
        OpenAIAsyncClient client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
            .endpoint(CLIENT_ENDPOINT)
            .buildAsyncClient();

        KernelPlugin lightPlugin = KernelPluginFactory.createFromObject(new LightsPlugin(), "LightsPlugin");

        ChatCompletionService chatCompletionService = OpenAIChatCompletion.builder()
            .withModelId(MODEL_ID)
            .withOpenAIAsyncClient(client)
            .build();

        Kernel kernel = Kernel.builder()
            .withAIService(ChatCompletionService.class, chatCompletionService)
            .withPlugin(lightPlugin)
            .build();

        ContextVariableTypes.addGlobalConverter(
            ContextVariableTypeConverter.builder(LightModel.class)
                .toPromptString(new Gson()::toJson)
                .build()
        );

        InvocationContext invocationContext = new InvocationContext.Builder()
            .withReturnMode(InvocationReturnMode.LAST_MESSAGE_ONLY)
            .withToolCallBehavior(ToolCallBehavior.allowAllKernelFunctions(true))
            .build();

        ChatHistory history = new ChatHistory();

        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            System.out.print("User > ");
            userInput = scanner.nextLine();

            history.addUserMessage(userInput);

            List<ChatMessageContent<?>> results = chatCompletionService
                .getChatMessageContentsAsync(history, kernel, invocationContext)
                .block();

            for (ChatMessageContent<?> result : results) {
                if (result.getAuthorRole() == AuthorRole.ASSISTANT && result.getContent() != null) {
                    System.out.println("Assistant > " + result.getContent());
                }
                history.addMessage(result);
            }
        } while (userInput != null && !userInput.isEmpty());

        scanner.close();
    }
}
