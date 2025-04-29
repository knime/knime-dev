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

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.json.JSONCell;
import org.knime.core.data.json.JSONCellFactory;
import org.knime.core.data.v2.RowBuffer;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.BooleanWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringListWriteValue;
import org.knime.core.data.v2.value.ValueInterfaces.StringWriteValue;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.data.xml.XMLCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.extension.InvalidNodeFactoryExtensionException;
import org.knime.core.node.extension.NodeFactoryExtension;
import org.knime.core.node.extension.NodeFactoryExtensionManager;
import org.knime.core.node.extension.NodeSetFactoryExtension;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;
import org.knime.core.webui.node.impl.analytics.WebUIDialogDetailsUtil;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.collect.Streams;

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
        var messageBuilder = createMessageBuilder();
        var spec = createSpec(s);
        try (var tableContainer = exec.createRowContainer(spec);
                var writeCursor = tableContainer.createCursor()) {
            RowBuffer row = tableContainer.createRowBuffer();
            var i = 0;
            var n = 0;
            final var nodeFactoryExtensions = NodeFactoryExtensionManager.getInstance().getNodeFactoryExtensions();
            final var nodeFactorySetExtensions = NodeFactoryExtensionManager.getInstance().getNodeSetFactoryExtensions();
            List<SingleNode> allNodes = Streams.concat( //
                Streams.stream(nodeFactoryExtensions).map(StandardNode::new), //
                Streams.stream(nodeFactorySetExtensions).flatMap(
                    setExt -> setExt.getNodeFactoryIds().stream().map(id -> new NodeSetExtensionNode(setExt, id))))
                .collect(Collectors.toList());
            var size = allNodes.size();
            for (SingleNode singleNode : allNodes) {
                n += 1;
                NodeFactory<? extends NodeModel> f;
                try {
                    f = singleNode.getOrCreateFactory();
                } catch (InvalidSettingsException | InvalidNodeFactoryExtensionException e) {
                    getLogger()
                        .warn("Unable to instantiate node " + singleNode.getFallbackName() + ": " + e.getMessage(), e);
                    continue;
                }
                exec.setProgress(n / (double)size, String.format("Indexing \"%s\" (%d/%d)", f.getNodeName(), n, size));
                exec.checkCanceled();

                int col = 0;
                ((StringWriteValue)row.getWriteValue(col++)).setStringValue(f.getNodeName());
                ((StringWriteValue)row.getWriteValue(col++)).setStringValue(singleNode.getCategoryPath());
                ((StringWriteValue)row.getWriteValue(col++)).setStringValue(singleNode.getPlugInSymbolicName());
                ((BooleanWriteValue)row.getWriteValue(col++)).setBooleanValue(singleNode.isDeprecated());
                ((BooleanWriteValue)row.getWriteValue(col++)).setBooleanValue(singleNode.isHidden());
                if (s.m_includeNodeFactoryID) {
                    ((StringWriteValue)row.getWriteValue(col++)).setStringValue(singleNode.getFactoryId());
                }
                if (s.m_includeNodeDescription) {
                    DataCell cell;
                    try {
                        cell = toDocument(f.getXMLDescription());
                    } catch (Exception e) {
                        final var msg = String.format("Unable to extract node description for %s: %s",
                            singleNode.getFactoryId(), e.getMessage());
                        messageBuilder.addTextIssue(msg);
                        getLogger().debug(msg, e);
                        cell = null;
                    }
                    if (cell != null) {
                        ((WriteValue<DataValue>)row.getWriteValue(col++)).setValue(cell);
                    } else {
                        row.setMissing(col++);
                    }
                }
                if (s.m_includeKeywords) {
                    final var keywords = f.getKeywords();
                    if (keywords.length == 0) {
                        row.setMissing(col++);
                    } else {
                        ((StringListWriteValue)row.getWriteValue(col++)).setValue(keywords);
                    }
                }
                if (s.m_includeWebUIDialogDetails) {
                    final var webUIDialogStatistics = WebUIDialogDetailsUtil.extractWebUIStatistics(f);
                    ((BooleanWriteValue)row.getWriteValue(col++))
                        .setBooleanValue(webUIDialogStatistics.hasWebUIDialog());
                    if (!webUIDialogStatistics.hasWebUIModel()
                        && webUIDialogStatistics.hasWebUIModelMessage() != null) {
                        row.setMissing(col++);
                    } else {
                        ((BooleanWriteValue)row.getWriteValue(col++))
                            .setBooleanValue(webUIDialogStatistics.hasWebUIModel());

                    }
                    if (webUIDialogStatistics.modelSettings() == null) {
                        row.setMissing(col++);
                        row.setMissing(col++);
                    } else {
                        DataCell schema =
                            new JSONCellFactory().createCell(webUIDialogStatistics.modelSettings().schema());
                        DataCell uiSchema =
                            new JSONCellFactory().createCell(webUIDialogStatistics.modelSettings().uiSchema());
                        ((WriteValue<DataValue>)row.getWriteValue(col++)).setValue(schema);
                        ((WriteValue<DataValue>)row.getWriteValue(col++)).setValue(uiSchema);
                    }

                }
                row.setRowKey(RowKey.createRowKey((long)i++));
                writeCursor.commit(row);
            }
            if (messageBuilder.getIssueCount() == 1) {
                messageBuilder.withSummary(messageBuilder.getFirstIssue().orElseThrow());
            } else if (messageBuilder.getIssueCount() > 1) {
                messageBuilder.withSummary(
                    String.format("There are %d nodes for which the node descriptions can not be extracted.",
                        messageBuilder.getIssueCount()));
            }
            messageBuilder.build().ifPresent(this::setWarning);
            return new BufferedDataTable[]{tableContainer.finish()};
        }
    }

    private static DataTableSpec createSpec(final NodeListExtractorNodeSettings settings) {
        var creator = new DataTableSpecCreator() //
            .addColumns(new DataColumnSpecCreator("Node Name", StringCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("Category", StringCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("Plug-In", StringCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("Deprecated", BooleanCell.TYPE).createSpec()) //
            .addColumns(new DataColumnSpecCreator("Hidden", BooleanCell.TYPE).createSpec());
        if (settings.m_includeNodeFactoryID) {
            creator.addColumns(new DataColumnSpecCreator("Node Factory ID", StringCell.TYPE).createSpec());
        }
        if (settings.m_includeNodeDescription) {
            creator.addColumns(new DataColumnSpecCreator("Node Description", XMLCell.TYPE).createSpec());
        }
        if (settings.m_includeKeywords) {
            creator.addColumns(
                new DataColumnSpecCreator("Keywords", ListCell.getCollectionType(StringCell.TYPE)).createSpec());
        }
        if (settings.m_includeWebUIDialogDetails) {
            creator.addColumns(new DataColumnSpecCreator("Has Web UI Dialog", BooleanCell.TYPE).createSpec());
            creator.addColumns(new DataColumnSpecCreator("Has Web UI Model", BooleanCell.TYPE).createSpec());
            creator.addColumns(new DataColumnSpecCreator("Model Settings Schema", JSONCell.TYPE).createSpec());
            creator.addColumns(new DataColumnSpecCreator("Model Settings UI Schema", JSONCell.TYPE).createSpec());
        }
        return creator.createSpec();
    }

    private static DataCell toDocument(final Element xml) throws TransformerFactoryConfigurationError,
        TransformerException, IOException, ParserConfigurationException, SAXException, XMLStreamException {
        // this doesn't work:
        //      return XMLCellFactory.create(xml.getOwnerDocument());
        // because ... "DOM Level 3 Not implemented" (xmlbeans problem, apparently)
        DOMSource domSource = new DOMSource(xml);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        StreamResult sr = new StreamResult(sw);
        transformer.transform(domSource, sr);
        return XMLCellFactory.create(sw.toString());
    }

    interface SingleNode {

        String getFallbackName();
        String getNodeName();
        String getCategoryPath();
        String getPlugInSymbolicName();
        boolean isDeprecated();
        boolean isHidden();
        String getFactoryId();

        NodeFactory<? extends NodeModel> getOrCreateFactory()
            throws InvalidNodeFactoryExtensionException, InvalidSettingsException;
    }

    static final class StandardNode implements SingleNode {

        private final NodeFactoryExtension m_nodeFactoryExtension;
        private NodeFactory<? extends NodeModel> m_nodeFactory;

        StandardNode(final NodeFactoryExtension nodeFactoryExtension) {
            m_nodeFactoryExtension = nodeFactoryExtension;
        }

        @Override
        public String getFallbackName() {
            return m_nodeFactoryExtension.getFactoryClassName();
        }

        @Override
        public String getNodeName() {
            return m_nodeFactory.getNodeName();
        }

        @Override
        public String getCategoryPath() {
            return m_nodeFactoryExtension.getCategoryPath();
        }

        @Override
        public String getPlugInSymbolicName() {
            return m_nodeFactoryExtension.getPlugInSymbolicName();
        }

        @Override
        public boolean isDeprecated() {
            return m_nodeFactoryExtension.isDeprecated();
        }

        @Override
        public boolean isHidden() {
            return m_nodeFactoryExtension.isHidden();
        }

        @Override
        public String getFactoryId() {
            return m_nodeFactory.getFactoryId();
        }

        @Override
        public NodeFactory<? extends NodeModel> getOrCreateFactory() throws InvalidNodeFactoryExtensionException {
            if (m_nodeFactory == null) {
                m_nodeFactory = m_nodeFactoryExtension.getFactory();
            }
            return m_nodeFactory;
        }

        static Callable<SingleNode> factory(final NodeFactoryExtension ext) {
            return () -> new StandardNode(ext);
        }
    }


    private static final class NodeSetExtensionNode implements SingleNode {

        private final String m_id;
        private final NodeSetFactoryExtension m_nodeSetFactoryExtension;
        private NodeFactory<? extends NodeModel> m_nodeFactory;

        NodeSetExtensionNode(final NodeSetFactoryExtension nodeSetFactoryExtension, final String id) {
            m_id = id;
            m_nodeSetFactoryExtension = nodeSetFactoryExtension;
        }

        @Override
        public String getFallbackName() {
            return m_nodeSetFactoryExtension.getClass().getName() + "#" + m_id;
        }

        @Override
        public String getNodeName() {
            return m_nodeFactory.getNodeName();
        }

        @Override
        public String getCategoryPath() {
            return m_nodeSetFactoryExtension.getCategoryPath(m_id);
        }

        @Override
        public String getPlugInSymbolicName() {
            return m_nodeSetFactoryExtension.getPlugInSymbolicName();
        }

        @Override
        public boolean isDeprecated() {
            return m_nodeSetFactoryExtension.isDeprecated() || m_nodeFactory.isDeprecated();
        }

        @Override
        public boolean isHidden() {
            return false;
        }

        @Override
        public String getFactoryId() {
            return m_nodeFactory.getFactoryId();
        }

        @Override
        public NodeFactory<? extends NodeModel> getOrCreateFactory() throws InvalidSettingsException {
            if (m_nodeFactory == null) {
                m_nodeFactory = m_nodeSetFactoryExtension.createNodeFactory(m_id)
                        .orElseThrow(() -> new InvalidSettingsException("Unable to load " + m_id));
            }
            return m_nodeFactory;
        }

        static Callable<SingleNode> factory(final NodeSetFactoryExtension ext, final String id) {
            return () -> new NodeSetExtensionNode(ext, id);
        }

    }

}
