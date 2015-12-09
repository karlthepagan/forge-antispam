package karl.codes.antispam;

/**
 * Created by karl on 12/8/2015.
 */
public interface IRule<N> {
    IRule OK = new IRule() {
        @Override
        public boolean test(Object input, String last) {
            return true;
        }

        @Override
        public Action onHit() {
            return Action.OK;
        }

        @Override
        public IRule onMiss() {
            return null;
        }

        @Override
        public String name() {
            return "OK";
        }
    };

    boolean test(N input, String last);
    Action onHit();
    IRule<N> onMiss();
    String name();
}
