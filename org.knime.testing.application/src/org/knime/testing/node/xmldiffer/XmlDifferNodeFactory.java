/*
 * ------------------------------------------------------------------------
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
 */
package org.knime.testing.node.xmldiffer;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;

/**
 * Factory for the XmlDiffer node.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public final class XmlDifferNodeFactory extends WebUINodeFactory<XmlDifferNodeModel> {

    private static final WebUINodeConfiguration CONFIG = WebUINodeConfiguration.builder()//
        .name("XML Diff") //
        .icon("./icon.png") //
        .shortDescription("Computes differences between two XML columns.") //
        .fullDescription("""
                This node computes differences between XML documents.
                One document acts as a control to which the test document is compared to.

                The %s column classifies the severity of the difference. The default is <code>DIFFERENT</code>
                but <code>SIMILAR</code> will be returned for
                <ul>
                <li>CDATA and Text nodes with the same content</li>
                <li>DOCTYPE differences</li>
                <li>different xsi:schemaLocation and xsi:noNamspaceSchemaLocation</li>
                <li>different XML namespaces prefixes</li>
                <li>explicit/implicit status of attributes</li>
                <li>a different order of child nodes</li>
                <li>XML encoding</li>
                </ul>
                """.formatted(XmlDifferCellFactory.DiffColumn.SUMMARY.getName()))
        .modelSettingsClass(XmlDifferNodeSettings.class) //
        .addInputTable("Test table",
            "Should contain an XML column that contains the documents to be checked for differences.") //
        .addInputTable("Control table",
            "Should contain an XML column that contains the documents that provide the baseline.") //
        .addOutputTable("Output",
            "The test table with an appended column containing "
                + "collection cells that summarize the type and location of differences.") //
        .nodeType(NodeType.Manipulator) //
        .keywords("testing", "xml", "compare", "comparison", "difference", "checker") //
        .sinceVersion(5, 4, 0) //
        .build();

    /** Default constructor (required for instantiation) */
    public XmlDifferNodeFactory() {
        super(CONFIG);
    }

    @Override
    public XmlDifferNodeModel createNodeModel() {
        return new XmlDifferNodeModel(CONFIG);
    }
}
