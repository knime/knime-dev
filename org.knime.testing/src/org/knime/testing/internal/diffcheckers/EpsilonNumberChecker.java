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
 *   13.08.2013 (thor): created
 */
package org.knime.testing.internal.diffcheckers;

import java.util.Collections;
import java.util.List;

import org.knime.core.data.DoubleValue;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;

/**
 * Checker for numerical values which allows a configurable epsilon-deviation.
 *
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.9
 */
public class EpsilonNumberChecker implements DifferenceChecker<DoubleValue> {
    /**
     * Factory for the {@link EpsilonNumberChecker}.
     */
    public static class Factory implements DifferenceCheckerFactory<DoubleValue> {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<DoubleValue> getType() {
            return DoubleValue.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DifferenceChecker<DoubleValue> newChecker() {
            return new EpsilonNumberChecker();
        }
    }

    static final String DESCRIPTION = "Numbers with epsilon";

    private final SettingsModelDouble m_epsilon = new SettingsModelDouble("epsilon", 0.01);
    private final DialogComponentNumber m_component = new DialogComponentNumber(m_epsilon, "Epsilon", 0.001);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean check(final DoubleValue valueA, final DoubleValue valueB) {
        double diff = Math.abs(valueA.getDoubleValue() - valueB.getDoubleValue());
        return diff <= m_epsilon.getDoubleValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends SettingsModel> getSettings() {
        return Collections.singletonList(m_epsilon);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends DialogComponent> getDialogComponents() {
        return Collections.singletonList(m_component);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
