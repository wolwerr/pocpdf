package com.poc.pocpdf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Azure azure = new Azure();
    private final Storage storage = new Storage();
    private final LibreOffice libreOffice = new LibreOffice();

    public Azure getAzure() { return azure; }
    public Storage getStorage() { return storage; }
    public LibreOffice getLibreOffice() { return libreOffice; }

    public static class Azure {
        private String connectionString;
        public String getConnectionString() { return connectionString; }
        public void setConnectionString(String connectionString) { this.connectionString = connectionString; }
    }

    public static class Storage {
        private String templatesContainer = "templates";
        private String outputContainer = "output";
        private String workDir = "out/work";
        public String getTemplatesContainer() { return templatesContainer; }
        public void setTemplatesContainer(String templatesContainer) { this.templatesContainer = templatesContainer; }
        public String getOutputContainer() { return outputContainer; }
        public void setOutputContainer(String outputContainer) { this.outputContainer = outputContainer; }
        public String getWorkDir() { return workDir; }
        public void setWorkDir(String workDir) { this.workDir = workDir; }
    }

    public static class LibreOffice {
        private String sofficePath = "/usr/bin/soffice";
        public String getSofficePath() { return sofficePath; }
        public void setSofficePath(String sofficePath) { this.sofficePath = sofficePath; }
    }
}
