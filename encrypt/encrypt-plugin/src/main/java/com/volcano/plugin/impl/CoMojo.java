package com.volcano.plugin.impl;

import lombok.SneakyThrows;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * copy
 * @Author bjvolcano
 * @Date 2021/5/10 6:05 下午
 * @Version 1.0
 */
@Mojo(name = "co", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false)
public class CoMojo extends BaseMojo {
    @Parameter(property = "jarTargetPath", defaultValue = "")
    private String jarTargetPath;

    @SneakyThrows
    @Override
    public void execute() {
        if (jarTargetPath != null && !jarTargetPath.trim().equals("")) {
            super.execute();
            encryptService.copyJar2TargetPath(basedir.getPath(),
                    jarTargetPath);
        }
    }
}
