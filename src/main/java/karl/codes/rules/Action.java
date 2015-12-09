package karl.codes.rules;

/**
* Created by karl on 11/1/15.
*/
public enum Action {
    OK,
    DENY,
    NEXT,
    PASS;

    public Action getOpposite() {
        switch (this) {
            case OK:
                return DENY;
            case DENY:
                return OK;
            case NEXT: // check more rules
                return PASS;
            case PASS: // continue down chain
                return NEXT;
            default:
                throw new IllegalStateException(this + " has no opposite");
        }
    }
}
