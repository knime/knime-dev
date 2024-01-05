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
 *   Jan 5, 2024 (hornm): created
 */
package org.knime.testing.node.reporting2;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.report.IReportPortObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Paul Bärnreuther
 */
public class ReportToTableNodeModel extends NodeModel {

    @SuppressWarnings("javadoc")
    protected ReportToTableNodeModel() {
        super(new PortType[]{IReportPortObject.TYPE}, new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new PortObjectSpec[]{createSpec()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final var container = exec.createDataContainer(createSpec());
        final var reportFragments = ((IReportPortObject)inObjects[0]).getReportFragments();
        final var mapper = new ObjectMapper();
        for (int i = 0; i < reportFragments.size(); i++) {
            final var fragment = reportFragments.get(i);
            container.addRowToTable(new DefaultRow(RowKey.createRowKey(Integer.toUnsignedLong(i)),
                toXMLCell(fragment.content()), JSONCellFactory.create(mapper.writeValueAsString(fragment.config()))));

        }
        container.close();
        return new BufferedDataTable[]{container.getTable()};
    }

    private static DataTableSpec createSpec() {
        return new DataTableSpec( //
            new DataColumnSpecCreator("content", XMLCell.TYPE).createSpec(), //
            new DataColumnSpecCreator("config", JSONCell.TYPE).createSpec());
    }

    private static DataCell toXMLCell(final String string) {
        final var document = new W3CDom().fromJsoup(Jsoup.parse(string));
        /**
         * Necessary to be able to test xpath occurrences within TableView Reports. Without this, any xPath leads to the
         * error "TransformerException: The context can not be null when the operation is context-dependent."
         */
        removeAllAttributes(document);
        return XMLCellFactory.create(document);
    }

    private static void removeAllAttributes(final Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element)node;
            final var attributes = ((Element)node).getAttributes();
            for (var i = 0; i < attributes.getLength(); i++) {
                element.removeAttribute(attributes.item(i).getNodeName());
            }
        }

        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            removeAllAttributes(node.getChildNodes().item(i));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        //

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        //

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        //

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        //

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        //

    }

}
