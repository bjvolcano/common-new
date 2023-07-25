package org.springframework.boot.loader;

import com.volcano.classloader.config.Encrypt;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.ExplodedArchive;
import org.springframework.boot.loader.archive.JarFileArchive;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class CmdLauncher extends ExecutableArchiveLauncher {

    private static final String DEFAULT_CLASSPATH_INDEX_LOCATION = "BOOT-INF/classpath.idx";

    static final Archive.EntryFilter NESTED_ARCHIVE_ENTRY_FILTER = (entry) -> {
        if (entry.isDirectory()) {
            return entry.getName().equals("BOOT-INF/classes/");
        }
        return entry.getName().startsWith("BOOT-INF/lib/");
    };

    public CmdLauncher() {
    }

    protected CmdLauncher(Archive archive) {
        super(archive);
    }

    @Override
    protected ClassPathIndexFile getClassPathIndex(Archive archive) throws IOException {
        // Only needed for exploded archives, regular ones already have a defined order
        if (archive instanceof ExplodedArchive) {
            String location = getClassPathIndexFileLocation(archive);
            return ClassPathIndexFile.loadIfPossible(archive.getUrl(), location);
        }
        return super.getClassPathIndex(archive);
    }

    private String getClassPathIndexFileLocation(Archive archive) throws IOException {
        Manifest manifest = archive.getManifest();
        Attributes attributes = (manifest != null) ? manifest.getMainAttributes() : null;
        String location = (attributes != null) ? attributes.getValue(BOOT_CLASSPATH_INDEX_ATTRIBUTE) : null;
        return (location != null) ? location : DEFAULT_CLASSPATH_INDEX_LOCATION;
    }

    @Override
    protected boolean isPostProcessingClassPathArchives() {
        return false;
    }

    @Override
    protected boolean isSearchCandidate(Archive.Entry entry) {
        return entry.getName().startsWith("BOOT-INF/");
    }

    @Override
    protected boolean isNestedArchive(Archive.Entry entry) {
        return NESTED_ARCHIVE_ENTRY_FILTER.matches(entry);
    }

    public static void main(String[] args) throws Exception {
        Encrypt.load();
        String path = CmdLauncher.class.getResource("/").getPath();
        String runPath;
        if (Arrays.asList(args).isEmpty()) {
            runPath = path + ".." + File.separator + "test.jar";
        } else {
            runPath = path + args[0];
        }

        System.out.println("run ..." + runPath);
        Archive archive = new JarFileArchive(new File(runPath));
        new CmdLauncher(archive).launch(args);
    }
}
