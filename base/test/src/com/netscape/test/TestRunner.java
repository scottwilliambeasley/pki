package com.netscape.test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner {

    public Result run(String... args) throws Exception {

        JUnitCore core = new JUnitCore();
        core.addListener(new TestListener());

        List<Class<?>> classes= new ArrayList<Class<?>>();
        List<Failure> missingClasses= new ArrayList<Failure>();
        for (String each : args) {
              try {
                    classes.add(Class.forName(each));
              } catch (ClassNotFoundException e) {
                    Description description= Description.createSuiteDescription(each);
                    Failure failure= new Failure(description, e);
                    missingClasses.add(failure);
              }
        }
        Result result= core.run(classes.toArray(new Class[0]));
        for (Failure each : missingClasses)
              result.getFailures().add(each);

        return result;
    }

    public static void main(String... args) throws Exception {

        TestRunner runner = new TestRunner();
        Result result = runner.run(args);

        String status;
        int rc;

        if (result.wasSuccessful()) {
            status = "PASSED";
            rc = 0;
        } else {
            status = "FAILED";
            rc = 1;
        }

        System.err.println("TestRunner: Test " + status);

        Path path = Paths.get(System.getProperty("junit.reports.dir"));
        System.out.println("TestRunner: See test reports in " + path.toAbsolutePath());

        System.exit(rc);
    }
}
