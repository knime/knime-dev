/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   12.08.2013 (thor): created
 */
package org.knime.testing.core;

import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModel;

/**
 * Interface used by the difference checker node. Subclasses can implement more relaxed difference checks than pure
 * 1:1 equality.
 *
 * @param <T> value type that this checker handles
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public interface DifferenceChecker<T extends DataValue> {
    /**
     * Checks whether the two values are "equal" in the sense of this checker.
     *
     * @param valueA the first value
     * @param valueB the second value
     * @return <code>true</code> if both value are considered equal, <code>false</code> otherwise
     */
    boolean check(T valueA, T valueB);

    /**
     * Returns a (possibly empty) list of settings with which this checker can be configured.
     *
     * @return a list with {@link SettingsModel}s, never <code>null</code>
     */
    List<? extends SettingsModel> getSettings();


    /**
     * Returns a (possibly empty) list of dialog components for the settings returned by {@link #getSettings()}. The
     * number and order of the settings and the dialog components must match.
     *
     * @return a list with {@link DialogComponent}s, never <code>null</code>
     */
    List<? extends DialogComponent> getDialogComponents();

    /**
     * Returns a short description for this checker.
     *
     * @return a description, never <code>null</code>
     */
    String getDescription();
}
