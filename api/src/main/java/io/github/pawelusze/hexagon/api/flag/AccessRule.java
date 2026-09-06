package io.github.pawelusze.hexagon.api.flag;

import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * A rule that allows or denies elements of a type, optionally limited to a list of targets.
 *
 * <p>Without targets the rule applies to every element. With targets, the state applies to the
 * listed elements and the opposite state to everything else, so {@code deny(tnt)} blocks only
 * TNT while {@code allowOnly(stone)} blocks everything but stone. At most {@value #MAX_TARGETS}
 * targets may be listed.
 *
 * @param state the state applied to the targets, or to everything when no target is listed
 * @param targets the elements the state applies to; empty means all elements
 * @param <E> the element type, for example a block material
 */
public record AccessRule<E>(@NotNull State state, @NotNull Set<E> targets) {

    /** Maximum number of targets a single rule may list. */
    public static final int MAX_TARGETS = 64;

    /**
     * Creates a rule, defensively copying the targets.
     *
     * @param state the state
     * @param targets the targets
     * @throws IllegalArgumentException if more than {@value #MAX_TARGETS} targets are listed
     */
    public AccessRule {
        if (targets.size() > MAX_TARGETS) {
            throw new IllegalArgumentException("A rule may list at most " + MAX_TARGETS + " targets");
        }
        targets = Set.copyOf(targets);
    }

    /**
     * Creates a rule allowing every element.
     *
     * @param <E> the element type
     * @return the rule
     */
    public static <E> @NotNull AccessRule<E> allow() {
        return new AccessRule<>(State.ALLOW, Set.of());
    }

    /**
     * Creates a rule denying every element.
     *
     * @param <E> the element type
     * @return the rule
     */
    public static <E> @NotNull AccessRule<E> deny() {
        return new AccessRule<>(State.DENY, Set.of());
    }

    /**
     * Creates a rule allowing only the listed elements.
     *
     * @param targets the allowed elements
     * @param <E> the element type
     * @return the rule
     */
    public static <E> @NotNull AccessRule<E> allowOnly(@NotNull Set<E> targets) {
        return new AccessRule<>(State.ALLOW, targets);
    }

    /**
     * Creates a rule denying only the listed elements.
     *
     * @param targets the denied elements
     * @param <E> the element type
     * @return the rule
     */
    public static <E> @NotNull AccessRule<E> denyOnly(@NotNull Set<E> targets) {
        return new AccessRule<>(State.DENY, targets);
    }

    /**
     * Tells whether the rule applies to every element rather than a list of targets.
     *
     * @return true if no targets are listed
     */
    public boolean isUniversal() {
        return this.targets.isEmpty();
    }

    /**
     * Evaluates the rule for an element.
     *
     * @param element the element
     * @return true if the element is allowed
     */
    public boolean allows(@NotNull E element) {
        boolean targeted = this.isUniversal() || this.targets.contains(element);
        return targeted == this.state.isAllowed();
    }
}
