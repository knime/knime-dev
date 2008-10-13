/*
 * --------------------------------------------------------------------- *
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * --------------------------------------------------------------------- *
 * 
 * 2006-06-08 (tm): reviewed
 */
package org.knime.core.node.property.hilite;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.knime.core.data.RowKey;
  
/**
 * JUnit test for the <code>HiLiteHandler</code>.
 * 
 * @see HiLiteHandler
 *  
 * @author Thomas Gabriel, University of Konstanz
 */
public final class DefaultHiLiteHandlerTest extends TestCase {
          
    private RowKey m_c1 = new RowKey("m_c1");
    private RowKey m_c2 = new RowKey("m_c2");
    private RowKey m_c3 = new RowKey("m_c3");

    /** Set with m_c1 and m_c2. */
    private Set<RowKey> m_s12;
    /** Set with m_c2 and m_c3. */
    private Set<RowKey> m_s23;
    
    private class MyHiLiteListener implements HiLiteListener {
        public void hiLite(final KeyEvent event) {
        }
        public void unHiLite(final KeyEvent event) {
        }
        public void unHiLiteAll(final KeyEvent event) {
        }
    }
    
    private HiLiteHandler m_hdl = new HiLiteHandler();
    private HiLiteListener m_l1 = new MyHiLiteListener();
    private HiLiteListener m_l2 = new MyHiLiteListener();
    
