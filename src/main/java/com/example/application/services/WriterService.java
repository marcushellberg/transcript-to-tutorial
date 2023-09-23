package com.example.application.services;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.BrowserCallable;
import dev.langchain4j.model.input.structured.StructuredPrompt;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@BrowserCallable
@AnonymousAllowed
public class WriterService {

    private Writer writer;

    @Value("${openai.api.key}")
    private String OPENAI_API_KEY;

    @StructuredPrompt({
            "You are a senior technical writer specializing in Java and React content. You take pride in writing content that is informational, educational and engages developers. Your content is specific, concise, and engaging.",
            "Generate a high-quality technical tutorial based on the given transcript, code snippets, and SEO keywords. Your tutorial should be aimed at developers, prioritizing educational and engaging content. Do not create any new examples; only use the provided code snippets. Incorporate the SEO keywords naturally into the content without sacrificing readability or educational value.",
            "Only base the tutorial on the given transcript, do not add any additional steps.",
            "Input 1: Transcript of Tutorial",
            "Please use the transcript as a base for structuring the tutorial. You can paraphrase or expand on points to fit the written format, but the core content should remain the same.",
            "===\n{{transcript}}\n===\n",
            "Input 2: Relevant Code Snippets",
            "Incorporate these code snippets at the appropriate points in the tutorial, explaining their functionality and relevance. Do not create new code examples; strictly use the code snippets provided.",
            "===\n{{code}}\n===\n",
            "Input 3: SEO Keywords",
            "Integrate these keywords into the tutorial naturally. Aim to use each keyword at least once but do not force them in; the primary focus should be on providing value to the reader.",
            "===\n{{keywords}}\n===\n",
            "Output the tutorial as a Markdown file. Use sentence case for headings and subheadings.",
            "Write in the second person, using 'you' and 'your' to address the reader. The tone of voice should be as if you were a developer explaining the topic to a colleague.",
    })
    public record Brief(
            String transcript,
            String code,
            String keywords
    ) {
    }

    interface Writer {
        TokenStream write(Brief brief);
    }

    @PostConstruct
    public void init() {
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder();
        builder.apiKey(OPENAI_API_KEY);
        builder.maxTokens(5000);
        builder.modelName("gpt-4");
        var model = builder.build();

        writer = AiServices.create(Writer.class, model);
    }

    public Flux<String> write(Brief brief) {
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

        var stream = writer.write(brief);

        stream.onNext(sink::tryEmitNext)
                .onComplete(sink::tryEmitComplete)
                .onError(sink::tryEmitError).start();

        return sink.asFlux();
    }
}
