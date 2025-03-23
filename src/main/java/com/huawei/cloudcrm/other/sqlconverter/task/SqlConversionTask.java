package com.huawei.cloudcrm.other.sqlconverter.task;

import com.huawei.cloudcrm.other.sqlconverter.service.SqlConverterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Component
public class SqlConversionTask {
    private static final Logger logger = LoggerFactory.getLogger(SqlConversionTask.class);

    @Value("${file.input-dir}")
    private String inputDirPath;

    @Value("${file.output-dir}")
    private String outputDirPath;

    @Value("${file.completed-dir}")
    private String completedDirPath;

    @Resource
    private SqlConverterService sqlConverterService;

    // 每分钟执行一次
    @Scheduled(fixedRateString = "${task.schedule.fixed-rate}")
    public void executeSqlConversion() {
        try {
            // 创建目录（如果不存在）
            createDirectories();

            // 获取输入目录中的所有文件
            File inputDir = new File(inputDirPath);
            File[] inputFiles = inputDir.listFiles();
            if (inputFiles == null || inputFiles.length == 0) {
                logger.info("输入目录中没有文件，跳过执行。");
                return;
            }

            for (File inputFile : inputFiles) {
                if (inputFile.isFile()) {
                    long timestampInMillis = System.currentTimeMillis();
                    String inputFilePath = inputFile.getAbsolutePath();
                    String outputFilePath = outputDirPath + File.separator + inputFile.getName().replace(".sql", "") + "_output" + timestampInMillis + ".sql";
                    String completedFilePath = completedDirPath + File.separator + inputFile.getName().replace(".sql", String.valueOf(timestampInMillis)) + ".sql";

                    // 执行SQL转换
                    sqlConverterService.convertSqlFile(inputFilePath, outputFilePath);

                    // 将输入文件移动到已完成目录
                    moveFile(inputFile, new File(completedFilePath));

                    logger.info("文件处理完成: " + inputFile.getName());
                }
            }
        } catch (Exception e) {
            logger.error("SQL转换任务执行失败: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDirectories() throws Exception {
        // 创建输入目录
        File inputDir = new File(inputDirPath);
        if (!inputDir.exists()) {
            if (!inputDir.mkdirs()) {
                throw new Exception("无法创建输入目录: " + inputDirPath);
            }
        }

        // 创建输出目录
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw new Exception("无法创建输出目录: " + outputDirPath);
            }
        }

        // 创建已完成目录
        File completedDir = new File(completedDirPath);
        if (!completedDir.exists()) {
            if (!completedDir.mkdirs()) {
                throw new Exception("无法创建已完成目录: " + completedDirPath);
            }
        }
    }

    private void moveFile(File sourceFile, File targetFile) throws Exception {
        try {
            Files.move(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new Exception("无法移动文件: " + sourceFile.getAbsolutePath(), e);
        }
    }
}