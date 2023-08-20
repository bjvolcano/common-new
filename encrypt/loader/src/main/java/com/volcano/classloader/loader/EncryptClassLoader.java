package com.volcano.classloader.loader;

import com.volcano.classloader.pack.UnPack;
import com.volcano.classloader.util.LoaderUtil;
import com.volcano.util.IoUtils;
import com.volcano.util.SpringContextUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.JarEntry;

import org.springframework.boot.loader.jar.JarFile;

import java.util.zip.ZipEntry;

import static com.volcano.classloader.util.LoaderUtil.CLASS_FILE_BYTES;

/**
 * @Author bjvolcano
 * @Date 2021/5/7 1:52 下午
 * @Version 1.0
 */
@Slf4j
public class EncryptClassLoader extends URLClassLoader {
    private static final ScheduledExecutorService checkPool = Executors.newSingleThreadScheduledExecutor();

    private static final Map<String, Class> classes = new ConcurrentHashMap();

    private static final Map<File, Long> fileModify = new HashMap();

    private static EncryptClassLoader INSTANCE;

    private DefaultListableBeanFactory beanFactory;

    public void setBeanFactory(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    private EncryptClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        log.info("Parent classloader : {}", parent);
        //Thread.currentThread().setContextClassLoader(this);
    }

    public static synchronized EncryptClassLoader getInstance() {
        return INSTANCE;
    }

    public static synchronized ClassLoader getInstance(final ClassLoader classLoader) {
        if (INSTANCE != null) {
            return INSTANCE;
        }

        final String path = System.getProperty("java.class.path");
        final File[] Files = path == null ? new File[0] : LoaderUtil.getClassPath(path);
        final File[] files;
        try {
            files = LoaderUtil.addJarLibUrls(Files);
        } catch (Exception e) {
            INSTANCE = new EncryptClassLoader(new URL[0], classLoader);
            return INSTANCE;
        }

        return AccessController.doPrivileged((PrivilegedAction<EncryptClassLoader>) () -> {
                    URL[] sourceUrls = pathToURLs(files, false, true);
                    //URL[] encryptUrls = StringUtils.isEmpty(path) ? new URL[0] : pathToURLs(files, true, false);
                    INSTANCE = new EncryptClassLoader(sourceUrls, classLoader);
                    //LoaderUtil.processParentClassLoaderUrls(encryptUrls, sourceUrls, classLoader, INSTANCE);
                    return INSTANCE;
                }
        );
    }

    public void start() {
        loadClasses2Cache();
        checkPool.scheduleWithFixedDelay(() -> {
            loadClasses2Cache();
        }, 60, 60, TimeUnit.SECONDS);
    }

