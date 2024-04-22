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
 *   Mar 13, 2015 (wiswedel): created
 */
package org.knime.testing.node.credentialsvalidate;

import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.CredentialsProvider;
import org.knime.core.node.workflow.ICredentials;

/**
 * Config to node.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
final class CredentialsValidateNodeConfiguration {

    private String m_credentialsID;
    private String m_username;
    private String m_password;
    private String m_secondFactor;

    private boolean m_passwordExpectedToBeSet;
    private boolean m_secondFactorExpectedToBeSet;
    private boolean m_validateCredentialsAtLoad;

    /**
     * @return the credId
     */
    String getCredId() {
        return m_credentialsID;
    }

    /**
     * @param credId the credId to set
     */
    void setCredentialsID(final String credId) {
        m_credentialsID = credId;
    }

    /**
     * @return the username
     */
    String getUsername() {
        return m_username;
    }

    /**
     * @param username the username to set
     */
    void setUsername(final String username) {
        m_username = username;
    }

    /**
     * @return the password
     */
    String getPassword() {
        return m_password;
    }

    /**
     * @param password the password to set
     */
    void setPassword(final String password) {
        m_password = password;
    }

    /**
     * @return the secondFactor
     */
    String getSecondFactor() {
        return m_secondFactor;
    }

    /**
     * @param secondFactor the secondFactor to set
     */
    void setSecondFactor(final String secondFactor) {
        m_secondFactor = secondFactor;
    }

    /**
     * @return the passwordExpectedToBeSet
     */
    boolean isPasswordExpectedToBeSet() {
        return m_passwordExpectedToBeSet;
    }

    /**
     * @param passwordExpectedToBeSet the property
     */
    void setPasswordExpectedToBeSet(final boolean passwordExpectedToBeSet) {
        m_passwordExpectedToBeSet = passwordExpectedToBeSet;
    }

    /**
     * @return the secondFactorExpectedToBeSet
     */
    boolean isSecondFactorExpectedToBeSet() {
        return m_secondFactorExpectedToBeSet;
    }

    /**
     * @param secondFactorExpectedToBeSet the property
     */
    void setSecondFactorExpectedToBeSet(final boolean secondFactorExpectedToBeSet) {
        m_secondFactorExpectedToBeSet = secondFactorExpectedToBeSet;
    }

    /**
     * @return the validateCredentialsAtLoad
     */
    public boolean isValidateCredentialsAtLoad() {
        return m_validateCredentialsAtLoad;
    }

    /**
     * @param validateCredentialsAtLoad the validateCredentialsAtLoad to set
     */
    public void setValidateCredentialsAtLoad(final boolean validateCredentialsAtLoad) {
        m_validateCredentialsAtLoad = validateCredentialsAtLoad;
    }

    void verify(final CredentialsProvider credProvider) throws InvalidSettingsException {
        CheckUtils.checkSetting(credProvider.listNames().contains(m_credentialsID),
            "Invalid credentials ID '%s'", m_credentialsID);
        ICredentials iCredentials = credProvider.get(m_credentialsID);
        CheckUtils.checkSetting(Objects.equals(iCredentials.getLogin(), getUsername()),
            "Wrong user name, expected '%s' but got '%s'", getUsername(), iCredentials.getLogin());
        if (isPasswordExpectedToBeSet()) {
            CheckUtils.checkSetting(Objects.equals(iCredentials.getPassword(), getPassword()),
                "Wrong password, expected '%s' but got %s",
                getPassword(), iCredentials.getPassword() != null ? "something different" : "null");
        } else {
            CheckUtils.checkSetting(iCredentials.getPassword() == null,
                    "Password expected to be not set (null) but is not null");
        }
        if (isSecondFactorExpectedToBeSet()) {
            CheckUtils.checkSetting(
                Objects.equals(iCredentials.getSecondAuthenticationFactor().orElse(null), getSecondFactor()),
                "Wrong second factor, expected '%s' but got %s", getSecondFactor(),
                iCredentials.getSecondAuthenticationFactor() != null ? "something different" : "null");
        } else {
            CheckUtils.checkSetting(
                iCredentials.getSecondAuthenticationFactor() == null
                    || iCredentials.getSecondAuthenticationFactor().isEmpty(),
                "Second factor expected to be not set (null) but is %s",
                iCredentials.getSecondAuthenticationFactor().orElse(null));
        }
    }

    void saveSettings(final NodeSettingsWO s) {
        s.addString("credentialsID", m_credentialsID);
        s.addString("username", m_username);
        s.addString("password", m_password);
        s.addString("secondFactor", m_secondFactor);
        s.addBoolean("passwordExpectedToBeSet", m_passwordExpectedToBeSet);
        s.addBoolean("secondFactorExpectedToBeSet", m_secondFactorExpectedToBeSet);
        s.addBoolean("validateCredentialsAtLoad", m_validateCredentialsAtLoad);
    }

    CredentialsValidateNodeConfiguration loadSettingsInModel(final NodeSettingsRO s) throws InvalidSettingsException {
        m_credentialsID = s.getString("credentialsID");
        m_username = s.getString("username");
        m_password = s.getString("password");
        m_secondFactor = s.getString("secondFactor", null);
        m_passwordExpectedToBeSet = s.getBoolean("passwordExpectedToBeSet");
        m_secondFactorExpectedToBeSet = s.getBoolean("secondFactorExpectedToBeSet", false);
        m_validateCredentialsAtLoad = s.getBoolean("validateCredentialsAtLoad", true);
        return this;
    }

    void loadSettingsInDialog(final NodeSettingsRO s) {
        m_credentialsID = s.getString("credentialsID", "credentials-id");
        m_username = s.getString("username", "some-user-name");
        m_password = s.getString("password", "some-password");
        m_secondFactor = s.getString("secondFactor", "some-second-factor");
        m_passwordExpectedToBeSet = s.getBoolean("passwordExpectedToBeSet", false);
        m_secondFactorExpectedToBeSet = s.getBoolean("secondFactorExpectedToBeSet", false);
        m_validateCredentialsAtLoad = s.getBoolean("validateCredentialsAtLoad", true);
    }
}
