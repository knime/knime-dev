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
 *   Jan 26, 2023 (wiswedel): created
 */
package org.knime.testing.node.extractor;

import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.assertj.core.util.IterableUtil;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.v2.value.ValueInterfaces.BooleanWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringListWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringWriteValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.extension.NodeFactoryExtension;
import org.knime.core.node.extension.NodeFactoryExtensionManager;
import org.knime.core.webui.node.dialog.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.dialog.impl.WebUINodeModel;
import org.w3c.dom.Element;

/**
 *
 * @author wiswedel
 */
public class NodeListExtractorNodeModel extends WebUINodeModel<NodeListExtractorNodeSettings> {

    NodeListExtractorNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, NodeListExtractorNodeSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final NodeListExtractorNodeSettings s)
        throws InvalidSettingsException {
        return new DataTableSpec[] {createSpec(s)};
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final NodeListExtractorNodeSettings s) throws Exception {
        var spec = createSpec(s);
        try (var tableContainer = exec.createRowContainer(spec);
                var writeCursor = tableContainer.createCursor()) {
            var i = 0;
            var n = 0;
            final var nodeFactoryExtensions = NodeFactoryExtensionManager.getInstance().getNodeFactoryExtensions();
            var size = IterableUtil.sizeOf(nodeFactoryExtensions);
            for (NodeFactoryExtension ext : nodeFactoryExtensions) {
                n += 1;
                NodeFactory<? extends NodeModel> f;
                try {
                    f = ext.getFactory();
                } catch (Exception e) {
                    getLogger().warn("Unable to instantiate node " + ext.getFactoryClassName() + ": " + e.getMessage(),
                        e);
                    continue;
                }
                exec.setProgress(n / (double)size, String.format("Indexing \"%s\" (%d/%d)", f.getNodeName(), n, size));
                exec.checkCanceled();

                var rowWrite = writeCursor.forward();
                int col = 0;
                ((StringWriteValue)rowWrite.getWriteValue(col++)).setStringValue(f.getNodeName());
                ((StringWriteValue)rowWrite.getWriteValue(col++)).setStringValue(ext.getCategoryPath());
                ((StringWriteValue)rowWrite.getWriteValue(col++)).setStringValue(ext.getPlugInSymbolicName());
                ((BooleanWriteValue)rowWrite.getWriteValue(col++)).setBooleanValue(ext.isDeprecated());
                ((BooleanWriteValue)rowWrite.getWriteValue(col++)).setBooleanValue(ext.isHidden());
                if (s.m_includeNodeFactory) {
                    ((StringWriteValue)rowWrite.getWriteValue(col++)).setStringValue(ext.getFactoryClassName());
                }
                if (s.m_includeNodeDescription) {
                    ((StringWriteValue)rowWrite.getWriteValue(col++)).setStringValue(toDocument(f.getXMLDescription()));
                }
                if (s.m_includeKeywords) {
                    final var keywords = f.getKeywords();
                    if (keywords.length == 0) {
                        rowWrite.setMissing(col++);
                    } else {
                        ((StringListWriteValue)rowWrite.getWriteValue(col++)).setValue(keywords);
                    }
                }
                rowWrite.setRowKey(RowKey.createRowKey((long)i++));
            }
            return new BufferedDataTable[] {tableContainer.finish()};
        }
    }

    private static DataTableSpec createSpec(final NodeListExtractorNodeSettings settings) {
        var creator = new DataTableSpecCreator() //
            .addColumns(new DataColumnSpecCreator("Node Name", StringCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("Category", StringCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("Plug-In", StringCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("Deprecated", BooleanCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("Hidden", BooleanCell.TYPE).createSpec());
        if (settings.m_includeNodeFactory) {
            creator.addColumns(new DataColumnSpecCreator("Node Factory", StringCell.TYPE).createSpec());
        }
        if (settings.m_includeNodeDescription) {
            creator.addColumns(new DataColumnSpecCreator("Node Description", StringCell.TYPE).createSpec());
        }
        if (settings.m_includeKeywords) {
            creator.addColumns(new DataColumnSpecCreator("Keywords", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
        }
        return creator.createSpec();
    }

    private static String toDocument(final Element xml)
        throws TransformerFactoryConfigurationError, TransformerException {
        DOMSource domSource = new DOMSource(xml);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        StreamResult sr = new StreamResult(sw);
        transformer.transform(domSource, sr);
        return sw.toString();
    }

}