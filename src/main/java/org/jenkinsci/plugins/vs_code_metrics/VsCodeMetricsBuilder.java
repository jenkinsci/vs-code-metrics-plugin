package org.jenkinsci.plugins.vs_code_metrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jenkinsci.plugins.vs_code_metrics.util.StringUtil;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.CopyOnWrite;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;

/**
 * @author Yasuyuki Saito
 */
public class VsCodeMetricsBuilder extends Builder {

    private final String toolName;
    private final String files;
    private final String outputXML;
    private final String directory;
    private final boolean searchGac;
    private final String platform;
    private final String reference;
    private final boolean ignoreInvalidTargets;
    private final String cmdLineArgs;

    /**
     *
     * @param toolName
     * @param files
     * @param outputXML
     * @param directory
     * @param searchGac
     * @param platform
     * @param reference
     * @param ignoreInvalidTargets
     * @param cmdLineArgs
     */
    @DataBoundConstructor
    public VsCodeMetricsBuilder(String toolName, String files, String outputXML, String directory, boolean searchGac
                                ,String platform, String reference, boolean ignoreInvalidTargets, String cmdLineArgs) {
        this.toolName             = toolName;
        this.files                = files;
        this.outputXML            = outputXML;
        this.directory            = directory;
        this.searchGac            = searchGac;
        this.platform             = platform;
        this.reference            = reference;
        this.ignoreInvalidTargets = ignoreInvalidTargets;
        this.cmdLineArgs          = cmdLineArgs;
    }

    public String getToolName() {
        return toolName;
    }

    public String getFiles() {
        return files;
    }

    public String getOutputXML() {
        return outputXML;
    }

    public String getDirectory() {
        return directory;
    }

    public boolean isSearchGac() {
        return searchGac;
    }

    public String getPlatform() {
        return platform;
    }

    public String getReference() {
        return reference;
    }

    public boolean isIgnoreInvalidTargets() {
        return ignoreInvalidTargets;
    }

    public String getCmdLineArgs() {
        return cmdLineArgs;
    }

    public VsCodeMetricsInstallation getInstallation() {
        if (toolName == null) return null;
        for (VsCodeMetricsInstallation i : DESCRIPTOR.getInstallations()) {
            if (toolName.equals(i.getName()))
                return i;
        }
        return null;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        ArrayList<String> args = new ArrayList<String>();
        EnvVars env = build.getEnvironment(listener);

        // Metrics.exe path.
        String toolPath = getToolPath(launcher, listener, env);
        if (StringUtil.isNullOrSpace(toolPath)) return false;
        args.add(toolPath);

        // Assembly file(s) to analyze.
        if (!StringUtil.isNullOrSpace(files))
            args.addAll(getArguments(build, env, "file", files));

        // Metrics results XML output file.
        if (!StringUtil.isNullOrSpace(outputXML)) {
            args.add(StringUtil.convertArgumentWithQuote("out", outputXML));

            FilePath outputXMLPath = build.getWorkspace().child(outputXML);
            if (outputXMLPath.exists())
                outputXMLPath.delete();
            else
                outputXMLPath.getParent().mkdirs();
        }

        // Location to search for assembly dependencies.
        if (!StringUtil.isNullOrSpace(directory))
            args.addAll(getArguments(build, env, "directory", directory));

        // Search the Global Assembly Cache for missing references.
        if (searchGac)
            args.add("/searchgac");

        // Location of framework assemblies, such as mscorlib.dll.
        if (!StringUtil.isNullOrSpace(platform))
            args.add(StringUtil.convertArgumentWithQuote("platform", platform));

        // Reference assemblies required for analysis.
        if (!StringUtil.isNullOrSpace(reference))
            args.addAll(getArguments(build, env, "reference", reference));

        // Silently ignore invalid target files.
        if (ignoreInvalidTargets)
            args.add("/ignoreinvalidtargets");

        // Manual Command Line String
        if (!StringUtil.isNullOrSpace(cmdLineArgs))
            args.add(cmdLineArgs);

        // Metrics.exe run.
        boolean r = execTool(args, build, launcher, listener, env);

        return r;
    }


