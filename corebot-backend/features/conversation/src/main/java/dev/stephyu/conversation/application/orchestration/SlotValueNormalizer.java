package dev.stephyu.conversation.application.orchestration;

import dev.stephyu.conversation.application.analysis.AnalyzedEntity;
import dev.stephyu.conversation.domain.slot.SlotDataValue;
import dev.stephyu.conversation.domain.slot.SlotName;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface SlotValueNormalizer {

    Optional<SlotDataValue> normalize(AnalyzedEntity entity, SlotName slotName, SlotNormalizationContext context);
}
