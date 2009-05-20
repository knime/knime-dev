/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   20.05.2009 (Fabian Dill): created
 */
package org.knime.core.node.port.pmml;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.xml.sax.InputSource;

public class DataDictionaryTest extends TestCase {
    
    
    public void testValueParsingString() throws Exception {
        DataDictionaryContentHandler hdl = new DataDictionaryContentHandler();
        SAXParserFactory saxFac = SAXParserFactory.newInstance();
        SAXParser parser = saxFac.newSAXParser();
        parser.parse(new InputSource(this.getClass().getResourceAsStream(
                "datadict_string_values.txt")), hdl);
        DataColumnSpec colSpec = hdl.getDataTableSpec().getColumnSpec(0);
        // this should be true in any case
        assertTrue(colSpec != null);
        assertTrue(colSpec.getName().equals("PETALLEN"));
        assertTrue(colSpec.getType().isCompatible(StringValue.class));
        assertTrue(colSpec.getDomain().hasValues());
        assertTrue(colSpec.getDomain().getValues().contains(
                new StringCell("1")));
        assertTrue(colSpec.getDomain().getValues().contains(
                new StringCell("-11")));
        assertTrue(colSpec.getDomain().getValues().contains(
                new StringCell("54")));
    }
    
    public void testValueParsingNumberInterval() throws Exception { 
        try {
            // test to ensure that a nominal data field with an interval
            // will raise an exception
            DataDictionaryContentHandler hdl =
                    new DataDictionaryContentHandler();
            SAXParserFactory saxFac = SAXParserFactory.newInstance();
            SAXParser parser = saxFac.newSAXParser();
            parser.parse(new InputSource(this.getClass().getResourceAsStream(
                    "datadict_string_interval.txt")), hdl);
        } catch (NumberFormatException nfe) {
            assertTrue(true);
            return;
        }
        // there must be an exception -> if we reach this code here the
        // exception was not raised -> fail!
        assertTrue(false);
    }
    
    public void testValueParsingNumberValues() throws Exception {
        DataDictionaryContentHandler hdl = new DataDictionaryContentHandler();
        SAXParserFactory saxFac = SAXParserFactory.newInstance();
        SAXParser parser = saxFac.newSAXParser();
        parser.parse(new InputSource(this.getClass().getResourceAsStream(
                "datadict_values_number.txt")), hdl);
        DataColumnSpec colSpec = hdl.getDataTableSpec().getColumnSpec(0);
        // this should be true in any case
        assertTrue(colSpec != null);
        assertTrue(colSpec.getName().equals("PETALLEN"));
        assertTrue(colSpec.getType().isCompatible(DoubleValue.class));
        // now check whether lower and upper bound was correctly extracted from 
        // the values (14, 54, -14, 11)
        // LOWER BOUND
        assertTrue(colSpec.getDomain().hasLowerBound());
        // since "optype" is "continuous" the column type is "double"
        assertTrue(colSpec.getDomain().getLowerBound().getType().equals(DoubleCell.TYPE));
        double lowerBound = ((DoubleValue)colSpec.getDomain().getLowerBound()).getDoubleValue();
        assertTrue(lowerBound == -14);
        // UPPER BOUND
        assertTrue(colSpec.getDomain().hasUpperBound());
        // since "optype" is "continuous" the column type is "double"
        assertTrue(colSpec.getDomain().getUpperBound().getType().equals(DoubleCell.TYPE));
        double upperBound = ((DoubleValue)colSpec.getDomain().getUpperBound()).getDoubleValue();
        assertTrue(upperBound == 54);
    }
    
    public void testValueParsingNumberValuesContainingStrings()throws Exception {
        DataDictionaryContentHandler hdl = new DataDictionaryContentHandler();
        SAXParserFactory saxFac = SAXParserFactory.newInstance();
        SAXParser parser = saxFac.newSAXParser();
        parser.parse(new InputSource(this.getClass().getResourceAsStream(
                "datadict_values_number_error.txt")), hdl);
        DataColumnSpec colSpec = hdl.getDataTableSpec().getColumnSpec(0);
        // this should be true in any case
        assertTrue(colSpec != null);
        assertTrue(colSpec.getName().equals("PETALLEN"));
        assertTrue(colSpec.getType().isCompatible(DoubleValue.class));
        // now check whether lower and upper bound was correctly extracted from 
        // the values (14, 54, -14, 11)
        // LOWER BOUND
        assertTrue(colSpec.getDomain().hasLowerBound());
        // since "optype" is "continuous" the column type is "double"
        assertTrue(colSpec.getDomain().getLowerBound().getType().equals(DoubleCell.TYPE));
        double lowerBound = ((DoubleValue)colSpec.getDomain().getLowerBound()).getDoubleValue();
        assertTrue(lowerBound == -14);
        // UPPER BOUND
        assertTrue(colSpec.getDomain().hasUpperBound());
        // since "optype" is "continuous" the column type is "double"
        assertTrue(colSpec.getDomain().getUpperBound().getType().equals(DoubleCell.TYPE));
        double upperBound = ((DoubleValue)colSpec.getDomain().getUpperBound()).getDoubleValue();
        assertTrue(upperBound == 54);
        
    }
    
}
