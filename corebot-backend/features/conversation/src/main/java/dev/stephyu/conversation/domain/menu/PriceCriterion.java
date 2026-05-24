package dev.stephyu.conversation.domain.menu;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record PriceCriterion(
        PriceComparator comparator,
        @Nullable Integer minPriceCents,
        @Nullable Integer maxPriceCents
) {

    public PriceCriterion {
        if (comparator == PriceComparator.BETWEEN) {
            if (minPriceCents == null || maxPriceCents == null) {
                throw new IllegalArgumentException("BETWEEN requires both minPriceCents and maxPriceCents");
            }
            if (minPriceCents < 0 || maxPriceCents < 0) {
                throw new IllegalArgumentException("price bounds must be positive");
            }
            if (minPriceCents > maxPriceCents) {
                throw new IllegalArgumentException("minPriceCents must be <= maxPriceCents");
            }
        } else {
            if (minPriceCents == null) {
                throw new IllegalArgumentException(comparator + " requires minPriceCents");
            }
            if (minPriceCents < 0) {
                throw new IllegalArgumentException("minPriceCents must be positive");
            }
            if (maxPriceCents != null) {
                throw new IllegalArgumentException(comparator + " does not accept maxPriceCents");
            }
        }
    }
}
