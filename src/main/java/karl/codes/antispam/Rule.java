package karl.codes.antispam;

/**
 * Created by karl on 12/8/2015.
 */
public interface Rule<N> {
    Rule OK = new Rule() {
        @Override
        public boolean test(Object input, String last) {
            return true;
        }

        @Override
        public Action onHit() {
            return Action.OK;
        }

        @Override
        public Rule onMiss() {
            return null;
        }

        @Override
        public String name() {
            return "OK";
        }
    };

    boolean test(N input, String last);
    Action onHit();
    Rule<N> onMiss();
    String name();
}
