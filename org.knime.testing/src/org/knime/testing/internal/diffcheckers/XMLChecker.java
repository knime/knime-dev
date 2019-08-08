/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   04.02.2016 (thor): created
 */
package org.knime.testing.internal.diffcheckers;

import org.knime.core.data.util.LockedSupplier;
import org.knime.core.data.xml.XMLValue;
import org.knime.core.data.xml.util.XmlDomComparer;
import org.knime.core.data.xml.util.XmlDomComparer.Diff;
import org.knime.testing.core.AbstractDifferenceChecker;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;
import org.w3c.dom.Document;

/**
 * Checker if two XML documents are equal and outputs a detailed message where they differ otherwise.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class XMLChecker extends AbstractDifferenceChecker<XMLValue> {
    /**
     * Factory for the {@link XMLChecker}.
     */
    public static class Factory implements DifferenceCheckerFactory<XMLValue> {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<XMLValue> getType() {
            return XMLValue.class;
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
        public DifferenceChecker<XMLValue> newChecker() {
            return new XMLChecker();
        }
    }

    static final String DESCRIPTION = "XML";

    /**
     * {@inheritDoc}
     */
    @Override
    public Result check(final XMLValue expected, final XMLValue got) {
        try (LockedSupplier<Document> expSupplier = expected.getDocumentSupplier();
                LockedSupplier<Document> gotSupplier = got.getDocumentSupplier()) {
            Diff result = XmlDomComparer.compareNodes(expSupplier.get(), gotSupplier.get());

            if (result != null) {
                return new Result(result.toString() + " [expected document: '" + expected.toString() + "', actual '"
                    + got.toString() + "']");
            } else {
                return OK;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
