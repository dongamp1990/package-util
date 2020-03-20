package demo;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import javax.swing.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Hello world!
 */
public class PackageMainApp {

    private static String BASE_PATH;
    private static String[] PROJECTS;
    private static String PROJECT_PATH;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static Map<String, String> fileNameMap = new HashMap<>();
    private static String parentId;
    private static String childId;
    private Properties properties;
    private static String jars_copy_to_lib;
    private static boolean exclusion_properties;
    private static boolean isMultiProject = false;
    private static boolean exclusion_other_project = false;
    private static String branchName;
    private static JTextArea logTextArea;
    public static String LINE = "\r\n";
    private static String pathPrefix;

    public PackageMainApp(Properties properties, JTextArea logTextArea2) {
        this.properties = properties;
        logTextArea = logTextArea2;
        init();
    }

    private void init(){
        try {
            BASE_PATH = properties.getProperty("output_path");
            if (BASE_PATH != null) {
                String c = BASE_PATH.substring(BASE_PATH.length() - 1, BASE_PATH.length());
                if (c != "/" || c != "\\") {
                    BASE_PATH += "/";
                }
            }
            //多项目一起打包
            String multiProjdcts = properties.getProperty("multi_projects");
            String projectPath = properties.getProperty("project_path");
            //如果未空，把project_path最后的路径作为项目名
            if (multiProjdcts == null || multiProjdcts.isEmpty()) {
                int lastIndex = projectPath.lastIndexOf("/");
                if (lastIndex < 0) {
                    lastIndex = projectPath.lastIndexOf("//");
                }
                if (lastIndex < 0) {
                    lastIndex = projectPath.lastIndexOf("\\");
                }
                String projectName = projectPath.substring(lastIndex + 1);
                PROJECTS = new String[]{projectName};
                PROJECT_PATH = projectPath.replace(projectName, "");
                isMultiProject = false;
            }else {
                PROJECTS = multiProjdcts.split(",");
                PROJECT_PATH = projectPath;
                if (!PROJECT_PATH.endsWith("/") && !PROJECT_PATH.endsWith("\\")) {
                    PROJECT_PATH += "/";
                }
                isMultiProject = true;
            }
            childId = properties.getProperty("previous_commit_id");
            parentId = properties.getProperty("last_commit_id");
            jars_copy_to_lib = properties.getProperty("jars_copy_to_lib");
            exclusion_properties = properties.getProperty("exclusion_properties") == null ? false : Boolean.valueOf(properties.getProperty("exclusion_properties"));
            exclusion_other_project = properties.getProperty("exclusion_other_project") == null ? false : Boolean.valueOf(properties.getProperty("exclusion_other_project"));

            int index = projectPath.lastIndexOf("/") > 0 ? projectPath.lastIndexOf("/") : projectPath.lastIndexOf("\\");
            pathPrefix = projectPath.substring(index + 1, projectPath.length());
            run();
        } catch (Exception e) {
            System.out.println("读取配置文件错误");
            logTextArea.append("读取配置文件错误");
            e.printStackTrace();
        }
    }

    public void run() throws Exception {
       List<String> files = diffMethod(PROJECT_PATH, childId, parentId);
       if (files == null || files.isEmpty()) {
           System.out.println("没有更改的文件");
           logTextArea.append("没有更改的文件" + LINE);
           return;
       }
       copyFile(files);
    }
/*
    public static void packageFile() throws IOException, URISyntaxException {
        fileNameMap.put("fgw-bank", "fgw-bank");
        fileNameMap.put("fgw-web", "vz-fgw");
        for (String p : PROJECTS) {
            System.out.println("=======开始处理 " + p + " =======");
            logTextArea.append("=======开始处理 " + p + " =======" + LINE);
            preCreatePath(BASE_PATH + p);
            ZIPUtil.compress(BASE_PATH + p + "/WEB-INF", BASE_PATH + fileNameMap.get(p) + "-" + dateFormat.format(new Date()) + ".zip");
            System.out.println("=======处理 " + p + " 结束=======");
            logTextArea.append("=======处理 " + p + " 结束=======" + LINE);
        }
    }*/

    public static void main(String[] args) throws Exception {
//        test();

        /*Process process = Runtime.getRuntime().exec("javac");
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));
        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "GBK"));
        String line;
        System.out.println("OUTPUT");
        while ((line = stdoutReader.readLine()) != null) {
            System.out.println(line);
        }
        System.out.println("ERROR");
        while ((line = stderrReader.readLine()) != null) {
            System.out.println(line);
        }
        int exitVal = process.waitFor();
        System.out.println("process exit value is " + exitVal);*/

    }

    public static void preCreatePath(String path) throws URISyntaxException, IOException {
        //创建WEB-INF目录
//        Path classesPath = Paths.get(project + CLASSES_PATH);

        String targetPath = BASE_PATH + (path.length() > 2 ? path : pathPrefix);
        Path classesPath = Paths.get(targetPath);
        boolean isExists = Files.exists(Paths.get(targetPath));
        if (isExists) {
            //删除
            deleteFile(classesPath);
        }
        Files.createDirectories(classesPath);
    }

