package dev.stephyu.conversation.application.port.outbound;

import dev.stephyu.conversation.application.analysis.ConversationAnalysis;
import dev.stephyu.conversation.application.analysis.ConversationAnalysisRequest;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ConversationAnalyzerPort {

    ConversationAnalysis analyze(ConversationAnalysisRequest request);
}
