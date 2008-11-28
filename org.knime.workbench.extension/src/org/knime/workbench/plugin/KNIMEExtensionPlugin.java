/*
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package org.knime.workbench.plugin;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class KNIMEExtensionPlugin extends AbstractUIPlugin {

    // The shared instance.
    private static KNIMEExtensionPlugin plugin;

    /** Plugin ID as defined in plugin XML. */
    public static final String ID = "org.knime.workbench.extension";

    /**
     * The constructor.
     */
    public KNIMEExtensionPlugin() {
        plugin = this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * @return the shared instance
     */
    public static KNIMEExtensionPlugin getDefault() {
        return plugin;
    }

}
