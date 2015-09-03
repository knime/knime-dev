On recent Linux distributions with GTK3, the KNIME SDK may get stuck during startup. This can be fixed by setting the
environment variable SWT_GTK3=0 prior to starting Eclipse. The eclipse_knime shell script in this folder takes care
of this. You can use it instead of the standard "eclipse" launcher.