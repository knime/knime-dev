/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   27.04.2010 (hofer): created
 */
package org.knime.base.node.mine.regression.pmmlgreg;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.xml.sax.SAXException;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

/**
 *
 * @author hofer, University of Konstanz
 */
public class PMMLGeneralRegressionPortObjectTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionPortObject#loadFrom(org.knime.core.node.port.pmml.PMMLPortObjectSpec, java.io.InputStream, java.lang.String)}.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    @Test
    public final void testLoadFrom() throws ParserConfigurationException,
        SAXException, IOException {
        PMMLGeneralRegressionPortObject port = readPMMLModel();
        // TODO Test if result is correct
        port.getTargetVariableName();
    }

    /**
     * Test method for {@link org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionPortObject#writePMMLModel(javax.xml.transform.sax.TransformerHandler)}.
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws TransformerConfigurationException
     */
    @Test
    public final void testWritePMMLModel() throws ParserConfigurationException,
    SAXException, IOException, TransformerConfigurationException {
        PMMLGeneralRegressionPortObject port = readPMMLModel();
        ByteOutputStream os = new ByteOutputStream();
        port.save(os);
        String saved = new String(os.getBytes(), "UTF-8");
        PMMLGeneralRegressionPortObject portSaved =
            new PMMLGeneralRegressionPortObject();
        PMMLPortObjectSpec spec = getPMMLSpec();
        InputStream stream = new ByteArrayInputStream(
                saved.getBytes("UTF-8"));
        portSaved.loadFrom(spec, stream, PMMLPortObject.PMML_V3_1);

        Assert.assertEquals(port.getContent().getModelType(),
                portSaved.getContent().getModelType());
        Assert.assertEquals(port.getContent().getFunctionName(),
                portSaved.getContent().getFunctionName());
    }

    private PMMLGeneralRegressionPortObject readPMMLModel()
        throws ParserConfigurationException, SAXException, IOException {
        PMMLGeneralRegressionPortObject port =
            new PMMLGeneralRegressionPortObject();
        PMMLPortObjectSpec spec = getPMMLSpec();
        InputStream stream = new ByteArrayInputStream(
                getPMMLFile().getBytes("UTF-8"));
        port.loadFrom(spec, stream, PMMLPortObject.PMML_V3_1);
        return port;
    }


    private PMMLPortObjectSpec getPMMLSpec() {
        String[] names = new String[]{"jobcat", "minority", "sex", "age",
                "work"};
        DataType[] types = new DataType[]{DoubleCell.TYPE, DoubleCell.TYPE,
                DoubleCell.TYPE, DoubleCell.TYPE, DoubleCell.TYPE};
        DataTableSpec tableSpec = new DataTableSpec(names, types);
        PMMLPortObjectSpecCreator specCreator =
            new PMMLPortObjectSpecCreator(tableSpec);
        return specCreator.createSpec();
    }

    private String getPMMLFile() {
        return "<?xml version=\"1.0\" ?>"
        + "<PMML version=\"3.1\" xmlns=\"http://www.dmg.org/PMML-3_1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
        + "  <Header copyright=\"dmg.org\"/>"
        + "  <DataDictionary numberOfFields=\"5\">"
        + "    <DataField name=\"jobcat\" optype=\"continuous\" dataType=\"double\"/>"
        + "    <DataField name=\"minority\" optype=\"continuous\" dataType=\"double\"/>"
        + "    <DataField name=\"sex\" optype=\"continuous\" dataType=\"double\"/>"
        + "    <DataField name=\"age\" optype=\"continuous\" dataType=\"double\"/>"
        + "    <DataField name=\"work\" optype=\"continuous\" dataType=\"double\"/>"
        + "  </DataDictionary>"
        + ""
        + "  <GeneralRegressionModel"
        + "        targetVariableName=\"jobcat\""
        + "        modelType=\"multinomialLogistic\""
        + "        functionName=\"classification\">"
        + ""
        + "    <MiningSchema>"
        + "      <MiningField name=\"jobcat\" usageType=\"predicted\"/>"
        + "      <MiningField name=\"minority\" usageType=\"active\"/>"
        + "      <MiningField name=\"sex\" usageType=\"active\"/>"
        + "      <MiningField name=\"age\" usageType=\"active\"/>"
        + "      <MiningField name=\"work\" usageType=\"active\"/>"
        + "    </MiningSchema>"
        + ""
        + "    <ParameterList>"
        + "      <Parameter name=\"p0\" label=\"Intercept\"/>"
        + "      <Parameter name=\"p1\" label=\"[SEX=0]\"/>"
        + "      <Parameter name=\"p2\" label=\"[SEX=1]\"/>"
        + "      <Parameter name=\"p3\" label=\"[MINORITY=0]([SEX=0])\"/>"
        + "      <Parameter name=\"p4\" label=\"[MINORITY=1]([SEX=0])\"/>"
        + "      <Parameter name=\"p5\" label=\"[MINORITY=0]([SEX=1])\"/>"
        + "      <Parameter name=\"p6\" label=\"[MINORITY=1]([SEX=1])\"/>"
        + "      <Parameter name=\"p7\" label=\"age\"/>"
        + "      <Parameter name=\"p8\" label=\"work\"/>"
        + "    </ParameterList>"
        + ""
        + "    <FactorList>"
        + "      <Predictor name=\"sex\" />"
        + "      <Predictor name=\"minority\" />"
        + "    </FactorList>"
        + ""
        + "    <CovariateList>"
        + "      <Predictor name=\"age\" />"
        + "      <Predictor name=\"work\" />"
        + "    </CovariateList>"
        + ""
        + "    <PPMatrix>"
        + "      <PPCell value=\"0\" predictorName=\"sex\" parameterName=\"p1\"/>"
        + "      <PPCell value=\"1\" predictorName=\"sex\" parameterName=\"p2\"/>"
        + "      <PPCell value=\"0\" predictorName=\"sex\" parameterName=\"p3\"/>"
        + "      <PPCell value=\"0\" predictorName=\"sex\" parameterName=\"p4\"/>"
        + "      <PPCell value=\"1\" predictorName=\"sex\" parameterName=\"p5\"/>"
        + "      <PPCell value=\"1\" predictorName=\"sex\" parameterName=\"p6\"/>"
        + "      <PPCell value=\"0\" predictorName=\"minority\" parameterName=\"p3\"/>"
        + "      <PPCell value=\"1\" predictorName=\"minority\" parameterName=\"p4\"/>"
        + "      <PPCell value=\"0\" predictorName=\"minority\" parameterName=\"p5\"/>"
        + "      <PPCell value=\"1\" predictorName=\"minority\" parameterName=\"p6\"/>"
        + "      <PPCell value=\"1\" predictorName=\"age\" parameterName=\"p7\"/>"
        + "      <PPCell value=\"1\" predictorName=\"work\" parameterName=\"p8\"/>"
        + "    </PPMatrix>"
        + ""
        + "    <ParamMatrix>"
        + "      <PCell targetCategory=\"1\" parameterName=\"p0\" beta=\"26.836\" df=\"1\"/>"
        + "      <PCell targetCategory=\"1\" parameterName=\"p1\" beta=\"-.719\" df=\"1\"/>"
        + "      <PCell targetCategory=\"1\" parameterName=\"p3\" beta=\"-19.214\" df=\"1\"/>"
        + "      <PCell targetCategory=\"1\" parameterName=\"p5\" beta=\"-.114\" df=\"1\"/>"
        + "      <PCell targetCategory=\"1\" parameterName=\"p7\" beta=\"-.133\" df=\"1\"/>"
        + "      <PCell targetCategory=\"1\" parameterName=\"p8\" beta=\"7.885E-02\" df=\"1\"/>"
        + "      <PCell targetCategory=\"2\" parameterName=\"p0\" beta=\"31.077\" df=\"1\"/>"
        + "      <PCell targetCategory=\"2\" parameterName=\"p1\" beta=\"-.869\" df=\"1\"/>"
        + "      <PCell targetCategory=\"2\" parameterName=\"p3\" beta=\"-18.99\" df=\"1\"/>"
        + "      <PCell targetCategory=\"2\" parameterName=\"p5\" beta=\"1.01\" df=\"1\"/>"
        + "      <PCell targetCategory=\"2\" parameterName=\"p7\" beta=\"-.3\" df=\"1\"/>"
        + "      <PCell targetCategory=\"2\" parameterName=\"p8\" beta=\".152\" df=\"1\"/>"
        + "      <PCell targetCategory=\"3\" parameterName=\"p0\" beta=\"6.836\" df=\"1\"/>"
        + "      <PCell targetCategory=\"3\" parameterName=\"p1\" beta=\"16.305\" df=\"1\"/>"
        + "      <PCell targetCategory=\"3\" parameterName=\"p3\" beta=\"-20.041\" df=\"1\"/>"
        + "      <PCell targetCategory=\"3\" parameterName=\"p5\" beta=\"-.73\" df=\"1\"/>"
        + "      <PCell targetCategory=\"3\" parameterName=\"p7\" beta=\"-.156\" df=\"1\"/>"
        + "      <PCell targetCategory=\"3\" parameterName=\"p8\" beta=\".267\" df=\"1\"/>"
        + "      <PCell targetCategory=\"4\" parameterName=\"p0\" beta=\"8.816\" df=\"1\"/>"
        + "      <PCell targetCategory=\"4\" parameterName=\"p1\" beta=\"15.264\" df=\"1\"/>"
        + "      <PCell targetCategory=\"4\" parameterName=\"p3\" beta=\"-16.799\" df=\"1\"/>"
        + "      <PCell targetCategory=\"4\" parameterName=\"p5\" beta=\"16.48\" df=\"1\"/>"
        + "      <PCell targetCategory=\"4\" parameterName=\"p7\" beta=\"-.133\" df=\"1\"/>"
        + "      <PCell targetCategory=\"4\" parameterName=\"p8\" beta=\"-.16\" df=\"1\"/>"
        + "      <PCell targetCategory=\"5\" parameterName=\"p0\" beta=\"5.862\" df=\"1\"/>"
        + "      <PCell targetCategory=\"5\" parameterName=\"p1\" beta=\"16.437\" df=\"1\"/>"
        + "      <PCell targetCategory=\"5\" parameterName=\"p3\" beta=\"-17.309\" df=\"1\"/>"
        + "      <PCell targetCategory=\"5\" parameterName=\"p5\" beta=\"15.888\" df=\"1\"/>"
        + "      <PCell targetCategory=\"5\" parameterName=\"p7\" beta=\"-.105\" df=\"1\"/>"
        + "      <PCell targetCategory=\"5\" parameterName=\"p8\" beta=\"6.914E-02\" df=\"1\"/>"
        + "      <PCell targetCategory=\"6\" parameterName=\"p0\" beta=\"6.495\" df=\"1\"/>"
        + "      <PCell targetCategory=\"6\" parameterName=\"p1\" beta=\"17.297\" df=\"1\"/>"
        + "      <PCell targetCategory=\"6\" parameterName=\"p3\" beta=\"-19.098\" df=\"1\"/>"
        + "      <PCell targetCategory=\"6\" parameterName=\"p5\" beta=\"16.841\" df=\"1\"/>"
        + "      <PCell targetCategory=\"6\" parameterName=\"p7\" beta=\"-.141\" df=\"1\"/>"
        + "      <PCell targetCategory=\"6\" parameterName=\"p8\" beta=\"-5.058E-02\" df=\"1\"/>"
        + "    </ParamMatrix>"
        + "  "
        + "  </GeneralRegressionModel>"
        + ""
        + "</PMML>";

    }



}
