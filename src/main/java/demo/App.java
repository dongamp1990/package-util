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

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Hello world!
 */
public class App {

//    private static String BASE_PATH = "C:/Users/vzoom/Desktop/update/";
    private static String BASE_PATH;
    private static String[] PROJECTS;
    private static String PROJECT_PATH;
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private static Map<String, String> fileNameMap = new HashMap<>();
    private static String parentId;
    private static String childId;
    private static Properties properties;
    private static String jars_copy_to_lib;
    private static boolean exclusion_properties;
    private static boolean isMultiProjdct = false;
    private static String branchName;
    static {
        properties = new Properties();
        try (InputStream ins = new FileInputStream("config11.properties")){
            properties.load(ins);
            BASE_PATH = properties.getProperty("output_path");
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
            }else {
                PROJECTS = multiProjdcts.split(",");
                PROJECT_PATH = projectPath;
                isMultiProjdct = true;
            }
            childId = properties.getProperty("previous_commit_id");
            parentId = properties.getProperty("last_commit_id");
            jars_copy_to_lib = properties.getProperty("jars_copy_to_lib");
            exclusion_properties = properties.getProperty("exclusion_properties") == null ? false : Boolean.valueOf(properties.getProperty("exclusion_properties"));
        } catch (Exception e) {
            System.out.println("读取配置文件错误");
            e.printStackTrace();
        }
    }

    public static void test() throws Exception {
//        Archiver archiver = new Archiver();
//        archiver.create("zip", new File("E:\\test.zip"), new File("G:\\MyFile\\gitRepos\\maxfun_engine\\taskManager"));
       List<String> files = diffMethod(PROJECT_PATH, childId, parentId);
       if (files == null || files.isEmpty()) {
           System.out.println("没有更改的文件");
           return;
       }
       copyFile(files);
    }

    public static void packageFile() throws IOException, URISyntaxException {
        fileNameMap.put("fgw-bank", "fgw-bank");
        fileNameMap.put("fgw-web", "vz-fgw");
        for (String p : PROJECTS) {
            System.out.println("=======开始处理 " + p + " =======");
            preCreatePath(BASE_PATH + p);
            readFile(p, BASE_PATH + p);
            ZIPUtil.compress(BASE_PATH + p + "/WEB-INF", BASE_PATH + fileNameMap.get(p) + "-" + dateFormat.format(new Date()) + ".zip");
            System.out.println("=======处理 " + p + " 结束=======");
        }
    }

    public static void main(String[] args) throws Exception {
//        test();
        final Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("T:" + System.currentTimeMillis());
                    Thread.sleep(5000);
                    System.out.println("T:" + System.currentTimeMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println(System.currentTimeMillis());
                    t.join(3000);
                    Thread.sleep(5000);
                    System.out.println(System.currentTimeMillis());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t2.start();
        t.start();

        System.out.println("main end");
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

    public static void preCreatePath(String project) throws URISyntaxException, IOException {
        //创建WEB-INF目录
//        Path classesPath = Paths.get(project + CLASSES_PATH);
        Path classesPath = Paths.get(project);
        boolean isExists = Files.exists(Paths.get(project));
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
    private static String RESOURCES_PATH = "/src/main/resources/";
    private static String JAVA_PATH = "/src/main/java/";
    private static String LIB_PATH = "/WEB-INF/lib/";

    public static void copyFile(List<String> files) throws Exception {
        boolean hasCoped = false;
        for (String project : PROJECTS) {
            System.out.println("=======开始处理 " + project + " =======");
            //初始化目录
            preCreatePath(BASE_PATH + project);
            for (String fileName : files) {
                //跳过不是当前项目的文件
                if (!fileName.contains(project)) continue;
                hasCoped = true;
                String formFile = null;
                String toFile = null;
                if (fileName.contains("src/main/java") && fileName.endsWith(".java")) {
                    int index = fileName.indexOf(project);
                    String fName = fileName.substring(fileName.lastIndexOf("/") + 1).replace(".java", ".class");
                    //xx.class
                    String fileClassName = fileName.replace(".java", ".class");
                    String classPath = fileName.substring(index == 0 ? index + project.length() : index);
                    classPath = classPath.replace(JAVA_PATH, "");
                    classPath = classPath.replace(".java", ".class");
                    classPath = classPath.replace(project + "/", "");
                    String cPath = project + TARGET_CLASSES_PATH + classPath;
                    Path targetPath = Paths.get(BASE_PATH + project + CLASSES_PATH + classPath.replace(fName, ""));
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                    formFile = PROJECT_PATH + cPath;
                    toFile = BASE_PATH + project + CLASSES_PATH + classPath;
                } else if (fileName.contains("src/main/resources")) {
                    //resources路径
                    //过滤properties
                    if (exclusion_properties && fileName.contains(".properties")) {
                        System.out.println("过滤配置文件: " + fileName);
                        continue;
                    }
                    String fName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    Path targetPath = Paths.get(BASE_PATH + fileName.replace(RESOURCES_PATH, CLASSES_PATH).replace(fName, ""));
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                    formFile = PROJECT_PATH + fileName.replace(RESOURCES_PATH, TARGET_CLASSES_PATH);
                    toFile = targetPath + "/" + fName;
                    /*Files.copy(Paths.get(formFile), Paths.get(toFile), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("复制" + formFile);*/
                } else if (fileName.contains("/src/main/webapp/WEB-INF")) {
                    //WEB-INF
                    String fName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    String path = fileName.replace("src/main/webapp/WEB-INF/templates/", TEMPLATES_PATH);
                    Path targetPath = Paths.get(BASE_PATH + fileName.replace("src/main/webapp/WEB-INF/templates", TEMPLATES_PATH).replace(fName, ""));
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                    formFile = PROJECT_PATH + fileName.replace(RESOURCES_PATH, TEMPLATES_PATH);
                    toFile = targetPath + "/" + fName;
                    /*Files.copy(Paths.get(formFile), Paths.get(toFile), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("复制" + formFile);*/
                }
                if (formFile != null && toFile != null) {
                    System.out.println("复制" + formFile +" -----> " + toFile);
                    try {
                        Files.copy(Paths.get(formFile), Paths.get(toFile), StandardCopyOption.REPLACE_EXISTING);
                    }catch (FileNotFoundException e) {
                    }catch (NoSuchFileException e) {
                    }
                }
            }
            //复制jar包
            if (jars_copy_to_lib != null && jars_copy_to_lib.length() > 0) {
                for (String jarFile : jars_copy_to_lib.split(",")) {
                    String filePath = PROJECT_PATH + project + "/target/" + project + LIB_PATH + jarFile;
                    String targetPathStr = BASE_PATH + project + "/WEB-INF/lib/";
                    Path targetPath = null;
                    if (Files.exists(Paths.get(filePath))) {
                        if (targetPath == null) {
                            targetPath = Paths.get(targetPathStr);
                        }
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                        String toFile = BASE_PATH + project + "/WEB-INF/lib/" + jarFile;
                        System.out.println("复制" + filePath + " -----> " + toFile);
                        Files.copy(Paths.get(filePath), Paths.get(toFile), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        System.out.println(filePath + "文件不存在");
                    }
                }
            }
            if (hasCoped) {
                String destPath = BASE_PATH + project + "-" + dateFormat.format(new Date()) + "_" +branchName + "-" + childId+ "-" + parentId + ".zip";
                System.out.println(project +  "压缩包：" + destPath);
                ZIPUtil.compress(BASE_PATH + project + "/WEB-INF", destPath);
            }
            System.out.println("=======处理 " + project + " 结束=======");
        }
    }

    public static void readFile(String project, String dirPath) {
        try {
            File file = new File(dirPath);
            File[] list = file.listFiles();
            for (File f : list) {
                if (f.isDirectory()) {
                    readFile(project, f.getPath());
                } else {
                    if (f.getPath().contains("src\\main\\java") && f.getName().endsWith(".java")) {
                        int index = f.getPath().indexOf(project);
                        String fileName = f.getName();
                        //xx.class
                        String fileClassName = fileName.replace(".java", ".class");
                        String classPath = f.getPath().substring(index);
                        classPath = classPath.replace("src\\main\\java\\", "");
                        classPath = classPath.replace(".java", ".class");
                        classPath = classPath.replace(project + "\\", "");
                        String cPath = project + "/target/classes/" + classPath;
//                        System.out.println(classPath);
//                        System.out.println(cPath);
                        String targetPathStr = BASE_PATH + project + "/WEB-INF/classes/" + classPath.replace(fileClassName, "").replace(project + "/", "");
                        Path targetPath = Paths.get(targetPathStr);
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                        String formFile = PROJECT_PATH + cPath;
                        String toFile = BASE_PATH + project + "/WEB-INF/classes/" + classPath;
//                        System.out.println("复制" + formFile + " -> " + toFile );
                        System.out.println("复制" + formFile);
                        Files.copy(Paths.get(formFile), Paths.get(toFile), StandardCopyOption.REPLACE_EXISTING);
                    } else if (f.getPath().contains("src\\main\\resources")) {
                        //resources路径
                        String fileName = f.getName();
                        String path = f.getPath().replace("src\\main\\resources", "WEB-INF/classes/");
                        Path targetPath = Paths.get(path.replace(fileName, ""));
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                        Files.copy(Paths.get(f.getPath()), Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("复制" + f.getPath());
                    } else if (f.getPath().contains("src\\main\\webapp\\WEB-INF")) {
                        //WEB-INF
                        String fileName = f.getName();
                        String path = f.getPath().replace("src\\main\\webapp\\WEB-INF\\templates", "WEB-INF/templates/");
                        Path targetPath = Paths.get(path.replace(fileName, ""));
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                        Files.copy(Paths.get(f.getPath()), Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("复制" + f.getPath());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