    private static void deleteFile(Path classesPath) throws IOException {
        File file = new File(classesPath.toString());
        File[] list = file.listFiles();
        if (list == null || list.length <= 0) return;
        for (File f : list) {
            if (f.isDirectory()) {
                deleteFile(Paths.get(f.getPath()));
            }
            Files.delete(Paths.get(f.getPath()));
        }
    }

    private static String TARGET_CLASSES_PATH = "/target/classes/";
    private static String CLASSES_PATH = "/WEB-INF/classes/";
    private static String TEMPLATES_PATH = "/WEB-INF/templates/";
    private static String RESOURCES_PATH = "src/main/resources/";
    private static String WEBAPP_PATH = "src/main/webapp/";
    private static String JAVA_PATH = "src/main/java/";
    private static String LIB_PATH = "/WEB-INF/lib/";

    public static void copyFile(List<String> files) throws Exception {
        boolean hasCoped = false;
        for (String project : PROJECTS) {
            System.out.println("=======开始处理 " + (project.length() > 2 ? project : pathPrefix) + " =======");
            logTextArea.append("=======开始处理 " + (project.length() > 2 ? project : pathPrefix) + " =======" + LINE);
            //初始化目录
            preCreatePath(project);
            for (String fileName : files) {
                //跳过不是当前项目的文件
                if (!fileName.contains(project) && exclusion_other_project && isMultiProject) continue;
                hasCoped = true;
                String formFile = null;
                String toFile = null;
                if (fileName.contains("src/main/java") && fileName.endsWith(".java")) {
                    int index = fileName.indexOf(project);
                    String fName = fileName.substring(fileName.lastIndexOf("/") + 1).replace(".java", ".class");
                    //xx.class
                    String fileClassName = fileName.replace(".java", ".class");
//                    String classPath = fileClassName.substring(0, fileClassName.lastIndexOf("/"));
                    String classPath = fileClassName;
//                    String classPath = fileName.substring(index == 0 ? index + project.length() : index);
                    classPath = classPath.replace(JAVA_PATH, "");
                    classPath = classPath.replace(".java", ".class");
                    classPath = classPath.replace(project + "/", "");
                    String cPath = TARGET_CLASSES_PATH + classPath;
                    Path targetPath = Paths.get(BASE_PATH + project + CLASSES_PATH + classPath.replace(fName, ""));
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                    if (fileName.contains(project))
                        formFile = PROJECT_PATH + project + cPath;
                    if (!fileName.contains(project))
                        formFile = PROJECT_PATH + project + cPath;
                    toFile = BASE_PATH + project + CLASSES_PATH + classPath;
                } else if (fileName.contains("src/main/resources")) {
                    //resources路径
                    //过滤properties
                    if (exclusion_properties && fileName.contains(".properties")) {
                        System.out.println("过滤配置文件: " + fileName);
                        logTextArea.append("过滤配置文件: " + fileName + LINE);
                        continue;
                    }
                    String fName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    String classPath = fileName;
                    classPath = classPath.replace(RESOURCES_PATH, "");
                    classPath = classPath.replace(project + "/", "");
                    Path targetPath = Paths.get(BASE_PATH + project + "/"  + CLASSES_PATH +  classPath.replace(fName, ""));
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                    if (fileName.contains(project)) {
                        formFile = PROJECT_PATH + fileName;
                    }else {
                        formFile = PROJECT_PATH + project + "/" + fileName;
                    }
                    toFile = targetPath + "/" + fName;
                } /*else if (fileName.contains("/WEB-INF")) {
                    //WEB-INF
                    String fName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    String path = fileName.replace("src/main/webapp/WEB-INF/templates/", TEMPLATES_PATH);
                    Path targetPath = Paths.get(BASE_PATH + fileName.replace("src/main/webapp/WEB-INF/templates", TEMPLATES_PATH).replace(fName, ""));
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                    formFile = PROJECT_PATH + fileName.replace(RESOURCES_PATH, TEMPLATES_PATH);
                    toFile = targetPath + "/" + fName;
                } */
                else if (fileName.contains("/webapp")) {
                    //WEB-INF
                    String fName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    String classPath = fileName;
//                    String classPath = fileName.substring(index == 0 ? index + project.length() : index);
                    classPath = classPath.replace(WEBAPP_PATH, "");
                    if (!"/".equals(project) && !"\\".equals(project)) {
                        classPath = classPath.replace(project, "");
                    }
                    String path = fileName.replace("src/main/webapp/", "");
//                    Path targetPath = Paths.get(BASE_PATH + project + "/" + path.replace(fName, ""));
                    Path targetPath = Paths.get(BASE_PATH + project + (classPath.startsWith("/") ? "" : "/") + classPath.replace(fName, ""));
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                    if (fileName.contains(project))
                        formFile = PROJECT_PATH + fileName;
                    else
                        formFile = PROJECT_PATH + project + "/" + fileName;
                    toFile = targetPath + "/" + fName;
                    /*Files.copy(Paths.get(formFile), Paths.get(toFile), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("复制" + formFile);*/
                }
                if (formFile != null && toFile != null) {
                    System.out.println("复制" + formFile +" -----> " + toFile);
                    logTextArea.append("复制" + formFile +" -----> " + toFile + LINE);
                    try {
                        Files.copy(Paths.get(formFile), Paths.get(toFile), StandardCopyOption.REPLACE_EXISTING);
                    }catch (FileNotFoundException e) {
                        System.out.println("FileNotFound : " +e.getMessage());
                        logTextArea.append("FileNotFound : " +e.getMessage() + toFile + LINE);
                    }catch (NoSuchFileException e) {
                        System.out.println("NoSuchFile : " + e.getMessage());
                        logTextArea.append("NoSuchFile : " + e.getMessage() + toFile + LINE);
                    }
                }
            }
            //复制jar包
            if (jars_copy_to_lib != null && jars_copy_to_lib.length() > 0) {
                for (String jarFile : jars_copy_to_lib.split(",")) {
                    try {
                        String filePath = PROJECT_PATH + "/" + project + "/target/" + (project.length() > 2 ? project : pathPrefix) + LIB_PATH + jarFile;
                        String targetPathStr = BASE_PATH + (project.length() > 2 ? project : pathPrefix) + "/WEB-INF/lib/";
                        Path targetPath = Paths.get(targetPathStr);
                        if (Files.exists(Paths.get(filePath))) {
                            if (!Files.exists(targetPath)) {
                                Files.createDirectories(targetPath);
                            }
                            String toFile = BASE_PATH + project + "/WEB-INF/lib/" + jarFile;
                            System.out.println("复制" + filePath + " -----> " + toFile);
                            logTextArea.append("复制" + filePath + " -----> " + toFile + toFile + LINE);
                            Files.copy(Paths.get(filePath), Paths.get(toFile), StandardCopyOption.REPLACE_EXISTING);
                        } else {
                            System.out.println("复制JAR包：" + filePath + "文件不存在");
                            logTextArea.append("复制JAR包：" + filePath + "文件不存在" + LINE);
                        }
                    }catch (Exception e) {
                        System.out.println("NoSuchFile : " + e.getMessage());
                        logTextArea.append("NoSuchFile : " + e.getMessage() + LINE);
                    }
                }
            }
            if (hasCoped) {
                String destPath = BASE_PATH + (project.length() > 2 ? project : pathPrefix) + "-"
                        + dateFormat.format(new Date()) + "_" +branchName + "-" + childId+ "-" + parentId + ".zip";
                System.out.println(project +  "压缩包：" + destPath);
                logTextArea.append(project +  "压缩包：" + destPath + LINE);
                /*for (File f : path.listFiles()) {
                    ZIPUtil.compress(f.getPath(), destPath);
                }*/
                ZIPUtil.compress( BASE_PATH + (project.length() > 2 ? project : pathPrefix), destPath);
            }
            System.out.println("=======处理 " + project + " 结束=======");
            logTextArea.append("=======处理 " + project + " 结束=======" + LINE);
        }
    }

