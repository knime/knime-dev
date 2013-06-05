/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2013
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 */
package org.knime.testing.headless;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.ui.PlatformUI;
import org.knime.testing.core.AbstractTestcaseCollector;

/**
 * This application runs all Unit tests it can find. It collects all classes by
 * querying implementations of {@link AbstractTestcaseCollector} that are
 * registered at the extension point.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class UnittestRunnerApplication implements IApplication {
    private volatile boolean m_stopped;

    private File m_destDir;

    @Override
    public Object start(final IApplicationContext context) throws Exception {
        PlatformUI.createDisplay(); // create a display because some tests may need it
        context.applicationRunning();
        Object args =
                context.getArguments()
                        .get(IApplicationContext.APPLICATION_ARGS);

        if (!extractCommandLineArgs(args) || (m_destDir == null)) {
            printUsage();
            return EXIT_OK;
        }
        if (!m_destDir.mkdirs()) {
            throw new IOException("Could not create destination directory '" + m_destDir + "'");
        }

        // run the tests
        for (Class<?> testClass : AllJUnitTests.getAllJunitTests()) {
            if (m_stopped) {
                System.err.println("Tests aborted");
                break;
            }

            System.out.println("======= Running " + testClass.getName() + " =======");
            JUnitTest junitTest = new JUnitTest(testClass.getName());
            JUnitTestRunner runner = new JUnitTestRunner(junitTest, false, false, false, testClass.getClassLoader());
            XMLJUnitResultFormatter formatter = new XMLJUnitResultFormatter();
            OutputStream out = new FileOutputStream(new File(m_destDir, testClass.getName() + ".xml"));
            formatter.setOutput(out);
            runner.addFormatter(formatter);
            runner.run();
            out.close();
        }

        return EXIT_OK;
    }

    @Override
    public void stop() {
        m_stopped = true;
    }

    /**
     * Extracts from the passed object the arguments. Returns true if everything
     * went smooth, false if the application must exit.
     *
     * @param args the object with the command line arguments.
     * @return true if the members were set according to the command line
     *         arguments, false, if an error message was printed and the
     *         application must exit.
     */
    private boolean extractCommandLineArgs(final Object args) {
        String[] stringArgs;
        if (args instanceof String[]) {
            stringArgs = (String[])args;
            if (stringArgs.length > 0 && stringArgs[0].equals("-pdelaunch")) {
                String[] copy = new String[stringArgs.length - 1];
                System.arraycopy(stringArgs, 1, copy, 0, copy.length);
                stringArgs = copy;
            }
        } else if (args != null) {
            System.err.println("Unable to read application's arguments."
                    + " (was expecting a String array, but got a "
                    + args.getClass().getName() + ". toString() returns '"
                    + args.toString() + "')");
            return false;
        } else {
            stringArgs = new String[0];
        }

        int i = 0;
        while (i < stringArgs.length) {

            // "-destDir" specifies the destination directory
            if ((stringArgs[i] != null) && stringArgs[i].equals("-destDir")) {
                if (m_destDir != null) {
                    System.err.println("You can't specify multiple -destDir "
                            + "options at the command line");
                    return false;
                }

                i++;
                // requires another argument
                if ((i >= stringArgs.length) || (stringArgs[i] == null)
                        || (stringArgs[i].length() == 0)) {
                    System.err
                            .println("Missing <dir_name> for option -destDir.");
                    printUsage();
                    return false;
                }
                m_destDir = new File(stringArgs[i++]);
                continue;
            }

            System.err.println("Invalid option: '" + stringArgs[i] + "'\n");
            printUsage();
            return false;
        }

        return true;
    }

    private void printUsage() {
        System.err.println("Valid arguments:");

        System.err.println("    -destDir <dir_name>: specifies the"
                + " directory into which the test results are written.");
    }
}
