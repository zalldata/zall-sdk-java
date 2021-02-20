package cn.zalldigital.consumer;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class LoggerConsumer implements Consumer{

    /**
     * 日志切割规则
     */
    public enum LogrotateEnum {

        /** 按日切分 */
        DAILY,

        /**  按小时切分 */
        HOURLY
    }

    /**
     * LoggerConsumer 的配置信息
     */
    public static class Config {
        String logPath;
        LogrotateEnum logrotateEnum = LogrotateEnum.DAILY;
        String lockFileName;
        String fileNamePrefix;
        int fileSize = 0;
        int bufferSize = 8192;
        /**
         * 创建指定日志存放路径的 LoggerConsumer 配置
         *
         * @param logPath   日志存放路径
         */
        public Config(String logPath) {
            this.logPath = logPath;
        }

        /**
         * 创建指定日志存放路径和日志大小的 LoggerConsumer 配置
         *
         * @param logPath       日志存放路径
         * @param fileSize      日志大小, 单位 MB, 默认为无限大
         */
        public Config(String logPath, int fileSize) {
            this.logPath = logPath;
            this.fileSize = fileSize;
        }

        /**
         * 设置日志切分模式
         *
         * @param logrotateEnum 日志切分模式
         */
        public void setLogrotateEnum(LogrotateEnum logrotateEnum) {
            this.logrotateEnum = logrotateEnum;
        }

        /**
         * 设置日志大小
         *
         * @param fileSize 日志大小，单位 MB
         */
        public void setFileSize(int fileSize) {
            this.fileSize = fileSize;
        }

        public void setLockFile(String lockFileName) {
            this.lockFileName = lockFileName;
        }

        /**
         * 设置缓冲区容量, 当超过该容量时会触发 flush 动作
         *
         * @param bufferSize 缓冲区大小，单位 byte.
         */
        public void setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        /**
         * 设置用户名前缀
         *
         * @param fileNamePrefix 文件前缀名
         */
        public void setFilenamePrefix(String fileNamePrefix) {
            this.fileNamePrefix = fileNamePrefix;
        }
    }

    private final String fileName;
    private final String lockFileName;
    private final int bufferSize;
    private final int fileSize;
    private final SimpleDateFormat sdf;
    private final StringBuffer messageBuffer = new StringBuffer();

    private LoggerFileWriter loggerWriter;

    public LoggerConsumer(final Config config) {
        String fileNamePrefix = config.fileNamePrefix == null ? config.logPath +  File.separator  :  config.logPath +  File.separator + config.fileNamePrefix + ".";
        this.fileName = fileNamePrefix + "log.";
        this.fileSize = config.fileSize;
        this.lockFileName = config.lockFileName;
        this.bufferSize = config.bufferSize;

        final String dataFormat = config.logrotateEnum == LogrotateEnum.HOURLY ? "yyyy-MM-dd-HH" : "yyyy-MM-dd";
        this.sdf = new SimpleDateFormat(dataFormat);
    }


    public LoggerConsumer(final String logPath) {
        this(new Config(logPath));
    }


    public LoggerConsumer(final String logPath, int fileSize) {
        this(new Config(logPath, fileSize));
    }


    @Override
    public void send(Map<String, Object> message) {
        try {
            messageBuffer.append(new Gson().toJson(message));
            messageBuffer.append("\n");
        } catch (JsonIOException e) {
            throw new RuntimeException("Failed to add data", e);
        }

        if (messageBuffer.length() >= bufferSize) {
            this.flush();
        }
    }

    @Override
    public synchronized void flush() {
        if (messageBuffer.length() == 0) {
            return;
        }

        String fileName = getFileName();
        if (loggerWriter != null && !loggerWriter.getFileName().equals(fileName)) {
            LoggerFileWriter.removeInstance(loggerWriter);
            loggerWriter = null;
        }

        if (loggerWriter == null) {
            try {
                loggerWriter = LoggerFileWriter.getInstance(fileName, lockFileName);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        if (loggerWriter.write(messageBuffer)) {
            messageBuffer.setLength(0);
        }
    }

    private String getFileName() {
        String resultPrefix = fileName + sdf.format(new Date()) + "_";
        int count = 0;
        String result = resultPrefix + count;
        if (fileSize > 0) {
            File target = new File(result);
            while (target.exists()) {
                if ((target.length() / (1024 * 1024)) < fileSize) {
                    break;
                }
                result = resultPrefix + (++count);
                target = new File(result);
            }
        }
        return result;
    }

    @Override
    public synchronized void close() {
        this.flush();
        if (loggerWriter != null) {
            LoggerFileWriter.removeInstance(loggerWriter);
            loggerWriter = null;
        }
    }

    private static class LoggerFileWriter {

        private final String fileName;
        private final FileOutputStream outputStream;
        private final FileOutputStream lockStream;
        private int refCount;

        private static final Map<String, LoggerFileWriter> LOGGER_FILE_WRITER_MAP = new HashMap<>();

        static LoggerFileWriter getInstance(final String fileName, final String lockFileName) throws FileNotFoundException {
            synchronized (LOGGER_FILE_WRITER_MAP) {
                if (!LOGGER_FILE_WRITER_MAP.containsKey(fileName)) {
                    LOGGER_FILE_WRITER_MAP.put(fileName, new LoggerFileWriter(fileName, lockFileName));
                }
                LoggerFileWriter writer = LOGGER_FILE_WRITER_MAP.get(fileName);
                writer.refCount++;
                return writer;
            }
        }

        static void removeInstance(final LoggerFileWriter writer) {
            synchronized (LOGGER_FILE_WRITER_MAP) {
                writer.refCount--;
                if (writer.refCount == 0) {
                    writer.close();
                    LOGGER_FILE_WRITER_MAP.remove(writer.fileName);
                }
            }
        }

        private LoggerFileWriter(final String fileName, final String lockFileName) throws FileNotFoundException {
            this.outputStream = new FileOutputStream(fileName, true);
            if (lockFileName != null) {
                this.lockStream = new FileOutputStream(lockFileName, true);
            } else {
                this.lockStream = this.outputStream;
            }

            this.fileName = fileName;
            this.refCount = 0;
        }

        private void close() {
            try {
                outputStream.close();
            } catch (Exception e) {
                throw new RuntimeException("fail to close tga outputStream.", e);
            }
        }

        String getFileName() {
            return this.fileName;
        }

        boolean write(final StringBuffer sb) {
            synchronized (this.lockStream) {
                FileLock lock = null;
                try {
                    final FileChannel channel = lockStream.getChannel();
                    lock = channel.lock(0, Long.MAX_VALUE, false);
                    outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    throw new RuntimeException("failed to write tga file.", e);
                } finally {
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e) {
                            throw new RuntimeException("failed to release tga file lock.", e);
                        }
                    }
                }
            }
            return true;
        }
    }
}
