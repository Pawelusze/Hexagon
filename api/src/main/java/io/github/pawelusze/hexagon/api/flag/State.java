package io.github.pawelusze.hexagon.api.flag;

/** Binary flag value: an action is either allowed or denied. */
public enum State {
    ALLOW,
    DENY;

    /**
     * Tells whether this state permits the action.
     *
     * @return true for {@link #ALLOW}
     */
    public boolean isAllowed() {
        return this == ALLOW;
    }
}
