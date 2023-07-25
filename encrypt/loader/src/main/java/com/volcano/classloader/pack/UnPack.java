package com.volcano.classloader.pack;

import com.volcano.classloader.config.Encrypt;
import com.volcano.classloader.loader.EncryptClassLoader;
import com.volcano.util.EncryptUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;

/**
 * @Author bjvolcano
 * @Date 2021/5/13 3:27 下午
 * @Version 1.0
 */
@Slf4j
public class UnPack {

    @SneakyThrows
    public void unEncryptClasses(String classPath, String pwdPath) {
        File classRoot = new File(pwdPath);
        File[] files = classRoot.listFiles();
        if (files == null) {
            log.error("no have classes files!");
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                unEncryptClasses(classPath, file.getPath());
                continue;
            }

            if (file.getName().endsWith(".class")) {
                String name = file.getPath()
                        .replace(classPath + File.separator, "")
                        .replaceAll("/", ".")
                        .replace(".class", "");
                EncryptClassLoader.getInstance().decryptClass(new FileInputStream(file), name);
            }
        }

    }

    @SneakyThrows
    public static byte[] deEncrypt(byte[] bytes) {
        return EncryptUtils.confuse(bytes, Encrypt.getInstance().getKey().hashCode(), false);
    }
}
