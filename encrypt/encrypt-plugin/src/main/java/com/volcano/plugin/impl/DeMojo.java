package com.volcano.plugin.impl;

import lombok.SneakyThrows;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * 测试编码classes
 *
 */

@Mojo(name = "de", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class DeMojo extends BaseMojo {
    @Override
    @SneakyThrows
    public void execute() {
        super.execute();
        encryptService.testEncryptClasses(basedir.getPath());
    }
}