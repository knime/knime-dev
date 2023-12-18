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
 *   Dec 18, 2023 (wiswedel): created
 */
package org.knime.testing.node.filestore.create.port;

import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeFactory;
import org.knime.testing.data.filestore.LargeFileStorePortObject;

/**
 *
 * @author wiswedel
 */
public final class FileStorePortCreateNodeFactory extends WebUINodeFactory<FileStorePortCreateNodeModel> {

    @SuppressWarnings("restriction")
    private static final WebUINodeConfiguration CONFIGURATION = WebUINodeConfiguration.builder() //
        .name("Create FileStore Port") //
        .icon(null) //
        .shortDescription("Creates a port object hosting one ore multiple file stores.") //
        .fullDescription("""
                The node produces a port object containing one or two file store object. This node is used in
                automatic workflow tests. It is of no use in any production workflow.
                    """) //
        .modelSettingsClass(FileStorePortCreateNodeSettings.class) //
        .addOutputPort("Port Object", LargeFileStorePortObject.TYPE, "Test Port Object backed by FileStores") //
        .nodeType(NodeType.Source) //
        .build();

    public FileStorePortCreateNodeFactory() {
        super(CONFIGURATION);
    }


    @Override
    public FileStorePortCreateNodeModel createNodeModel() {
        return new FileStorePortCreateNodeModel(CONFIGURATION);
    }

}
