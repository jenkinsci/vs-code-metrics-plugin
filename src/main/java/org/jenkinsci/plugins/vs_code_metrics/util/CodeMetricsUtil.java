package org.jenkinsci.plugins.vs_code_metrics.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map.Entry;

import hudson.FilePath;
import hudson.model.AbstractBuild;

import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

import org.jenkinsci.plugins.vs_code_metrics.Messages;
import org.jenkinsci.plugins.vs_code_metrics.bean.*;

import javax.xml.parsers.ParserConfigurationException;

public abstract class  CodeMetricsUtil {

    /** */
    private static final String[] ATTRIBUTE_NAMES = new String[] { "name", "mi", "cyc", "cls", "doi", "loc" };

    /** */
    private static final String[] PROPERTIES_NAMES = new String[] { "name", "maintainabilityIndex", "cyclomaticComplexity", "classCoupling", "depthOfInheritance", "linesOfCode" };

    private CodeMetricsUtil() {}

    /**
     *
     * @param  path
     * @return
     * @throws VsCodeMetricsException
     */
    public static CodeMetrics createCodeMetrics(FilePath path) throws IOException, InterruptedException {
        InputStream stream = null;
        try {
            stream = path.read();

            Digester digester = new Digester();
            if (!Boolean.getBoolean(CodeMetricsUtil.class.getName() + ".UNSAFE")) {
                digester.setXIncludeAware(false);
                try {
                    digester.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                    digester.setFeature("http://xml.org/sax/features/external-general-entities", false);
                    digester.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                    digester.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                } catch (ParserConfigurationException ex) {
                    throw new SAXException("Failed to securely configure xml digester parser", ex);
                }
            }
            digester.setClassLoader(CodeMetrics.class.getClassLoader());

            digester.addObjectCreate("*/report", CodeMetrics.class);

            digester.addObjectCreate("*/module", Module.class);
            digester.addSetNext("*/module", "addChild");
            digester.addSetProperties("*/module", ATTRIBUTE_NAMES, PROPERTIES_NAMES);

            digester.addObjectCreate("*/namespace", Namespace.class);
            digester.addSetNext("*/namespace", "addChild");
            digester.addSetProperties("*/namespace", ATTRIBUTE_NAMES, PROPERTIES_NAMES);

            digester.addObjectCreate("*/type", Type.class);
            digester.addSetNext("*/type", "addChild");
            digester.addSetProperties("*/type", ATTRIBUTE_NAMES, PROPERTIES_NAMES);

            digester.addObjectCreate("*/member", Member.class);
            digester.addSetNext("*/member", "addChild");
            digester.addSetProperties("*/member", ATTRIBUTE_NAMES, PROPERTIES_NAMES);

            CodeMetrics bean = (CodeMetrics)digester.parse(stream);

            return bean;
        } catch (SAXException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            try { if (stream != null) stream.close(); } catch (Exception e) {}
        }
    }

    /**
     *
     * @param  build
     * @return
     */
    public static File getReportDir(AbstractBuild<?, ?> build) {
        return new File(build.getRootDir(), Constants.REPORT_DIR);
    }

    /**
     *
     * @param  file
     * @return
     * @throws VsCodeMetricsException
     */
    public static FilePath[] getReports(File file) throws IOException, InterruptedException {
        FilePath path = new FilePath(file);
        if (path.isDirectory()) {
            return path.list("*xml");
        } else {
            FilePath report = new FilePath(new File(path.getName() + ".xml"));
            return report.exists() ? new FilePath[]{report} : new FilePath[0];
        }
    }

    /**
     *
     * @param  build
     * @return
     */
    public static CodeMetrics getCodeMetrics(AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        File reportFolder = getReportDir(build);

        FilePath[] reports = getReports(reportFolder);
        CodeMetrics total = null;

        for (FilePath report : reports) {
            CodeMetrics bean = createCodeMetrics(report);
            if (bean == null) continue;

            if (total != null) {
                for (Entry<String, Module> entry : bean.getChildren().entrySet()) {
                    if (!total.getChildren().containsKey(entry.getKey())) {
                        total.addChild(entry.getValue());
                    }
                }
            } else {
                total = bean;
            }
        }

        // set total bean.
        total.setName(Messages.Summart_AllClasses());

        if (total.getChildren().size() > 0) {
            int sumMaintainabilityIndex = 0;
            int sumCyclomaticComplexity = 0;
            int sumClassCoupling        = 0;
            int sumDepthOfInheritance   = 0;
            int sumtLinesOfCode         = 0;

            for (Module module : total.getChildren().values()) {
                sumMaintainabilityIndex += module.getMaintainabilityIndex();
                sumCyclomaticComplexity += module.getCyclomaticComplexity();
                sumClassCoupling        += module.getClassCoupling();
                sumDepthOfInheritance   += module.getDepthOfInheritance();
                sumtLinesOfCode         += module.getLinesOfCode();
            }

            total.setMaintainabilityIndex(sumMaintainabilityIndex / total.getChildren().size());
            total.setCyclomaticComplexity(sumCyclomaticComplexity);
            total.setClassCoupling(sumClassCoupling);
            total.setDepthOfInheritance(sumDepthOfInheritance);
            total.setLinesOfCode(sumtLinesOfCode);
        }

        return total;
    }

    public static FilePath[] locateReports(FilePath workspace, String includes) throws IOException, InterruptedException {

        try {
            FilePath[] ret = workspace.list(includes);
            if (ret.length > 0) {
                return ret;
            }
        } catch (Exception e) {
        }

        ArrayList<FilePath> files = new ArrayList<FilePath>();
        String parts[] = includes.split("\\s*[;:,]+\\s*");
        for (String path : parts) {
            FilePath src = workspace.child(path);
            if (src.exists()) {
                if (src.isDirectory()) {
                    files.addAll(Arrays.asList(src.list("**/metrics*.xml")));
                } else {
                    files.add(src);
                }
            }
        }
        return files.toArray(new FilePath[files.size()]);
    }

    /**
     *
     * @param folder
     * @param files
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean saveReports(FilePath folder, FilePath[] files) throws IOException, InterruptedException {
        boolean r = true;
        ReportConverter converter = new ReportConverter();
        folder.mkdirs();
        for (int i = 0; i < files.length; i++) {
            String name = "metrics" + (i > 0 ? i : "") + ".xml";
            FilePath src = files[i];
            FilePath dst = folder.child(name);

            if (!converter.convertFile(src.read(), new File(dst.getRemote())))
                r = false;
        }
        return r;
    }

    public static AbstractBean<?> searchBean(AbstractBean<?> bean, String[] tokens) {
        return searchBean(bean, tokens, 0);
    }

    public static AbstractBean<?> searchBean(AbstractBean<?> bean, String[] tokens, int index) {
        if (tokens.length <= index)
            return bean;

        if (!bean.getChildren().containsKey(tokens[index]))
            return null;
        else
            return searchBean((AbstractBean<?>)bean.getChildren().get(tokens[index]), tokens, index + 1);
    }
}
