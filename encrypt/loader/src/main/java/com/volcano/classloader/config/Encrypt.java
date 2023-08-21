package com.volcano.classloader.config;

import com.volcano.classloader.loader.EncryptClassLoader;
import com.volcano.util.HttpClientResult;
import com.volcano.util.HttpClientUtils;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.ho.yaml.Yaml;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author bjvolcano
 * @Date 2021/5/11 3:21 下午
 * @Version 1.0
 */
@Slf4j
@Data
public class Encrypt {
    public static final String ENCRYPT_FILE = "encrypt.yml";

    public static final String CHARSET = "UTF-8";
    //@Value("${encrypt-classes.libPath:}")
    private String libPath;

    //@Value("${encrypt-classes.key}")
    private String key;

    private byte[] keyBytes;

    //@Value("${encrypt-classes.keyUrl:}")
    private Net net = new Net();

    private static Encrypt INSTANCE;

    public static synchronized Encrypt getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }

        return INSTANCE;
    }

    @SneakyThrows
    public static synchronized Encrypt load() {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        log.debug("load encrypt config by path : {}", Encrypt.ENCRYPT_FILE);
        InputStream inputStream = Encrypt.class.getClassLoader().getResourceAsStream(Encrypt.ENCRYPT_FILE);
        if (inputStream == null) {
            throw new RuntimeException("no have encrypt.yml , or dependency encrypt-key project , do not init DesClassLoader!");
        }

        INSTANCE = Yaml.loadType(inputStream, Encrypt.class);
        INSTANCE.fillClassLoader();
        return INSTANCE;
    }

    @SneakyThrows
    public void fillClassLoader() {
        if (StringUtils.isEmpty(key) && StringUtils.isEmpty(net.getUrl())) {
            throw new RuntimeException("please set key or keyurl!");
        }

        if (!StringUtils.isEmpty(key)) {
            keyBytes = key.trim().getBytes();
        } else {
            key = net.getKeyByRemote();
            keyBytes = key.getBytes();
        }

        setClassLoader();
    }


    @SneakyThrows
    private void setClassLoader() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        EncryptClassLoader instance = (EncryptClassLoader) EncryptClassLoader.getInstance(classLoader);
        if (instance == null) {
            return;
        }

        if (!StringUtils.isEmpty(libPath)) {
            File libsPath = new File(libPath);
            if (libsPath.isDirectory()) {
                List<File> files = getFiles(libsPath, null);
                if (!CollectionUtils.isEmpty(files)) {
                    for (File file : files) {
                        instance.addUrls(new String[]{"file:" + file.getPath()});
                    }
                }
            }
        }

        instance.start();
    }

    private List<File> getFiles(File dir, List<File> files) {
        if (files == null) {
            files = new ArrayList();
        }

        File[] subs = dir.listFiles();
        for (File file : subs) {
            if (file.isDirectory()) {
                files.addAll(getFiles(file, files));
            } else if (file.getName().endsWith(".jar")) {
                files.add(file);
            }
        }

        return files;
    }
}
