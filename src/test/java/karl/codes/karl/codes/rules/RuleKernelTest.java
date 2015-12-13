package karl.codes.karl.codes.rules;

import com.google.common.base.Functions;
import karl.codes.rules.RuleKernel;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by karl on 12/10/2015.
 */
public class RuleKernelTest {
    @Before
    public void setUp() throws Exception {
        target = new RuleKernel<>(Functions.identity(),null);
    }

    @After
    public void tearDown() throws Exception {
        target = null;
    }

    RuleKernel<String,String> target;

    @Test
    public void testApplyRules() throws Exception {

    }
}