    /**
     * Init internal members.
     * @throws Exception If setup failed.
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        m_s12 = new HashSet<RowKey>();
        m_s12.add(m_c1);
        assertTrue(m_s12.contains(m_c1));
        m_s12.add(m_c2);
        assertTrue(m_s12.contains(m_c2));
        assertTrue(m_s12.size() == 2);
        m_s23 = new HashSet<RowKey>();
        m_s23.add(m_c2);
        assertTrue(m_s23.contains(m_c2));
        m_s23.add(m_c3);
        assertTrue(m_s23.contains(m_c3));
        assertTrue(m_s23.size() == 2);
    }
    
    /**
     * Destroy internal members.
     * @throws Exception If setup failed.
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        m_s12.clear();
        m_s12 = null;
        m_s23.clear();
        m_s23 = null;
        m_c1 = null;
        m_c2 = null;
        m_c3 = null;
    }
    
    /**
     * Test adding and removing hilite listener.
     */
    public void test1() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l1);

        m_hdl.addHiLiteListener(m_l2);
        m_hdl.addHiLiteListener(m_l2);
        
        m_hdl.removeHiLiteListener(m_l1);
        m_hdl.removeHiLiteListener(m_l1);
        
        m_hdl.removeHiLiteListener(m_l2);
        m_hdl.removeHiLiteListener(m_l2);

        m_hdl.addHiLiteListener(null);
        m_hdl.removeHiLiteListener(null);

        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        m_hdl.removeAllHiLiteListeners();
    }
    
    /**
     * Test exceptions during hilite and unhilite. 
     */
    public void test3() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        try {
            m_hdl.fireHiLiteEvent((RowKey) null);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        try {
            m_hdl.fireHiLiteEvent((Set<RowKey>) null);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }

        try {
            m_hdl.fireUnHiLiteEvent((RowKey) null);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        try {
            m_hdl.fireUnHiLiteEvent((Set<RowKey>) null);
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        try {
            assertFalse(m_hdl.isHiLit((RowKey) null));
            fail();
        } catch (NullPointerException e) {
            assertTrue(true);
        }
        m_hdl.removeAllHiLiteListeners();
    }
    
    /**
     * Test hiliting and unhiliting for data cells. 
     */
    public void test4() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        // ensure c1 and c2 are not hilit
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        
        // hilite c1
        m_hdl.fireHiLiteEvent(m_c1);
        assertTrue(m_hdl.isHiLit(m_c1));
        m_hdl.fireHiLiteEvent(m_c1);
        assertTrue(m_hdl.isHiLit(m_c1));
        
        // hilite c2
        m_hdl.fireHiLiteEvent(m_c2);
        assertTrue(m_hdl.isHiLit(m_c2));
        m_hdl.fireHiLiteEvent(m_c2);
        assertTrue(m_hdl.isHiLit(m_c2));

        // unhilite c1
        m_hdl.fireUnHiLiteEvent(m_c1);
        assertFalse(m_hdl.isHiLit(m_c1));
        m_hdl.fireUnHiLiteEvent(m_c1);
        assertTrue(true);
        assertFalse(m_hdl.isHiLit(m_c1));

        // unhilite c2
        m_hdl.fireClearHiLiteEvent();
        assertFalse(m_hdl.isHiLit(m_c2));
        m_hdl.fireUnHiLiteEvent(m_c2);
        assertFalse(m_hdl.isHiLit(m_c2));
        
        m_hdl.removeAllHiLiteListeners();
    }
    
    /**
     * Tests exceptions for hiliting and unhiliting data cells.
     */
    public void test5() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        // ensure c1 and c2 are not hilit
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        
        // hilite c1
        m_hdl.fireHiLiteEvent(m_c1);
        assertTrue(m_hdl.isHiLit(m_c1));
        
        // hilite c2
        m_hdl.fireHiLiteEvent(m_c2);
        assertTrue(m_hdl.isHiLit(m_c2));
        
        // unhilite c1
        m_hdl.fireUnHiLiteEvent(m_c1);
        assertFalse(m_hdl.isHiLit(m_c1));
        m_hdl.fireUnHiLiteEvent(m_c1);
        
        // unhilite all
        m_hdl.fireClearHiLiteEvent();
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
    }
    
    /**
     * Tests hiliting and unhiliting for data cell sets.
     */
    public void test6() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        // ensure c1, c2, and c3 are not hilit
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertFalse(m_hdl.isHiLit(m_c3));
        
        // hilite s1
        m_hdl.fireHiLiteEvent(m_s12);
        assertTrue(m_hdl.isHiLit(m_c1));
        assertTrue(m_hdl.isHiLit(m_c2));
        m_hdl.fireHiLiteEvent(m_s12);
        
        // hilite c3
        assertFalse(m_hdl.isHiLit(m_c3));
        m_hdl.fireHiLiteEvent(m_c3);
        assertTrue(m_hdl.isHiLit(m_c3));
        
        // hilite s2
        m_hdl.fireHiLiteEvent(m_s23);
        assertTrue(m_hdl.isHiLit(m_c1, m_c2, m_c3));
        
        // unhilite s1
        m_hdl.fireUnHiLiteEvent(m_s12);
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertTrue(m_hdl.isHiLit(m_c3));
        m_hdl.fireUnHiLiteEvent(m_c1);
        
        // unhilite c3
        assertTrue(m_hdl.isHiLit(m_c3));
        m_hdl.fireUnHiLiteEvent(m_c3);
        assertFalse(m_hdl.isHiLit(m_c3));
        
        // unhilite s2
        m_hdl.fireUnHiLiteEvent(m_s23);
    }
    
    /**
     * Tests exceptions for hiliting and unhiliting data cell sets.
     */
    public void test7() {
        m_hdl.addHiLiteListener(m_l1);
        m_hdl.addHiLiteListener(m_l2);
        // ensure c1, c2, and c3 are not hilit
        assertFalse(m_hdl.isHiLit(m_c1, m_c2, m_c3));
        
        // hilite s1
        m_hdl.fireHiLiteEvent(m_s12);
        assertTrue(m_hdl.isHiLit(m_c1, m_c2));
        assertFalse(m_hdl.isHiLit(m_c3));
        
        // hilite c2
        m_hdl.fireHiLiteEvent(m_c2);
        
        // hilite s2
        m_hdl.fireHiLiteEvent(m_s23);
        assertTrue(m_hdl.isHiLit(m_c1, m_c2, m_c3));
        
        // unhilite s1
        m_hdl.fireUnHiLiteEvent(m_s12);
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertTrue(m_hdl.isHiLit(m_c3));
        m_hdl.fireUnHiLiteEvent(m_c1);

        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertTrue(m_hdl.isHiLit(m_c3));
        
        // unhilite c3
        m_hdl.fireUnHiLiteEvent(m_c3);
        assertFalse(m_hdl.isHiLit(m_c3));
        m_hdl.fireUnHiLiteEvent(m_c2);
        assertFalse(m_hdl.isHiLit(m_c3));
        
        // unhilite s2
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertFalse(m_hdl.isHiLit(m_c3));
        m_hdl.fireClearHiLiteEvent();
        assertFalse(m_hdl.isHiLit(m_c1));
        assertFalse(m_hdl.isHiLit(m_c2));
        assertFalse(m_hdl.isHiLit(m_c3));
    }
    
    /**
     * System entry point.
     * @param args Parameters from command line: ignored.
     */
    public static void main(final String[] args) {
        junit.textui.TestRunner.run(DefaultHiLiteHandlerTest.class);
    }

}   // HiLiteHandlerTest
