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
 * History
 *   Nov 27, 2008 (wiswedel): created
 */
package org.knime.testing.node.runtime;

import java.io.IOException;

import org.apache.xmlbeans.XmlException;
import org.knime.core.node.NoDescriptionProxy;
import org.knime.core.node.NodeDescription;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.xml.sax.SAXException;

/**
 * NodeFactory that creates a node whose implementation is defined as inline
 * code. It uses an abstract runtime node model.
 * @author wiswedel, University of Konstanz
 */
public class RuntimeNodeFactory extends NodeFactory<RuntimeNodeModel> {

    private final RuntimeNodeModel m_model;

    /**
     *
     */
    public RuntimeNodeFactory(final RuntimeNodeModel model) {
        m_model = model;
    }

    /** {@inheritDoc} */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public RuntimeNodeModel createNodeModel() {
        return m_model;
    }

    /** {@inheritDoc} */
    @Override
    public NodeView<RuntimeNodeModel> createNodeView(final int viewIndex,
            final RuntimeNodeModel nodeModel) {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    protected boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDescription createNodeDescription() throws SAXException, IOException, XmlException {
        return new NoDescriptionProxy(getClass());
    }
}
