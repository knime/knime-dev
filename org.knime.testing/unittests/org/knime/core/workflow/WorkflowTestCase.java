/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
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
 * History
 *   01.11.2008 (wiswedel): created
 */
package org.knime.core.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.TestCase;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.NodeContainer.State;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;

/**
 * 
 * @author wiswedel, University of Konstanz
 */
public class WorkflowTestCase extends TestCase {
    
    private final NodeLogger m_logger = NodeLogger.getLogger(getClass());
    
    private WorkflowManager m_manager;
    private WorkflowListener m_workflowListener;
    private ReentrantLock m_lock;
    private Condition m_workflowStableCondition;
    
    /**
     * 
     */
    public WorkflowTestCase() {
        m_lock = new ReentrantLock();
        m_workflowStableCondition = m_lock.newCondition();
        m_workflowListener = new WorkflowListener() {
            @Override
            public void workflowChanged(WorkflowEvent event) {
                m_lock.lock();
                try {
                    m_logger.info("Received " + event);
                    m_workflowStableCondition.signalAll();
                } finally {
                    m_lock.unlock();
                }
            }
        };
    }
    
    protected NodeID loadAndSetWorkflow() throws Exception {
        ClassLoader l = getClass().getClassLoader();
        String workflowDirString = getClass().getPackage().getName();
        URL workflowURL = l.getResource(workflowDirString.replace('.', '/'));
        if (workflowURL == null) {
            throw new Exception("Can't load workflow that's expected to be " 
                    + "in package " + workflowDirString);
        }
        File workflowDir = new File(workflowURL.getFile());
        if (!workflowDir.isDirectory()) {
            throw new Exception("Can't load workflow directory: " 
                    + workflowDirString);
        }
        WorkflowLoadResult loadResult = WorkflowManager.ROOT.load(
                workflowDir, new ExecutionMonitor());
        WorkflowManager m = loadResult.getWorkflowManager();
        if (m == null) {
            throw new Exception("Errors reading workflow: " 
                    + loadResult.getErrors());
        } else if (loadResult.hasErrors()) {
            m_logger.info("Errors reading workflow (proceeding anyway): ");
            dumpLineBreakStringToLog(loadResult.getErrors());
        }
        setManager(m);
        return m.getID();
    }
    
    /**
     * @param manager the manager to set
     */
    protected void setManager(WorkflowManager manager) {
        if (m_manager != null) {
            m_manager.removeListener(m_workflowListener);
        }
        m_manager = manager;
        if (m_manager != null) {
            m_manager.addListener(m_workflowListener);
        }
    }
    
    /**
     * @return the manager
     */
    protected WorkflowManager getManager() {
        return m_manager;
    }
    
    protected void lock() {
        m_lock.lock();
    }
    
    protected void unlock() {
        m_lock.unlock();
    }
    
    protected void checkState(final NodeID id, 
            final State expected) throws Exception {
        if (m_manager == null) {
            throw new NullPointerException("WorkflowManager not set.");
        }
        NodeContainer nc = m_manager.getNodeContainer(id);
        checkState(nc, expected);
    }
    
    protected void checkState(final NodeContainer nc, 
            final State expected) throws Exception {
        State actual = nc.getState();
        if (!actual.equals(expected)) {
            String error = "node " + nc.getNameWithID() + " has wrong state; "
            + "expected " + expected + ", actual " + actual + " (dump follows)";
            m_logger.info("Test failed: " + error);
            dumpWorkflowToLog();
            fail(error);
        }
    }
    
    protected void executeAndWait(final NodeID... ids) 
        throws Exception {
        getManager().executeUpToHere(ids);
        m_lock.lock();
        try {
            for (NodeID id : ids) {
                waitWhileNodeInExecution(id);
            } 
        } finally {
            m_lock.unlock();
        }
    }
    
    protected void waitWhileInExecution() throws InterruptedException {
        waitWhileNodeInExecution(m_manager);
    }

    protected void waitWhileNodeInExecution(final NodeID node) 
        throws InterruptedException {
        waitWhileNodeInExecution(m_manager.getNodeContainer(node));
    }
    
    protected void waitWhileNodeInExecution(final NodeContainer node) 
    throws InterruptedException {
        if (!m_lock.isHeldByCurrentThread()) {
            throw new IllegalStateException(
                    "Ill-posed test case; thread must own lock");
        }
        m_logger.debug("node " + node.getNameWithID() + " is " + node.getState());
        while (node.getState().executionInProgress()) {
            m_logger.debug("node " + node.getNameWithID() + " is " + node.getState());
            m_workflowStableCondition.await();
        }
    }
    
    /** {@inheritDoc} */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if (m_manager != null) {
            // in most cases we wait for individual nodes to finish. This
            // does not mean that the workflow state is also updated, give
            // it a second to finish its cleanup
            if (m_manager.getState().executionInProgress()) {
                m_lock.lock();
                try {
                    m_workflowStableCondition.await(2, TimeUnit.SECONDS);
                } finally {
                    m_lock.unlock();
                }
            }
            if (!WorkflowManager.ROOT.canRemoveNode(m_manager.getID())) {
                String error = "Cannot remove workflow, dump follows";
                m_logger.error(error);
                dumpWorkflowToLog();
                fail(error);
            }
            WorkflowManager.ROOT.removeProject(m_manager.getID());
            setManager(null);
        }
    }
    
    protected void dumpWorkflowToLog() throws IOException {
        String toString = m_manager.printNodeSummary(m_manager.getID(), 0);
        dumpLineBreakStringToLog(toString);
    }
    
    protected void dumpLineBreakStringToLog(final String s) throws IOException {
        BufferedReader r = new BufferedReader(new StringReader(s));
        String line;
        while ((line = r.readLine()) != null) {
            m_logger.info(line);
        }
        r.close();
    }
}
