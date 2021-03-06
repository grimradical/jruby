package org.jruby.test;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestCase;

/**
 *
 * @author headius
 */
public class Ruby1_9TestSuite extends TestUnitTestSuite {
    public static final String TEST_INDEX = "ruby_1_9_index";

    public Ruby1_9TestSuite(String testIndex) throws Exception {
        super(testIndex);
    }

    /**
     * suite method automatically generated by JUnit module
     */
    public static Test suite() throws Exception {
        return new Ruby1_9TestSuite(TEST_INDEX);
    }

    @Override
    protected TestCase createTest(String line, File testDir, TestUnitTestSuite.Interpreter interpreter) {
        return new Ruby1_9ScriptTest(line, testDir, interpreter);
    }

    protected class Ruby1_9ScriptTest extends TestUnitTestSuite.ScriptTest {
        public Ruby1_9ScriptTest(String filename, File dir, TestUnitTestSuite.Interpreter interpreter) {
            super(filename, dir, interpreter);
        }

        @Override
        protected String generateTestScript(String scriptName, String testClass) {
            StringBuffer script = new StringBuffer();
            script.append("$: << '.' unless $:.include? '.'").append('\n');
            script.append("$: << 'test/externals/ruby1.9' unless $:.include? 'test/externals/ruby1.9'").append('\n');
            script.append("require 'minitest/unit'\n");
            script.append("ENV['EXCLUDE_DIR'] = 'test/externals/ruby1.9/excludes'\n");
            script.append("require 'minitest/excludes'\n");
            script.append("require '" + scriptName + "'\n");
            script.append("unit = MiniTest::Unit.new\n");
            script.append("unit.run\n");
            script.append("unit.report\n");

            return script.toString();
        }
    }
}
