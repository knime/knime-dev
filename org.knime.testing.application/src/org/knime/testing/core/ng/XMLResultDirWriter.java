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
 *
 * History
 *   19.08.2013 (thor): created
 */
package org.knime.testing.core.ng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Result writer that writes each test suite into its own file.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class XMLResultDirWriter extends AbstractXMLResultWriter {
    private final File m_rootDir;
    private final boolean m_outputToSeparateFile;

    /**
     * Creates a new result writer.
     *
     * @param dir the destination directory
     * @param outputToSeparateFile <code>true</code> if stdout/stderr should be sent to a separate file
     * @throws ParserConfigurationException if the document builder cannot be created
     * @throws TransformerConfigurationException if the serializer cannot be created
     */
    public XMLResultDirWriter(final File dir, final boolean outputToSeparateFile)
        throws ParserConfigurationException, TransformerConfigurationException {
        m_rootDir = dir;
        m_outputToSeparateFile = outputToSeparateFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addResult(final WorkflowTestResult result) throws TransformerException, IOException {
        Document doc = m_docBuilder.newDocument();
        Element testsuite = createTestsuiteElement(result, doc, !m_outputToSeparateFile);
        doc.appendChild(testsuite);

        File destFile = new File(m_rootDir, result.getSuite().getName() + ".xml");
        destFile = new File(destFile.getParentFile(), "TEST-" + destFile.getName());
        if (!destFile.getParentFile().isDirectory() && !destFile.getParentFile().mkdirs()) {
            throw new IOException("Could not created directory for result file: "
                    + destFile.getParentFile().getAbsolutePath());
        }

        Source source = new DOMSource(doc);
        Result res = new StreamResult(destFile);
        m_serializer.transform(source, res);

        if (m_outputToSeparateFile) {
            File outputFile = new File(m_rootDir, result.getSuite().getName() + "-output.txt");
            try (Writer out = new FileWriter(outputFile)) {
                out.write(result.getSystemOut());
            }
        }

        m_startTimes.clear();
        m_endTimes.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startSuites() {
        // we don't care
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endSuites() {
        // we don't care
    }
}
