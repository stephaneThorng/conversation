package dev.stephyu.conversation.adapter.outbound.llm;

import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.guardrail.InputGuardrailResult;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Input guardrail applied before any LLM call.
 * Blocks oversized messages and known prompt-injection patterns
 * without consuming any tokens.
 */
@NullMarked
public final class ConversationInputGuardrail implements InputGuardrail {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationInputGuardrail.class);

    private static final int MAX_MESSAGE_LENGTH = 1000;

    /** Known prompt-injection trigger phrases (case-insensitive). */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore (previous|all|your) (instructions?|rules?|prompt|system)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget (everything|all|your instructions?|your rules?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("you are now|act as|pretend (you are|to be)|roleplay as", Pattern.CASE_INSENSITIVE),
            Pattern.compile("do anything now|DAN mode|jailbreak", Pattern.CASE_INSENSITIVE),
            Pattern.compile("repeat (your|the) (system|instructions?|prompt|rules?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reveal (your|the) (system|prompt|instructions?|configuration|api key)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("override (your|the) (instructions?|rules?|prompt)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[INST\\]|<\\|system\\|>|<\\|user\\|>", Pattern.CASE_INSENSITIVE)
    );

    @Override
    public InputGuardrailResult validate(InputGuardrailRequest request) {
        String text = request.userMessage().singleText();

        // 1. Length check
        if (text.length() > MAX_MESSAGE_LENGTH) {
            LOGGER.warn("Guardrail blocked oversized message: length={}", text.length());
            return failure("Your message is too long. Please keep it under " + MAX_MESSAGE_LENGTH + " characters.");
        }

        // 2. Prompt injection detection
        String lower = text.toLowerCase(Locale.ROOT);
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(lower).find()) {
                LOGGER.warn("Guardrail blocked suspected prompt injection: pattern={}", pattern.pattern());
                return failure("I can only assist with reservations and menu questions for this restaurant.");
            }
        }

        return success();
    }
}
