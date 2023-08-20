package com.volcano.plugin.impl;

import lombok.SneakyThrows;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * 编码classes
 *
 */

@Mojo(name = "en")
public class EnMojo extends BaseMojo {
    @Override
    @SneakyThrows
    public void execute() {
        super.execute();
        encryptService.encryptClasses(basedir.getPath());
    }
}