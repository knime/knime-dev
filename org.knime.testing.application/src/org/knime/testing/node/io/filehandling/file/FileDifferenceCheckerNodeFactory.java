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
 *   Jun 19, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.testing.node.io.filehandling.file;

import java.util.Optional;

import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeView;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.filehandling.core.port.FileSystemPortObject;

/**
 * The file difference checker node factory.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class FileDifferenceCheckerNodeFactory extends ConfigurableNodeFactory<FileDifferenceCheckerNodeModel> {

    static final String COMPARISON_INPUT_PORT_GRP_NAME = "Comparision File System Connection";

    static final String REFERENCE_INPUT_PORT_GRP_NAME = "Reference File System Connection";

    @Override
    protected Optional<PortsConfigurationBuilder> createPortsConfigBuilder() {
        final PortsConfigurationBuilder builder = new PortsConfigurationBuilder();
        builder.addOptionalInputPortGroup(COMPARISON_INPUT_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        builder.addOptionalInputPortGroup(REFERENCE_INPUT_PORT_GRP_NAME, FileSystemPortObject.TYPE);
        return Optional.of(builder);
    }

    @Override
    protected FileDifferenceCheckerNodeModel createNodeModel(final NodeCreationConfiguration creationConfig) {
        return new FileDifferenceCheckerNodeModel(
            creationConfig.getPortConfig().orElseThrow(IllegalStateException::new),
            createFileDiffConfig(creationConfig));
    }

    @Override
    protected NodeDialogPane createNodeDialogPane(final NodeCreationConfiguration creationConfig) {
        return new FileDifferenceCheckerNodeDialog(createFileDiffConfig(creationConfig));
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<FileDifferenceCheckerNodeModel> createNodeView(final int viewIndex,
        final FileDifferenceCheckerNodeModel nodeModel) {
        return null;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    private static FileDifferenceCheckerConfiguration
        createFileDiffConfig(final NodeCreationConfiguration creationConfig) {
        return new FileDifferenceCheckerConfiguration(
            creationConfig.getPortConfig().orElseThrow(IllegalStateException::new), COMPARISON_INPUT_PORT_GRP_NAME,
            REFERENCE_INPUT_PORT_GRP_NAME);
    }

}
