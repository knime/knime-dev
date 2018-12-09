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
package org.knime.testing.internal.diffcheckers;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.testing.core.AbstractDifferenceChecker;
import org.knime.testing.core.DifferenceChecker;
import org.knime.testing.core.DifferenceCheckerFactory;


/**
 * Checker for {@link ZonedDateTime} which allows to truncate time units.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @since 3.8
 */
public class ZonedDateTimeChecker extends AbstractDifferenceChecker<ZonedDateTimeValue> {

    private static final ChronoUnit DEFAULT_UNIT = ChronoUnit.MILLIS;

    /**
     * Factory for the {@link ZonedDateTimeChecker}.
     */
    public static class Factory implements DifferenceCheckerFactory<ZonedDateTimeValue> {
        /**
         * {@inheritDoc}
         */
        @Override
        public Class<ZonedDateTimeValue> getType() {
            return ZonedDateTimeValue.class;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDescription() {
            return DESCRIPTION;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DifferenceChecker<ZonedDateTimeValue> newChecker() {
            return new ZonedDateTimeChecker();
        }
    }

    static final String DESCRIPTION = "Truncated Zoned Date Time";

    private final SettingsModelString m_unit = new SettingsModelString("timeUnit", DEFAULT_UNIT.name());

    private final SettingsModelBoolean m_ignoreZone = new SettingsModelBoolean("ignoreZone", true);

    private DialogComponentStringSelection m_unitComponent;

    private DialogComponentBoolean m_ignoreZoneComponent;

    /**
     * {@inheritDoc}
     */
    @Override
    public Result check(final ZonedDateTimeValue expected, final ZonedDateTimeValue got) {
        final ZonedDateTime ev = expected.getZonedDateTime();
        final ZonedDateTime gv = got.getZonedDateTime();

        if (ev.equals(gv)) {
            return OK;
        } else {
            final ChronoUnit chronoUnit = ChronoUnit.valueOf(m_unit.getStringValue());
            final ZonedDateTime truncev = ev.truncatedTo(chronoUnit);
            final ZonedDateTime truncgv = gv.truncatedTo(chronoUnit);
            if (m_ignoreZone.getBooleanValue()) {
                if (truncev.toLocalDateTime().equals(truncgv.toLocalDateTime())
                        && truncev.getOffset().equals(truncgv.getOffset())) {
                    return OK;
                }
            } else if (truncev.equals(truncgv)) {
                return OK;
            }
            return new Result("expected " + truncev + ", got " + truncgv);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadSettings(settings);
        m_unit.loadSettingsFrom(settings);
        m_ignoreZone.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        super.loadSettingsForDialog(settings);
        try {
            m_unit.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_unit.setStringValue(DEFAULT_UNIT.name());
        }
        try {
            m_ignoreZone.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ex) {
            m_ignoreZone.setBooleanValue(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        m_unit.saveSettingsTo(settings);
        m_ignoreZone.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_unit.validateSettings(settings);
        m_ignoreZone.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends DialogComponent> getDialogComponents() {
        if (m_unitComponent == null) {
            List<String> names = Arrays.stream(ChronoUnit.values()).map((v) -> v.name()).collect(Collectors.toList());
            m_unitComponent = new DialogComponentStringSelection(m_unit, "Time unit to truncate: ", names);
        }
        if (m_ignoreZoneComponent == null) {
            m_ignoreZoneComponent = new DialogComponentBoolean(m_ignoreZone, "Ignore time-zone");
        }
        List<DialogComponent> l = new ArrayList<DialogComponent>();
        l.add(m_unitComponent);
        l.add(m_ignoreZoneComponent);
        l.addAll(super.getDialogComponents());
        return l;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    protected SettingsModelBoolean createColumnPropertiesModel() {
        final SettingsModelBoolean model = super.createColumnPropertiesModel();
        model.setBooleanValue(true);
        return model;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelBoolean createColumnElementNameModels() {
        final SettingsModelBoolean model = super.createColumnElementNameModels();
        model.setBooleanValue(true);
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected SettingsModelBoolean createIgnoreDomainModel() {
        final SettingsModelBoolean model = super.createIgnoreDomainModel();
        model.setBooleanValue(true);
        return model;
    }
}