    /**
     *
     * @param  launcher
     * @param  listener
     * @param  env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private String getToolPath(Launcher launcher, BuildListener listener, EnvVars env) throws InterruptedException, IOException {
        String execName = "Metrics.exe";
        VsCodeMetricsInstallation installation = getInstallation();

        if (installation == null) {
            listener.getLogger().println("Path To Metrics.exe: " + execName);
            return execName;
        } else {
            installation = installation.forNode(Computer.currentComputer().getNode(), listener);
            installation = installation.forEnvironment(env);
            String pathToMetrics = installation.getHome();
            FilePath exec = new FilePath(launcher.getChannel(), pathToMetrics);

            try {
                if (!exec.exists()) {
                    listener.fatalError(pathToMetrics + " doesn't exist");
                    return null;
                }
            } catch (IOException e) {
                listener.fatalError("Failed checking for existence of " + pathToMetrics);
                return null;
            }

            listener.getLogger().println("Path To Metrics.exe: " + pathToMetrics);
            return StringUtil.appendQuote(pathToMetrics);
        }
    }

    /**
     *
     * @param  build
     * @param  env
     * @param  option
     * @param  values
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private List<String> getArguments(AbstractBuild<?, ?> build, EnvVars env, String option, String values) throws InterruptedException, IOException {
        ArrayList<String> args = new ArrayList<String>();
        StringTokenizer valuesToknzr = new StringTokenizer(values, " \t\r\n");

        while (valuesToknzr.hasMoreTokens()) {
            String value = valuesToknzr.nextToken();
            value = Util.replaceMacro(value, env);
            value = Util.replaceMacro(value, build.getBuildVariables());

            if (!StringUtil.isNullOrSpace(value))
                args.add(StringUtil.convertArgumentWithQuote(option, value));
        }

        return args;
    }

    /**
     *
     * @param  args
     * @param  build
     * @param  launcher
     * @param  listener
     * @param  env
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private boolean execTool(List<String> args, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, EnvVars env) throws InterruptedException, IOException {
        ArgumentListBuilder cmdExecArgs = new ArgumentListBuilder();
        FilePath tmpDir = null;
        FilePath pwd = build.getWorkspace();

        if (!launcher.isUnix()) {
            tmpDir = pwd.createTextTempFile("vs_code_metrics", ".bat", StringUtil.concatString(args), false);
            cmdExecArgs.add("cmd.exe", "/C", tmpDir.getRemote(), "&&", "exit", "%ERRORLEVEL%");
        } else {
            for (String arg : args) {
                cmdExecArgs.add(arg);
            }
        }

        listener.getLogger().println("Executing Metrics: " + cmdExecArgs.toStringWithQuote());

        try {
            int r = launcher.launch().cmds(cmdExecArgs).envs(env).stdout(listener).pwd(pwd).join();
            return (r == 0);
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("Metrics execution failed"));
            return false;
        } finally {
            try {
                if (tmpDir != null) tmpDir.delete();
            } catch (IOException e) {
                Util.displayIOException(e, listener);
                e.printStackTrace(listener.fatalError("temporary file delete failed"));
            }
        }
    }


    @Override
    public Descriptor<Builder> getDescriptor() {
         return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    /**
     * @author Yasuyuki Saito
     */
    public static final class DescriptorImpl extends Descriptor<Builder> {

        @CopyOnWrite
        private volatile VsCodeMetricsInstallation[] installations = new VsCodeMetricsInstallation[0];

        DescriptorImpl() {
            super(VsCodeMetricsBuilder.class);
            load();
        }

        public String getDisplayName() {
            return Messages.VsCodeMetricsBuilder_DisplayName();
        }

        public VsCodeMetricsInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(VsCodeMetricsInstallation... installations) {
            this.installations = installations;
            save();
        }

        /**
         * Obtains the {@link VsCodeMetricsInstallation.DescriptorImpl} instance.
         */
        public VsCodeMetricsInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(VsCodeMetricsInstallation.DescriptorImpl.class);
        }
    }
}
