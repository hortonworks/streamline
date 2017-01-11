package com.hortonworks.streamline.storage.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.Map;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class Utils {
    public static Map<String, Object> readStreamlineConfig(String configFilePath) throws IOException {
        ObjectMapper objectMapper = new YAMLMapper();
        return objectMapper.readValue(new File(configFilePath), Map.class);
    }

    /*
      This method is solely for the use of downloading the mysql java driver jar. 
      It will download the zip file from the provided url.
      Unzips the file and copies the jar into $STREAMLINE_HOME/bootstrap/lib and $STREAMLINE/libs.

      @params url mysql zip file URL
      @returns the mysql jar file name.
     */
    public static String downloadMysqlJarAndCopyToLibDir(String url, String fileNamePattern) throws IOException {
        System.out.println("Downloading mysql jar from url: " + url);
        String tmpFileName;
        try {
            URL downloadUrl = new URL(url);
            String[] pathSegments = downloadUrl.getPath().split("/");
            String fileName = pathSegments[pathSegments.length - 1];
            String tmpDir = System.getProperty("java.io.tmpdir");
            tmpFileName = tmpDir + File.separator + fileName;
            System.out.println("Downloading file " + fileName + " into " + tmpDir);
            ReadableByteChannel rbc = Channels.newChannel(downloadUrl.openStream());
            FileOutputStream fos = new FileOutputStream(tmpFileName);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch(IOException ie) {
            System.out.println("Failed to download the mysql driver from " + url);
            throw new IOException(ie);
        }

        System.out.println("Copying mysql libraries into StreamLine");
        String bootstrapDir = System.getProperty("streamline.bootstrap.dir");
        String bootstrapLibDir = bootstrapDir+ File.separator + "lib";
        String streamlineLibDir = bootstrapDir + File.separator + "../libs/";
        System.out.println("Unzipping downloaded mysql driver and copying into bootstrap lib dir");
        try {
            String mysqlJarFileName = Utils.copyFileFromZipToDir(tmpFileName, fileNamePattern, bootstrapLibDir);
            File bootstrapLibFile = new File(bootstrapLibDir + File.separator + mysqlJarFileName);
            File streamLineLibFile = new File(streamlineLibDir + File.separator + mysqlJarFileName);
            System.out.println("Copying file to StreamLine libs" + streamLineLibFile);
            Utils.copyFile(bootstrapLibFile, streamLineLibFile);
            return mysqlJarFileName;
        } catch (IOException ie) {
            System.out.println("Failed to copy mysql driver into " + bootstrapLibDir + " and " + streamlineLibDir);
        }
        return null;
    }

    public static String copyFileFromZipToDir(String zipFile, String fileNamePattern, String dir) throws IOException{
        ZipFile zip = new ZipFile(zipFile);
        Enumeration zipFileEntries = zip.entries();
        int BUFFER = 2048;
        while (zipFileEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
            String currentEntry = entry.getName();
            if (currentEntry.matches(fileNamePattern)) {
                String[] currentEntrySegments = currentEntry.split(File.separator);
                String matchedFileName = currentEntrySegments[currentEntrySegments.length - 1];
                File file = new File(dir + File.separator + matchedFileName);
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
                byte data[] = new byte[BUFFER];
                BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
                int currentByte;
                while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, currentByte);
                }
                dest.flush();
                dest.close();
                is.close();
                return matchedFileName;
            }
        }
        return null;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = new FileInputStream(sourceFile).getChannel();
        FileChannel destination = new FileInputStream(destFile).getChannel();

        if (destination != null && source != null) {
            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
        }
    }

    public static boolean fileExists(File dir, String regex) {
        File[] files = dir.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    return pathname.getName().matches(regex);
                }
            });
        return files.length == 1;
    }

    public static void loadJarIntoClasspath(File jarFile) throws Exception {
        try {
            URL url = jarFile.toURI().toURL();
            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, url);
        } catch(Exception e) {
            System.out.println("Failed to load " + jarFile + " into classpath.");
            throw new Exception(e);
        }
    }
}
