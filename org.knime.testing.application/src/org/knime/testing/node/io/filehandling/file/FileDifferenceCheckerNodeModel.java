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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import org.apache.commons.io.IOUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.status.NodeModelStatusConsumer;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;

/**
 * The file difference checker node model.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class FileDifferenceCheckerNodeModel extends NodeModel {

    private final FileDifferenceCheckerConfiguration m_config;

    private final NodeModelStatusConsumer m_statusConsumer;

    FileDifferenceCheckerNodeModel(final PortsConfiguration portsConfig,
        final FileDifferenceCheckerConfiguration config) {
        super(portsConfig.getInputPorts(), portsConfig.getOutputPorts());
        m_config = config;
        m_statusConsumer = new NodeModelStatusConsumer(EnumSet.of(MessageType.ERROR, MessageType.WARNING));
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        m_config.getFileChooser().configureInModel(inSpecs, m_statusConsumer);
        m_config.getReferenceFileChooser().configureInModel(inSpecs, m_statusConsumer);
        m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
        return new PortObjectSpec[]{};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        try (final ReadPathAccessor referenceAccessor = m_config.getReferenceFileChooser().createReadPathAccessor();
                final ReadPathAccessor fileAccessor = m_config.getFileChooser().createReadPathAccessor()) {
            final Path path = fileAccessor.getRootPath(m_statusConsumer);
            CheckUtils.checkArgument(Files.readAttributes(path, BasicFileAttributes.class).isRegularFile(),
                "The file to be compared is not a regular file");

            final Path refPath = referenceAccessor.getRootPath(m_statusConsumer);
            CheckUtils.checkArgument(Files.readAttributes(refPath, BasicFileAttributes.class).isRegularFile(),
                "The reference file is not a regular file");

            m_statusConsumer.setWarningsIfRequired(this::setWarningMessage);
            try (final InputStream pI = Files.newInputStream(path);
                    final InputStream rI = Files.newInputStream(refPath)) {
                if (!IOUtils.contentEquals(pI, rI)) {
                    throw new IllegalStateException("The files are different");
                }
            }
        }
        return new PortObject[]{};
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_config.saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.validateForModel(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_config.loadSettingsForModel(settings);
    }

    @Override
    protected void reset() {
        // nothing to reset
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to load
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to save
    }
}
