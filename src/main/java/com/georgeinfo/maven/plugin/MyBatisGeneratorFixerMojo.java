package com.georgeinfo.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author George
 * Created on 2023/05/17
 * mybatis generator修复插件，是一个maven插件，调用时传参basedir 和 scanPackage两个参数，示例配置如下：
 * <plugin>
 * <groupId>com.georgeinfo.maven.plugin</groupId>
 * <artifactId>mybatis-generator-fixer</artifactId>
 * <version>1.0-SNAPSHOT</version>
 * <configuration>
 * <!-- maven 内置属性:项目根路径 -->
 * <basedir>${basedir}</basedir>
 * <scanPackage>com.georgeinfo.dao.model</scanPackage>
 * </configuration>
 * </plugin>
 */
@Mojo(name = "mybatis-generator-fixer", defaultPhase = LifecyclePhase.PACKAGE)
public class MyBatisGeneratorFixerMojo extends AbstractMojo {

    /**
     * 计数器
     */
    private static int count = 0;

    @Parameter
    private String basedir;
    @Parameter
    private String scanPackage;

    public void execute() throws MojoExecutionException, MojoFailureException {
        System.out.println("basedir: " + basedir);
        System.out.println("============start count============");
        listDir(basedir);
        System.out.println("============ end count ============");
        System.out.println("============ count: " + count + "============");
        getLog().info("扫描包路径：" + scanPackage);
    }

    private void listDir(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("path is not exists!");
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                this.listDir(files[i].getAbsolutePath());
            }
        } else {
            String filePath = file.getAbsolutePath();
            String sp = scanPackage.replaceAll("[.]", "/");
            if (filePath.contains(sp) && filePath.endsWith(".java")) {
                System.out.println("即将重新生成文件：" + file.getAbsolutePath());
                fixMapperCode(filePath);
                count++;
            }
            getLog().info("filePath=" + filePath + "，scanPackage=" + scanPackage);
        }
    }

    private void fixMapperCode(String filePath) {
        String lineSeparator = System.getProperty("line.separator");
        Map<String, String> fileNameDefMap = new HashMap<>();
        StringBuilder codeContent = new StringBuilder();
        String content = "";
        try (BufferedReader br = Files.newBufferedReader(Paths.get(filePath))) {
            String str;
            while ((str = br.readLine()) != null) {
                if (str.trim().startsWith("public class") && str.trim().endsWith("{")) {
                    //增加变量占位符
                    str = str + lineSeparator + "${fileNameDef}";
                } else {
                    if (str.trim().startsWith("addCriterion(") &&
                            str.trim().contains(",")) {
                        String fileName = extractFileName(str);
                        String fileNameDef = fileNameDefMap.get(fileName);
                        String fnd = fileName + "Field";
                        if (fileNameDef == null) {
                            fileNameDef = "    private static String " + fnd + " =\"" + fileName + "\";" + lineSeparator;
                            fileNameDefMap.put(fileName, fileNameDef);
                        }

                        //替换重复字符串为变量
                        str = str.replace("\"" + fileName + "\"", fnd);
                    } else {
                        if (str.trim().contains("oredCriteria.size() == 0")) {
                            str = str.replace("oredCriteria.size() == 0", "oredCriteria.isEmpty()");
                        }
                    }
                }

                codeContent.append(str).append(lineSeparator);
            }

            //把提取出来的变量放入指定标签处
            StringBuilder defStr = new StringBuilder();
            for (String def : fileNameDefMap.values()) {
                defStr.append(def).append(lineSeparator);
            }

            content = codeContent.toString().replace("${fileNameDef}", defStr.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //重新生成文本文件
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), StandardCharsets.UTF_8)) {
            writer.write(content);
            getLog().info("新内容:" + content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String extractFileName(String str) {
        char[] chars = str.toCharArray();
        List<Integer> keyIndexList = new ArrayList<>();
        int i = 0;
        for (char c : chars) {
            if (c == '"') {
                keyIndexList.add(i);
            }
            i++;
        }
        int start = keyIndexList.size() - 2;
        int end = keyIndexList.size() - 1;

        String fileName = str.substring(keyIndexList.get(start) + 1, keyIndexList.get(end));
        return fileName;
    }

    public static void main(String[] args) {
        MyBatisGeneratorFixerMojo mojo = new MyBatisGeneratorFixerMojo();
        String source = "       addCriterion(\"user_id =\", value, \"userId\");";
        System.out.println(mojo.extractFileName(source));
    }
}