    public static List<String> diffMethod(String gitPath, String child, String parent) throws IOException {
        Git git = null;
        int count = 0;
        do {
            try {
                git = Git.open(new File(gitPath));
            }catch (RepositoryNotFoundException e) {
                if (count == 0) {
                    gitPath += PROJECTS[0];
                    count ++;
                }
            }
        }while (git == null && count <= 1);
        if (git == null) {
            System.out.println("项目路径不存在git项目");
        }
        Repository repository = git.getRepository();
        branchName = repository.getBranch();
        ObjectReader reader = repository.newObjectReader();
        CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
        List<String> fileList = new ArrayList<>();
        try {
            ObjectId old = repository.resolve(child + "^{tree}");
            ObjectId head = repository.resolve(parent + "^{tree}");
            if (old == null || head == null) {
                return null;
            }
            oldTreeIter.reset(reader, old);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head);
            List<DiffEntry> diffs = git.diff()
                    .setNewTree(newTreeIter)
                    .setOldTree(oldTreeIter)
                    .call();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter df = new DiffFormatter(out);
            df.setRepository(git.getRepository());
            for (DiffEntry diffEntry : diffs) {
                if (diffEntry.getChangeType() == DiffEntry.ChangeType.DELETE) {
                    continue;
                }
                String path = diffEntry.getPath(DiffEntry.Side.NEW);
//                df.format(diffEntry);
//                String diffText = out.toString("UTF-8");
//                System.out.println(diffText);
                //  out.reset();
                fileList.add(path);
            }
            return fileList;
        } catch (IncorrectObjectTypeException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return null;
    }
}
