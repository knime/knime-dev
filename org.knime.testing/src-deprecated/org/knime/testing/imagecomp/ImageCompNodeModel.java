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
 * ------------------------------------------------------------------------
 *
 * History
 *   28.10.2010 (gabriel): created
 */
package org.knime.testing.imagecomp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.knime.base.data.xml.SvgCell;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.image.png.PNGImageContent;
import org.knime.core.data.image.png.PNGImageValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.image.ImagePortObject;
import org.knime.testing.internal.nodes.image.ImageDifferNodeFactory;

/**
 * Model to compare two images.
 *
 * @author Iris Adae, University of Konstanz
 * @deprecated use the new image comparator {@link ImageDifferNodeFactory} instead
 */
@Deprecated
public class ImageCompNodeModel extends NodeModel {

    private final SettingsModelDoubleBounded m_allowance = ImageCompNodeDialog.getAllowanceModel();

    /**
     * New node model with on image port input and a data table output.
     */
    public ImageCompNodeModel() {
        super(new PortType[]{ImagePortObject.TYPE, ImagePortObject.TYPE}, new PortType[]{});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new DataTableSpec[]{};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        ImagePortObject ipo1 = (ImagePortObject)inObjects[0];
        ImagePortObject ipo2 = (ImagePortObject)inObjects[1];
        if (!ipo1.getSpec().getDataType().equals(ipo2.getSpec().getDataType())) {
            throw new IOException("Image types don't match: " + ipo1.getSpec().getDataType() + " vs. "
                    + ipo2.getSpec().getDataType());
        }
        // compare PNGs
        DataCell cell1 = ipo1.toDataCell();
        DataCell cell2 = ipo2.toDataCell();
        if (cell1.getType().isASuperTypeOf(PNGImageContent.TYPE)) {

            comparePNGs((PNGImageValue)cell1, (PNGImageValue)cell2, m_allowance.getDoubleValue() * 0.01);

        } else if (cell1.getType().isASuperTypeOf(SvgCell.TYPE)) {
            // compare SVGs
            boolean t = ((SvgCell)cell1).equals(cell2);
            if (!t) {
                throw new IOException("SVG images don't match");
            }
        } else {
            throw new IOException("Currently only SVG's and PNG's are supported!");
        }

        return new PortObject[]{};
    }

    /*
     * Compares two png images. If the image sizes and types match, an exception
     * will be thrown, if the image pixel-wise disagreement exceeds the
     * specified amount (allowedDisagreement - [0,1]).
     */
    private final void
            comparePNGs(final PNGImageValue val1, final PNGImageValue val2, final double allowedDisagreement)
                    throws IOException {

        // create awt images
        BufferedImage image1 = (BufferedImage)val1.getImageContent().getImage();
        BufferedImage image2 = (BufferedImage)val2.getImageContent().getImage();

        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            throw new RuntimeException("PNG image sizes doesn't match.");
        }

        if (image1.getType() != image2.getType()) {
            throw new RuntimeException("PNG image types doesn't match.");
        }

        int width = image1.getWidth();
        int height = image2.getHeight();

        // pixel-wise agreement
        int pixErrors = 0;
        int maxPixError = (int)Math.round(allowedDisagreement * width * height);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) {
                    pixErrors++;
                }
            }
        }

        if (pixErrors > maxPixError) {
            double pixPerc = pixErrors * 1.0 / (1.0 * width * height);
            throw new RuntimeException("PNG images doesn't match (more than " + (allowedDisagreement * 100)
                    + "% disagreement): " + pixPerc * 100 + "%");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // empty
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_allowance.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // new since 2.8.0
        //        m_allowance.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            m_allowance.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            // new since 2.8.0
            m_allowance.setDoubleValue(0.0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // empty
    }

}
