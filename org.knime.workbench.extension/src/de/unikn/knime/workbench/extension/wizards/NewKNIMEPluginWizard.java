package de.unikn.knime.workbench.extension.wizards;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Properties;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import de.unikn.knime.workbench.plugin.KNIMEExtensionPlugin;

/**
 * Wizard for creating a new Plugin-Project, containing a "stub implementation"
 * of NodeModel/Dialog/View.
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NewKNIMEPluginWizard extends Wizard implements INewWizard {
    private NewKNIMEPluginWizardPage m_page;

    private ISelection m_selection;

    /**
     * Constructor for NewKNIMEPluginWizard.
     */
    public NewKNIMEPluginWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */

    @Override
    public void addPages() {
        m_page = new NewKNIMEPluginWizardPage(m_selection);
        addPage(m_page);
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     */
    @Override
    public boolean performFinish() {
        final String projectName = m_page.getProjectName();
        final Properties substitutions = m_page.getSubstitutionMap();

        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(projectName, substitutions, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(false, false, op);
        } catch (InterruptedException e) {
            MessageDialog.openError(getShell(), "Error", e.getMessage());
            logError(e);
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error", realException
                    .getMessage());
            logError(e);
            return false;
        }
        return true;
    }

    /**
     * Logs an error
     * 
     * @param e the exception
     */
    private void logError(Exception e) {
        KNIMEExtensionPlugin.getDefault().getLog().log(
                new Status(IStatus.ERROR, KNIMEExtensionPlugin.getDefault()
                        .getBundle().getSymbolicName(), 0, e.getMessage() + "",
                        e));
    }

    /**
     * The worker method. It will find the container, create the file if missing
     * or just replace its contents, and open the editor on the newly created
     * file.
     * 
     * @param provider
     * @param description
     * @param factoryClassName
     */
    private void doFinish(final String projectName,
            final Properties substitutions, final IProgressMonitor monitor)
            throws CoreException {
        
        // set the current year in the substitutions
        Calendar cal = new GregorianCalendar();
        substitutions.setProperty(NewKNIMEPluginWizardPage.SUBST_CURRENT_YEAR,
                Integer.toString(cal.get(Calendar.YEAR)));

        String packageName = substitutions.getProperty(
                NewKNIMEPluginWizardPage.SUBST_BASE_PACKAGE, "knime.dummy");
        String nodeName = substitutions.getProperty(
                NewKNIMEPluginWizardPage.SUBST_NODE_NAME, "Dummy");

        // create the hosting project
        monitor.beginTask("Creating " + projectName, 20);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(projectName);
        if (project.exists()) {
            throwCoreException("Project \"" + projectName + "\" already exist.");
        }
        project.create(monitor);
        project.open(monitor);

        IContainer container = project;
        monitor.worked(2);

        // 1. create plugin.xml / plugin.properties
        monitor.beginTask("Creating plugin descriptor/properties ....", 6);
        createFile("plugin.xml", "plugin.template", substitutions, monitor,
                container);
        createFile("plugin.properties", "plugin.properties.template",
                substitutions, monitor, container);
        createFile("build.properties", "build.properties.template",
                substitutions, monitor, container);
        createFile(".classpath", "classpath.template", substitutions, monitor,
                container);
        createFile(".cvsignore", "cvsignore.template", substitutions, monitor,
                container);
        createFile(".project", "project.template", substitutions, monitor,
                container);

        monitor.worked(6);

        // 2. create Manifest.MF
        monitor.beginTask("Creating OSGI Manifest file ....", 2);
        final IFolder metaContainer = container.getFolder(new Path("META-INF"));
        metaContainer.create(true, true, monitor);
        createFile("MANIFEST.MF", "MANIFEST.template", substitutions, monitor,
                metaContainer);
        monitor.worked(2);

        // 3. create src/bin folders
        monitor.beginTask("Creating src/bin folders ....", 2);
        final IFolder srcContainer = container.getFolder(new Path("src"));
        final IFolder binContainer = container.getFolder(new Path("bin"));
        srcContainer.create(true, true, monitor);
        binContainer.create(true, true, monitor);

        monitor.worked(2);

        // 4. create package (folders)
        String[] pathSegments = packageName.split("\\.");
        monitor.beginTask("Creating package structure ....",
                pathSegments.length);
        IFolder packageContainer = container.getFolder(new Path("src"));
        for (int i = 0; i < pathSegments.length; i++) {
            packageContainer = packageContainer.getFolder(new Path(
                    pathSegments[i]));
            packageContainer.create(true, true, monitor);
            monitor.worked(1);
        }

        // 4.1. create Bundel Activator
        monitor.beginTask("Creating Bundle Activator....", 1);
        createFile(nodeName + "NodePlugin.java", "BundleActivator.template",
                substitutions, monitor, packageContainer);

        monitor.worked(1);

        // 5. create node factory
        monitor.beginTask("Creating node factory ....", 1);
        createFile(nodeName + "NodeFactory.java", "NodeFactory.template",
                substitutions, monitor, packageContainer);

        monitor.worked(1);

        // 6. create node model
        monitor.beginTask("Creating node model ....", 1);
        final IFile nodeModelFile = createFile(nodeName + "NodeModel.java",
                "NodeModel.template", substitutions, monitor, packageContainer);
        monitor.worked(1);

        // 7. create node dialog
        monitor.beginTask("Creating node dialog ....", 1);
        createFile(nodeName + "NodeDialog.java", "NodeDialog.template",
                substitutions, monitor, packageContainer);
        monitor.worked(1);

        // 8. create node view
        monitor.beginTask("Creating node view ....", 1);
        createFile(nodeName + "NodeView.java", "NodeView.template",
                substitutions, monitor, packageContainer);
        monitor.worked(1);

        // 9. create node description xml file
        monitor.beginTask("Creating node description xml file ....", 1);
        createFile(nodeName + "NodeFactory.xml", "NodeDescriptionXML.template",
                substitutions, monitor, packageContainer);

        monitor.worked(1);
        
        // 10. create node description xml file
        monitor.beginTask("Creating package.html file ....", 1);
        createFile("package.html", "packageHTML.template",
                substitutions, monitor, packageContainer);

        monitor.worked(1);

        // 11. copy additional files (icon, ...)
        monitor.beginTask("Adding additional files....", 2);
        IFile defIcon = packageContainer.getFile("default.png");

        // copy default.png
        URL url = KNIMEExtensionPlugin.getDefault().getBundle().getEntry(
                "templates/default.png");
        File iconFile;
        try {
            iconFile = new File(Platform.resolve(url).getFile());
            defIcon.create(new FileInputStream(iconFile), true, monitor);
        } catch (IOException e1) {
            e1.printStackTrace();
            throwCoreException(e1.getMessage());
        }

        monitor.worked(2);

        // open the model file in the editor
        monitor.setTaskName("Opening file for editing...");
        getShell().getDisplay().asyncExec(new Runnable() {
            public void run() {
                IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                try {
                    IDE.openEditor(page, nodeModelFile, true);
                } catch (PartInitException e) {
                }
            }
        });
        monitor.worked(1);
    }

    /**
     * @param monitor
     * @param container
     * @return The file
     * @throws CoreException
     */
    private IFile createFile(final String filename, final String templateFile,
            final Properties substitutions, final IProgressMonitor monitor,
            IContainer container) throws CoreException {
        final IFile file = container.getFile(new Path(filename));
        try {
            InputStream stream = openSubstitutedContentStream(templateFile,
                    substitutions);
            if (file.exists()) {
                file.setContents(stream, true, true, monitor);
            } else {
                file.create(stream, true, monitor);
            }
            stream.close();
        } catch (IOException e) {
            throwCoreException(e.getMessage());
        }
        return file;
    }

    /**
     * We will initialize file contents with an empty String
     * 
     * @throws CoreException
     */
    private InputStream openSubstitutedContentStream(
            final String templateFileName, final Properties substitutions)
            throws CoreException {
        URL url = KNIMEExtensionPlugin.getDefault().getBundle().getEntry(
                "templates/" + templateFileName);
        File templateFile = null;
        String contents = "";
        try {
            templateFile = new File(Platform.resolve(url).getFile());

            BufferedReader reader = new BufferedReader(new FileReader(
                    templateFile));
            String line = reader.readLine();
            StringBuffer buf = new StringBuffer();
            while (line != null) {
                buf.append(line);
                buf.append("\n");
                line = reader.readLine();
            }
            reader.close();
            contents = buf.toString();

            // substitute all placeholders
            // TODO this eats memory... make it more beautiful
            for (Iterator it = substitutions.keySet().iterator(); it.hasNext();) {
                String key = (String)it.next();

                contents = contents.replaceAll(key, substitutions.getProperty(
                        key, "??" + key + "??"));
            }

        } catch (Exception e) {
            logError(e);
            throwCoreException("Can't process template file: url=" + url
                    + " ;file=" + templateFile);
        }

        return new ByteArrayInputStream(contents.getBytes());
    }

    /**
     * 
     * @param message
     * @throws CoreException
     */
    private void throwCoreException(final String message) throws CoreException {
        IStatus status = new Status(IStatus.ERROR,
                "de.unikn.knime.workbench.extension", IStatus.OK, message, null);
        throw new CoreException(status);
    }

    /**
     * 
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
     *      org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(final IWorkbench workbench,
            final IStructuredSelection selection) {
        // TODO Auto-generated method stub

    }

}