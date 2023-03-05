package com.danxiaocampus.callback.util;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;

import java.nio.file.Paths;
import java.util.HashMap;

/**
 * 将数据库表生成Mybatis PLus所需的Java代码
 *
 * @author 黄磊
 * @since 2022/5/13
 **/
public class GenerateMysql2CodeUtil {
    /*数据库信息配置*/
    private static final String URL = "jdbc:mysql://1.117.165.232:3306/campus-test?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&useSSL=true";
    private static final String USERNAME = "campus-test";
    private static final String PASSWORD = "BhfzARKkEs6iNpA3";

    /*包配置 PackageConfig*/
    private static final HashMap<OutputFile, String> PATH_INFO = new HashMap<>();
    private static final String CLASS_PATH = "com.danxiaocampus.api";

    /*模板配置 TemplateConfig*/
    // 注意：Mybatis Plus是通过类路径读取模板的，模板需要放在resources下面，然后通过例如后面的链接来获取的"/mybatisplus/template/entity.java.vm"
    private static final String ENTITY_TEMPLATE = "/other/mybatisplus/entity.java.vm";

    static {
        String basePath = "./src/main";
        String classPath = CLASS_PATH.replace('.', '/');
        String javaFileBasePath = Paths.get(basePath, "java", classPath).toString();
        String xmlFilePath = Paths.get(basePath, "resources").toString();
        PATH_INFO.put(OutputFile.controller, Paths.get(javaFileBasePath, "controller").normalize().toAbsolutePath().toString());
        PATH_INFO.put(OutputFile.mapper, Paths.get(javaFileBasePath, "dao").normalize().toAbsolutePath().toString());
        PATH_INFO.put(OutputFile.entity, Paths.get(javaFileBasePath, "pojo").normalize().toAbsolutePath().toString());
        PATH_INFO.put(OutputFile.service, Paths.get(javaFileBasePath, "manager").normalize().toAbsolutePath().toString());
        PATH_INFO.put(OutputFile.serviceImpl, Paths.get(javaFileBasePath, "manager", "impl").normalize().toAbsolutePath().toString());
        PATH_INFO.put(OutputFile.xml, Paths.get(xmlFilePath, "mapper").normalize().toAbsolutePath().toString());
        PATH_INFO.put(OutputFile.other, Paths.get(xmlFilePath, "other").normalize().toAbsolutePath().toString());
    }

    public static void main(String[] args) {
        FastAutoGenerator.create(URL, USERNAME, PASSWORD)
                .globalConfig(builder -> builder.author("黄磊")
                            .outputDir(Paths.get(".").toString())
                            .disableOpenDir())
                .templateConfig(builder -> {
                    if (ENTITY_TEMPLATE != null && ENTITY_TEMPLATE.length() >= 0) {
                        builder.entity(ENTITY_TEMPLATE);
                    }
                })
                .packageConfig(builder -> {
                    builder.parent(CLASS_PATH) // 设置父包名
                            .mapper("dao")
                            .entity("pojo")
                            .service("manager")
                            .serviceImpl("manager")
                            .pathInfo(PATH_INFO); // 设置mapperXml生成路径
                })
                .strategyConfig(builder -> {
                    builder.entityBuilder()
                            .enableColumnConstant()
                            .enableLombok()
                            .enableRemoveIsPrefix()
                            .enableTableFieldAnnotation()
                            .versionColumnName("version")
                            .versionPropertyName("version")
                            .logicDeleteColumnName("is_deleted")
                            .logicDeletePropertyName("deleted")
                            .addIgnoreColumns("create_time", "update_time")
                            .idType(IdType.AUTO)
                            .formatFileName("%sDO")
                            .controllerBuilder()
                            .enableRestStyle()
                            .mapperBuilder()
                            .enableMapperAnnotation()
                            .enableBaseResultMap()
                            .enableBaseColumnList()
                            .formatMapperFileName("%sDao")
                            .serviceBuilder()
                            .formatServiceFileName("%sManager")
                            .formatServiceImplFileName("%sManagerImp");
                })
                .execute();
    }
}
