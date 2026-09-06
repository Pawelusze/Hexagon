package io.github.pawelusze.hexagon.api.flag;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/** Factory of the flag types shipped with Hexagon. */
public final class FlagTypes {

    private static final FlagType<State> STATE = new StateType();
    private static final FlagType<Boolean> BOOLEAN = new BooleanType();
    private static final FlagType<String> TEXT = new TextType();

    private FlagTypes() {}

    /**
     * Returns the type of {@code allow}/{@code deny} flags.
     *
     * @return the state type
     */
    public static @NotNull FlagType<State> state() {
        return STATE;
    }

    /**
     * Returns the type of {@code true}/{@code false} flags, used by the per-region game rules.
     *
     * @return the boolean type
     */
    public static @NotNull FlagType<Boolean> bool() {
        return BOOLEAN;
    }

    /**
     * Returns the type of free-text flags such as greetings. Text is interpreted as MiniMessage
     * when displayed.
     *
     * @return the text type
     */
    public static @NotNull FlagType<String> text() {
        return TEXT;
    }

    /**
     * Creates the type of an {@link AccessRule} flag. Values read as {@code allow}, {@code deny},
     * or a state followed by a list of targets separated by commas or spaces, e.g. {@code deny
     * tnt,lava}. The words {@code *} and {@code all} are accepted as an explicit "every element".
     *
     * @param parser resolves one target name, returning empty for unknown names
     * @param formatter renders one target so that {@code parser} accepts it again
     * @param <E> the element type
     * @return the access-rule type
     */
    public static <E> @NotNull FlagType<AccessRule<E>> accessRule(
            @NotNull Function<String, Optional<E>> parser, @NotNull Function<E, String> formatter) {
        return new AccessRuleType<>(parser, formatter);
    }

    private static final class StateType implements FlagType<State> {

        @Override
        public @NotNull State parse(@NotNull String input) {
            return switch (input.trim().toLowerCase(Locale.ROOT)) {
                case "allow", "true", "on" -> State.ALLOW;
                case "deny", "false", "off" -> State.DENY;
                default -> throw new FlagValueException("Expected 'allow' or 'deny', got '" + input + "'");
            };
        }

        @Override
        public @NotNull String serialize(@NotNull State value) {
            return value.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public @NotNull List<String> suggestions() {
            return List.of("allow", "deny");
        }
    }

    private static final class BooleanType implements FlagType<Boolean> {

        @Override
        public @NotNull Boolean parse(@NotNull String input) {
            return switch (input.trim().toLowerCase(Locale.ROOT)) {
                case "true", "yes", "on" -> Boolean.TRUE;
                case "false", "no", "off" -> Boolean.FALSE;
                default -> throw new FlagValueException("Expected 'true' or 'false', got '" + input + "'");
            };
        }

        @Override
        public @NotNull String serialize(@NotNull Boolean value) {
            return value.toString();
        }

        @Override
        public @NotNull List<String> suggestions() {
            return List.of("true", "false");
        }
    }

    private static final class TextType implements FlagType<String> {

        @Override
        public @NotNull String parse(@NotNull String input) {
            if (input.isBlank()) {
                throw new FlagValueException("Text must not be blank");
            }
            return input;
        }

        @Override
        public @NotNull String serialize(@NotNull String value) {
            return value;
        }

        @Override
        public @NotNull List<String> suggestions() {
            return List.of();
        }
    }

    private record AccessRuleType<E>(Function<String, Optional<E>> parser, Function<E, String> formatter)
            implements FlagType<AccessRule<E>> {

        private static final Pattern SEPARATOR = Pattern.compile("[,\\s]+");
        private static final Set<String> EVERYTHING = Set.of("*", "all");

        @Override
        public @NotNull AccessRule<E> parse(@NotNull String input) {
            String[] words = SEPARATOR.split(input.trim());
            if (words.length == 0 || words[0].isEmpty()) {
                throw new FlagValueException("Expected 'allow' or 'deny' optionally followed by targets");
            }
            State state = STATE.parse(words[0]);
            Set<E> targets = this.parseTargets(Arrays.asList(words).subList(1, words.length));
            return new AccessRule<>(state, targets);
        }

        private Set<E> parseTargets(List<String> names) {
            if (names.size() == 1 && EVERYTHING.contains(names.getFirst().toLowerCase(Locale.ROOT))) {
                return Set.of();
            }
            if (names.size() > AccessRule.MAX_TARGETS) {
                throw new FlagValueException("At most " + AccessRule.MAX_TARGETS + " targets may be listed");
            }
            Set<E> targets = new LinkedHashSet<>();
            for (String name : names) {
                targets.add(
                        parser.apply(name).orElseThrow(() -> new FlagValueException("Unknown target '" + name + "'")));
            }
            return targets;
        }

        @Override
        public @NotNull String serialize(@NotNull AccessRule<E> value) {
            String state = STATE.serialize(value.state());
            if (value.isUniversal()) {
                return state;
            }
            return state + " "
                    + value.targets().stream().map(formatter).sorted().collect(Collectors.joining(","));
        }

        @Override
        public @NotNull List<String> suggestions() {
            return List.of("allow", "deny");
        }
    }
}