    @SneakyThrows
    public void loadClasses2Cache() {
        URL[] urls = this.getURLs();
        if (urls != null) {
            for (URL url : urls) {
                // 系统类库路径
                File libPath = new File(URLDecoder.decode(url.getPath(), "utf-8"));
                // 获取所有的.jar和.zip文件
                File[] jarFiles = null;
                if (libPath.isDirectory()) {
                    jarFiles = libPath.listFiles((File dir, String name) ->
                            name.endsWith(".jar") || name.endsWith(".zip")
                    );
                    //目录下全是class
                    Long lastModify = fileModify.get(libPath);
                    fileModify.put(libPath, libPath.lastModified());
                    if (lastModify == null || lastModify > libPath.lastModified()) {
                        scanClassByDir(libPath.getPath(), libPath);
                    }
                } else if (libPath.getName().endsWith(".jar") || libPath.getName().endsWith(".zip")) {
                    jarFiles = new File[]{libPath};
                }

                if (jarFiles != null) {
                    // 从URLClassLoader类中获取类所在文件夹的方法
                    // 对于jar文件，可以理解为一个存放class文件的文件夹
                    try {
                        Arrays.stream(jarFiles).parallel().forEach(
                                file -> {
                                    Long lastModify = fileModify.get(file);
                                    fileModify.put(file, file.lastModified());
                                    if (lastModify == null || lastModify > file.lastModified()) {
                                        scanClassJar(file);
                                    }
                                }
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void scanClassByDir(String rootPath, File dir) {
        if (LoaderUtil.isEncrypted(dir)) {
            log.info("Load dir : {}", dir.getPath());
            listFileByDir(rootPath, dir);
        }
    }

    @SneakyThrows
    private void listFileByDir(String rootPath, File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                listFileByDir(rootPath, file);
            } else if (file.getName().endsWith(".class")) {
                String name = file.getPath().replace(rootPath + File.separator, "");
                name = name.replace(".class", "").replaceAll("/", ".");
                CLASS_FILE_BYTES.set(IoUtils.readFileToByte(file));
                findClass(name);
            }
        }

    }

    @SneakyThrows
    private void scanClassJar(File file) {
        if (LoaderUtil.isEncrypted(file)) {
            JarFile jarFile = null;
            String[] paths = file.getPath().split("!" + File.separator);
            log.info("Has encrypt jar ,Nested jar File : {}", paths[1]);
            //jar包内部包
            if (paths.length == 2) {
                jarFile = new JarFile(new File(paths[0]));
                ZipEntry entry = jarFile.getEntry(paths[1]);
                jarFile = jarFile.getNestedJarFile(entry);
            } else {
                jarFile = new JarFile(file);
            }

            Enumeration<JarEntry> jarEntrys = jarFile.entries();
            while (jarEntrys.hasMoreElements()) {
                JarEntry entry = jarEntrys.nextElement();
                // 简单的判断路径，如果想做到像Spring，Ant-Style格式的路径匹配需要用到正则。
                String name = entry.getName();
                if (!entry.isDirectory() && name.endsWith(".class")) {
                    name = name.replace(".class", "").replaceAll("/", ".");
                    byte[] bytes = IoUtils.toBytes(jarFile.getInputStream(entry));
                    CLASS_FILE_BYTES.set(bytes);
                    findClass(name);
                }
            }
        }
    }

    /**
     * 向spring容器注入beanDefinition
     *
     * @param className 全限定名 (com.xxx.xx.XXXX)
     */
    public void registryBean(String className) {
        Class cls = classes.get(className);
        if (cls == null) {
            return;
        }

        if (SpringContextUtil.isSpringBeanClass(cls) && beanFactory != null) {
            BeanDefinition beanDefinition = SpringContextUtil.buildBeanDefinition(cls);
            //将变量首字母置小写
            String beanName = StringUtils.uncapitalize(className);
            beanName = beanName.substring(beanName.lastIndexOf(".") + 1);
            beanName = StringUtils.uncapitalize(beanName);
            beanFactory.registerBeanDefinition(beanName, beanDefinition);
            log.info("Register to spring : {}", beanName);
        }
    }

    @SneakyThrows
    public Class decryptClass(InputStream is, String name) {
        byte[] bytes = IoUtils.toBytes(is);
        return parseClass(bytes, name);
    }

    private Class parseClass(byte[] oldBytes, String name) {
        byte[] bytes;
        try {
            bytes = UnPack.deEncrypt(oldBytes);
        } catch (Exception e) {
            //可能在开发环境中运行的时候会重新编译
            bytes = oldBytes;
            log.warn("{} load err! unEncrypt is null", name);
        }

        // defineClass方法可以将byte数组转化为一个类的Class对象实例
        //writeClass2RootPath(name,bys);
        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class cls = null;
            try {
                cls = super.findClass(name);
            } catch (Exception | Error e) {
                cls = decryptClass(name);
            } finally {
                CLASS_FILE_BYTES.remove();
            }

            return cls;
        }
    }

    private Class decryptClass(String name) {
        Class cls = classes.get(name);
        if (cls != null) {
            return cls;
        }

        byte[] bytes = CLASS_FILE_BYTES.get();
        if (bytes != null) {
            log.info("Load encrypt class by bytes ok : {}", name);
            cls = parseClass(bytes, name);
            if (cls != null) {
                classes.put(name, cls);
                registryBean(name);
            }
        }

        return cls;
    }


    private static URL[] pathToURLs(File[] files, boolean encryptedCheck, boolean all) {
        List<URL> urls = new ArrayList<>();
        boolean add;
        for (int i = 0; i < files.length; ++i) {
            add = false;
            if (all) {
                add = true;
            } else if (encryptedCheck && LoaderUtil.isEncrypted(files[i])) {
                add = true;
            } else if (!encryptedCheck && !LoaderUtil.isEncrypted(files[i])) {
                add = true;
            }

            if (add) {
                urls.add(LoaderUtil.getFileURL(files[i]));
            }
        }

        return urls.toArray(new URL[urls.size()]);
    }


    public void registryBeans() {
        if (!CollectionUtils.isEmpty(classes) && beanFactory != null) {
            classes.forEach(
                    (k, v) -> {
                        registryBean(k);
                    }
            );
        }
    }

    public void addUrls(String[] urls) {
        if (urls != null) {
            for (String url : urls) {
                try {
                    this.addURL(new URL(url));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}