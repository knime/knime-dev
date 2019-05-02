/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by KNIME AG, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 */
package org.knime.testing.explorer;

import java.io.File;
import java.io.OutputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Utility methods for {@link LocalExplorerFileStore}s.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class LocalFileStoreTestUtils {

    private static WorkflowManager TMP_METANODE_ROOT;

    private LocalFileStoreTestUtils() {
        // utility class
    }

    /**
     * Creates an empty workflow in given filesystem location.
     *
     * @param parent the location in which the flow should be created
     * @param name the workflow's name
     * @return a workflow
     * @throws Exception if something goes wrong
     */
    @SuppressWarnings("unchecked")
    public static <T extends LocalExplorerFileStore> T createEmptyWorkflow(final T parent, final String name)
        throws Exception {
        T newWf = (T)parent.getChild(name);

        // create workflow dir
        newWf.mkdir(EFS.NONE, null);

        // create a new empty workflow file
        final T workflowFile = (T)newWf.getChild(WorkflowPersistor.WORKFLOW_FILE);
        try (OutputStream outStream = workflowFile.openOutputStream(EFS.NONE, null)) {
            //
        }
        return newWf;
    }

    /**
     * Creates a new workflow group inside the given location.
     *
     * @param parent the location in which the group should be created
     * @param name the group's name
     * @return a new workflow group
     * @throws Exception if something goes wrong
     */
    public static <T extends LocalExplorerFileStore> T createWorkflowGroup(final T parent, final String name)
        throws Exception {
        @SuppressWarnings("unchecked")
        T newGroup = (T)parent.getChild(name);
        newGroup.mkdir(EFS.NONE, null);

        // create a new empty meta info file
        File locFile = newGroup.toLocalFile(EFS.NONE, null);
        MetaInfoFile.createMetaInfoFile(locFile, false);
        return newGroup;
    }

    /**
     * Creates an empty workflow template/metanode in given filesystem location. Empty means that essentially only empty
     * <tt>workflow.knime</tt> and <tt>template.knime</tt> files are created and nothing else in order represent the
     * template. This is sufficient for many test cases.
     *
     * @param parent the location in which the template should be created
     * @param name the workflow's name
     * @return a workflow
     * @throws Exception if something goes wrong
     */
    @SuppressWarnings("unchecked")
    public static <T extends LocalExplorerFileStore> T createEmptyTemplate(final T parent, final String name)
        throws Exception {
        T newWf = (T)parent.getChild(name);

        // create workflow dir
        newWf.mkdir(EFS.NONE, null);

        // create a new empty workflow file
        final T workflowFile = (T)newWf.getChild(WorkflowPersistor.WORKFLOW_FILE);
        try (OutputStream outStream = workflowFile.openOutputStream(EFS.NONE, null)) {
            //
        }

        // create a new empty workflow file
        final T templateFile = (T)newWf.getChild(WorkflowPersistor.TEMPLATE_FILE);
        try (OutputStream outStream = templateFile.openOutputStream(EFS.NONE, null)) {
            //
        }

        return newWf;
    }

    /**
     * Creates a 'real' template (with no input and output ports) where the respective files
     * (<tt>workflow.knime, template.knime</tt> etc.) have actual content.
     *
     * @param parent the location in which the template should be created
     * @param name the tempate's name
     * @param wrap whether to create a 'wrapped' metanode-template to a metanode-template
     * @return the file store that represents the new template
     */
    @SuppressWarnings("unchecked")
    public static <T extends LocalExplorerFileStore> T createTemplate(final T parent, final String name, final boolean wrap) {
        if (TMP_METANODE_ROOT == null) {
            TMP_METANODE_ROOT = WorkflowManager.ROOT.createAndAddProject("KNIME Tmp MetaNode Repository",
                createWorkflowCreationHelper(parent, null));
        }
        //deactivate prompt for linking type
        IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
        prefStore.setValue(PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE, MessageDialogWithToggle.NEVER);

        WorkflowManager metaNode =
            TMP_METANODE_ROOT.createAndAddProject(name, createWorkflowCreationHelper(parent, name));
        if (wrap) {
            metaNode.getParent().convertMetaNodeToSubNode(metaNode.getID());
            boolean success = parent.getContentProvider()
                .saveSubNodeTemplate((SubNodeContainer)metaNode.getParent().getNodeContainer(metaNode.getID()), parent);
            assert success;
        } else {
            boolean success = parent.getContentProvider().saveMetaNodeTemplate(metaNode, parent);
            assert success;
        }

        return (T)parent.getChild(name);
    }

    private static WorkflowCreationHelper createWorkflowCreationHelper(final AbstractExplorerFileStore parent,
        final String name) {
        try {
            WorkflowCreationHelper workflowCreation = new WorkflowCreationHelper();
            File currentLocation;
            if(name != null) {
                currentLocation =  new File(parent.toLocalFile(), name);
            } else {
                currentLocation = parent.toLocalFile();
            }
            WorkflowContext.Factory workflowContextFactory =
                new WorkflowContext.Factory(currentLocation);
            workflowContextFactory.setMountpointRoot(parent.getContentProvider().getFileStore("/").toLocalFile());
            workflowCreation.setWorkflowContext(workflowContextFactory.createContext());
            return workflowCreation;
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

}
