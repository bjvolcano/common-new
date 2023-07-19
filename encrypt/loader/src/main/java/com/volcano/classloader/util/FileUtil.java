package com.volcano.classloader.util;

import com.volcano.classloader.config.Encrypt;
import com.volcano.classloader.config.SpringRegistry;
import jodd.util.StringUtil;
import lombok.SneakyThrows;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;

/**
 * @Author bjvolcano
 * @Date 2021/5/7 2:42 下午
 * @Version 1.0
 */
public class FileUtil {

    public static void copyFileUsingFileChannels(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

    @SneakyThrows
    public static void writeFile(File file, String context) {
//        FileChannel out = new FileOutputStream(file).getChannel();
//        out.write(convertStringToByte(context));
//        out.close();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(context.getBytes());
            out.flush();
        }

    }

    @SneakyThrows
    public static String readFile(File file, String encoding) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),encoding))) {
            content.append(reader.readLine()+File.separator);
        }
        return content.toString();
    }


    @SneakyThrows
    public static void writeFile(File file, byte[] context, String encoding) {
//        FileChannel out = new FileOutputStream(file).getChannel();
//        out.write(convertStringToByte(context));
//        out.close();
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),encoding))) {
            writer.write(new String(context,encoding));
            writer.flush();
        }
    }


    @SneakyThrows
    public static void writeFile(File file, byte[] context) {
//        FileChannel out = new FileOutputStream(file).getChannel();
//        out.write(convertStringToByte(context));
//        out.close();
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(context);
            out.flush();
        }
    }

    @SneakyThrows
    public static String readFile2String(File file){
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        StringBuilder content = new StringBuilder();
        while((line=reader.readLine())!=null){
            content.append(line+File.separator);
        }

        return content.toString();
    }

    @SneakyThrows
    private static ByteBuffer convertStringToByte(String content) {
        return ByteBuffer.wrap(content.getBytes("utf-8"));
    }
}
