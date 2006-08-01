package de.unikn.knime.workbench.extension.wizards;

import java.util.Properties;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;

import de.unikn.knime.core.node.NodeFactory;
import de.unikn.knime.core.node.NodeFactory.NodeType;

/**
 * This page enables the user to enter the information needed to create the
 * extension plugin project. The Wizard collects the values via a substitution
 * map, that is used to fill out the templates.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewKNIMEPluginWizardPage extends WizardNewProjectCreationPage
        implements Listener {

    static final String SUBST_PROJECT_NAME = "__PROJECT_NAME__";

    static final String SUBST_BASE_PACKAGE = "__BASE_PACKAGE__";

    static final String SUBST_NODE_NAME = "__NODE_NAME__";

    static final String SUBST_DESCRIPTION = "__DESCRIPTION__";

    static final String SUBST_JAR_NAME = "__JAR_NAME__";

    static final String SUBST_VENDOR_NAME = "__VENDOR_NAME__";

    static final String SUBST_CURRENT_YEAR = "__CURRENT_YEAR__";
    
    private static final String SUBST_NODE_TYPE = "__NODE_TYPE__";

    private Text m_textBasePackage;

    private Text m_textNodeName;

    private Text m_textVendor;

    private Text m_textDescription;

    private Combo m_comboNodeType;

    // private ISelection m_selection;

    /**
     * Constructor for WizardPage.
     * 
     * @param selection The initial selection
     */
    public NewKNIMEPluginWizardPage(final ISelection selection) {
        super("wizardPage");
        setTitle("Create new KNIME Extension Plugin");
        setDescription("This wizard creates a KNIME Workbench extension "
                + "(=Plugin) that provides a dummy "
                + "implementation for a new node, including dialog and view");
        // m_selection = selection;
    }

    /**
     * 
     * @return The substitution map
     */
    public Properties getSubstitutionMap() {
        Properties map = new Properties();

        map.put(SUBST_PROJECT_NAME, getProjectName());
        map.put(SUBST_BASE_PACKAGE, m_textBasePackage.getText());
        map.put(SUBST_NODE_NAME, m_textNodeName.getText());
        map.put(SUBST_DESCRIPTION, m_textDescription.getText());
        map.put(SUBST_VENDOR_NAME, m_textVendor.getText());
        map
                .put(SUBST_JAR_NAME, m_textNodeName.getText().toLowerCase()
                        + ".jar");
        map.put(SUBST_NODE_TYPE, m_comboNodeType.getText());
        return map;

    }

    /**
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage
     *      #createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl(final Composite parent) {

        super.createControl(parent);
        Composite composite = (Composite)getControl();

        // Group for KNIME settings
        Group settingsGroup = new Group(composite, SWT.NONE);
        settingsGroup.setText("KNIME extension settings");
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        settingsGroup.setLayout(layout);
        settingsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        //
        // Node/Class name
        //
        Label label = new Label(settingsGroup, SWT.NONE);
        label.setText("Node class name (e.g. 'MyLearner') : ");
        label.setFont(composite.getFont());
        // base package text field
        m_textNodeName = new Text(settingsGroup, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        m_textNodeName.setFont(composite.getFont());
        m_textNodeName.setLayoutData(data);
        m_textNodeName.addListener(SWT.Modify, this);

        //
        // Base package
        //
        label = new Label(settingsGroup, SWT.NONE);
        label.setText("Package name (e.g. 'org.myname') : ");
        label.setFont(composite.getFont());
        // base package text field
        m_textBasePackage = new Text(settingsGroup, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        m_textBasePackage.setFont(composite.getFont());
        m_textBasePackage.setLayoutData(data);
        m_textBasePackage.addListener(SWT.Modify, this);

        //
        // Vendor name
        //
        label = new Label(settingsGroup, SWT.NONE);
        label.setText("Node vendor (e.g. Your Name): ");
        label.setFont(composite.getFont());
        // base package text field
        m_textVendor = new Text(settingsGroup, SWT.BORDER);
        data = new GridData(GridData.FILL_HORIZONTAL);
        m_textVendor.setFont(composite.getFont());
        m_textVendor.setLayoutData(data);
        m_textVendor.addListener(SWT.Modify, this);

        //
        // description
        //
        label = new Label(settingsGroup, SWT.NONE);
        label.setText("Node description text : ");
        // base package text field
        m_textDescription = new Text(settingsGroup, SWT.BORDER);
        data = new GridData(GridData.FILL_BOTH);
        m_textDescription.setFont(composite.getFont());
        m_textDescription.setLayoutData(data);
        m_textDescription.addListener(SWT.Modify, this);

        //
        // node type
        //
        label = new Label(settingsGroup, SWT.NONE);
        label.setText("Node type:");
        // base package combo field
        m_comboNodeType = createCategoryCombo(settingsGroup);
        data = new GridData(GridData.FILL_BOTH);
        m_comboNodeType.setFont(composite.getFont());
        m_comboNodeType.setLayoutData(data);
        m_comboNodeType.addListener(SWT.Modify, this);

    }

    /**
     * Creates the combo box with the possible node types. Uses the information
     * from the core factory defining the types.
     * 
     * @param parent the parent composite of the combo box
     * 
     * @return the created combo box
     */
    private static Combo createCategoryCombo(Composite parent) {

        Combo typeCombo = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);

        for (NodeType type : NodeFactory.NodeType.values()) {

            // unknown is just an internal type
            if (!type.equals(NodeType.Unknown)) {
                typeCombo.add(type.toString());

                if (typeCombo.getText() == null
                        || typeCombo.getText().trim().equals("")) {
                    typeCombo.setText(type.toString());
                }
            }
        }

        return typeCombo;
    }

    /**
     * This checks the text fields after a modify event and sets the
     * errormessage if necessary. This calls <code>validatePage</code> to
     * actually validate the fields.
     * 
     * @param event
     */
    public void handleEvent(final Event event) {
        if (event.type != SWT.Modify) {
            return;
        }
        boolean valid = validatePage();
        setPageComplete(valid);
    }

    /**
     * Validates the page, e.g. checks whether the textfields contain valid
     * values
     * 
     * @see org.eclipse.ui.dialogs.WizardNewProjectCreationPage#validatePage()
     */
    @Override
    protected boolean validatePage() {
        if (!super.validatePage()) {
            return false;
        }
        // Node name
        String nodeName = m_textNodeName.getText();
        if (nodeName.length() == 0) {
            setErrorMessage(null);
            setMessage("Please provide a Node name");
            return false;
        }
        if ((!Character.isLetter(nodeName.charAt(0)))
                || (nodeName.charAt(0) != nodeName.toUpperCase().charAt(0))) {
            setErrorMessage("Node name should start with an uppercase letter");
            return false;
        }
        for (int i = 0; i < nodeName.length(); i++) {
            char c = nodeName.charAt(i);
            if (!(Character.isLetter(c) || Character.isDigit(c) || c == '_')) {
                setErrorMessage("The class name '" + nodeName + "' is invalid");
                return false;
            }
        }

        // check package name
        String basePackage = m_textBasePackage.getText();
        if (basePackage.length() == 0) {
            setErrorMessage(null);
            setMessage("Please provide a package name");
            return false;
        }
        for (int i = 0; i < basePackage.length(); i++) {
            char c = basePackage.charAt(i);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '.' || c == '_')) {
                setErrorMessage("The package name '" + basePackage
                        + "' is invalid");
                return false;
            }
        }

        // everything ok
        return true;

    }

    // /**
    // * @see IDialogPage#createControl(Composite)
    // */
    // public void createControl(Composite parent) {
    // Composite container = new Composite(parent, SWT.NULL);
    // GridLayout layout = new GridLayout();
    // container.setLayout(layout);
    // layout.numColumns = 3;
    // layout.verticalSpacing = 9;
    // Label label = new Label(container, SWT.NULL);
    // label.setText("&Container:");
    //
    // containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
    // GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    // containerText.setLayoutData(gd);
    // containerText.addModifyListener(new ModifyListener() {
    // public void modifyText(ModifyEvent e) {
    // dialogChanged();
    // }
    // });
    //
    // Button button = new Button(container, SWT.PUSH);
    // button.setText("Browse...");
    // button.addSelectionListener(new SelectionAdapter() {
    // public void widgetSelected(SelectionEvent e) {
    // handleBrowse();
    // }
    // });
    // label = new Label(container, SWT.NULL);
    // label.setText("&File name:");
    //
    // fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
    // gd = new GridData(GridData.FILL_HORIZONTAL);
    // fileText.setLayoutData(gd);
    // fileText.addModifyListener(new ModifyListener() {
    // public void modifyText(ModifyEvent e) {
    // dialogChanged();
    // }
    // });
    // initialize();
    // dialogChanged();
    // setControl(container);
    // }
    //
    // /**
    // * Tests if the current workbench selection is a suitable container to
    // use.
    // */
    //
    // private void initialize() {
    // if (selection != null && selection.isEmpty() == false
    // && selection instanceof IStructuredSelection) {
    // IStructuredSelection ssel = (IStructuredSelection) selection;
    // if (ssel.size() > 1)
    // return;
    // Object obj = ssel.getFirstElement();
    // if (obj instanceof IResource) {
    // IContainer container;
    // if (obj instanceof IContainer)
    // container = (IContainer) obj;
    // else
    // container = ((IResource) obj).getParent();
    // containerText.setText(container.getFullPath().toString());
    // }
    // }
    // fileText.setText("new_file.mpe");
    // }
    //
    // /**
    // * Uses the standard container selection dialog to choose the new value
    // for
    // * the container field.
    // */
    //
    // private void handleBrowse() {
    // ContainerSelectionDialog dialog = new ContainerSelectionDialog(
    // getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
    // "Select new file container");
    // if (dialog.open() == ContainerSelectionDialog.OK) {
    // Object[] result = dialog.getResult();
    // if (result.length == 1) {
    // containerText.setText(((Path) result[0]).toString());
    // }
    // }
    // }
    //
    // /**
    // * Ensures that both text fields are set.
    // */
    //
    // private void dialogChanged() {
    // IResource container = ResourcesPlugin.getWorkspace().getRoot()
    // .findMember(new Path(getContainerName()));
    // String fileName = getFileName();
    //
    // if (getContainerName().length() == 0) {
    // updateStatus("File container must be specified");
    // return;
    // }
    // if (container == null
    // || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
    // updateStatus("File container must exist");
    // return;
    // }
    // if (!container.isAccessible()) {
    // updateStatus("Project must be writable");
    // return;
    // }
    // if (fileName.length() == 0) {
    // updateStatus("File name must be specified");
    // return;
    // }
    // if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
    // updateStatus("File name must be valid");
    // return;
    // }
    // int dotLoc = fileName.lastIndexOf('.');
    // if (dotLoc != -1) {
    // String ext = fileName.substring(dotLoc + 1);
    // if (ext.equalsIgnoreCase("mpe") == false) {
    // updateStatus("File extension must be \"mpe\"");
    // return;
    // }
    // }
    // updateStatus(null);
    // }
    //
    // private void updateStatus(String message) {
    // setErrorMessage(message);
    // setPageComplete(message == null);
    // }
    //
    // public String getContainerName() {
    // return containerText.getText();
    // }
    //
    // public String getFileName() {
    // return fileText.getText();
    // }
}