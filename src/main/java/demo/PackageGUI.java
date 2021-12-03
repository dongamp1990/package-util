package demo;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * GUI窗口类
 *
 * @author dongamp1990@gmail.com
 */
public class PackageGUI extends JFrame implements WindowListener {
    JPanel jPanel1;
    JTextField gitPathTextField;
    JButton gitPathBtn;
    JTextField outPathTextField;
    JButton outPathBtn;
    JTextField previousCommitIdTextField;
    JTextField lastCommitIdTextField;
    JButton packageBtn;
    JRadioButton skipPropertiesYesBtn;
    JRadioButton skipPropertiesNoBtn;
    JTextField projectNamesTextField;
    JTextField copyJarsTextField;
    JTextArea logTextArea;
    JRadioButton skipOtherProjectYesBtn;
    JRadioButton skipOtherProjectNoBtn;
    JList preVersionList;
    JButton preSelBtn;
    JTextField propertiesFile;
    JButton propertiesBtn;
    JTextField projectPathTextField;
    JButton projectPathButton;
    ButtonGroup skipPropertiesButtonGroup;
    String skipPropertiesValue;
    String skipOtherProjectValue;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss");
    final DefaultListModel defaultListModel1 = new DefaultListModel();
    static JFrame frame;
    GitCommitDialog dialog = new GitCommitDialog(frame, this);

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        String configName = System.getProperty("user.dir") + File.separator + "config.properties";
        if (propertiesFile.getText().length() > 0) {
            configName = propertiesFile.getText();
        }
        int val = JOptionPane.showConfirmDialog(jPanel1, "是否保存配置到" + configName + " ？", "保存", JOptionPane.YES_NO_OPTION);
        if (val == 0) {
            System.out.println("save config " + configName);
            //保存使用的内容
            BufferedWriter bw = null;
            BufferedReader br = null;
            try {
                //如果文件不存在，即新增
                Path savePath = Paths.get(configName);
                boolean exists = Files.exists(savePath);
                if (!exists) {
                    //复制当前配置文件到
                    Files.copy(Paths.get("config.properties"), Paths.get(configName), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.copy(Paths.get(configName), Paths.get(configName + ".bak"), StandardCopyOption.REPLACE_EXISTING);
                }
                bw = new BufferedWriter(new FileWriter(configName));
                br = new BufferedReader(new FileReader(exists ? configName + ".bak" : "config.properties"));
                byte[] arr = new byte[1024];
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                String line = null;
                boolean hasPropertiesPath = false;
                while ((line = br.readLine()) != null) {
                    if (!line.startsWith("#") && line.length() > 0) {
                        //不包含注释的
                        String newLine = line;
                        if (line.indexOf("project_path=") != -1) {
                            newLine = "project_path=" + gitPathTextField.getText().replace("\\", "/");
                        }
                        if (line.indexOf("previous_commit_id=") != -1) {
                            newLine = "previous_commit_id=" + previousCommitIdTextField.getText();
                        }
                        if (line.indexOf("last_commit_id=") != -1) {
                            newLine = "last_commit_id=" + lastCommitIdTextField.getText();
                        }
                        if (line.indexOf("output_path=") != -1) {
                            newLine = "output_path=" + outPathTextField.getText().replace("\\", "/");
                        }
                        if (line.indexOf("multi_projects=") != -1) {
                            newLine = "multi_projects=" + projectNamesTextField.getText();
                        }
                        if (line.indexOf("jars_copy_to_lib=") != -1) {
                            newLine = "jars_copy_to_lib=" + copyJarsTextField.getText();
                        }
                        if (line.indexOf("exclusion_properties=") != -1 && skipPropertiesValue != null) {
                            newLine = "exclusion_properties=" + ("1".equals(skipPropertiesValue) ? "true" : "false");
                        }
                        if (line.indexOf("exclusion_other_project=") != -1 && skipOtherProjectValue != null) {
                            newLine = "exclusion_other_project=" + ("1".equals(skipOtherProjectValue) ? "true" : "false");
                        }
                        if (line.indexOf("config_path=") != -1) {
                            newLine = "config_path=" + propertiesFile.getText().replace("\\", "/") + "\r\n";
                        }
                        if (newLine != null)
                            bw.write(newLine + "\r\n");
                        if (newLine.contains("config_path=")) {
                            hasPropertiesPath = true;
                        }
                    } else {
                        bw.write(line + "\r\n");
                    }
                }
                if (!hasPropertiesPath && propertiesFile.getText().length() > 0) {
                    bw.write("config_path=" + propertiesFile.getText().replace("\\", "/") + "\r\n");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (bw != null)
                        bw.close();
                    if (br != null)
                        br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    public PackageGUI() {
        addWindowListener(this);
        gitPathBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("open")) {
                    JFileChooser jf = null;
                    if (gitPathTextField.getText() != null) {
                        jf = new JFileChooser(gitPathTextField.getText());
                    } else {
                        jf = new JFileChooser();
                    }
                    jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    jf.showOpenDialog(jPanel1);//显示打开的文件对话框
                    File f = jf.getSelectedFile();//使用文件类获取选择器选择的文件
                    if (f != null) {
                        gitPathTextField.setText(f.getAbsolutePath());
                        dialog.loadGit();
                    }
                }
            }

        });
        outPathBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("open")) {
                    JFileChooser jf = null;
                    if (outPathTextField.getText() != null) {
                        jf = new JFileChooser(outPathTextField.getText());
                    } else {
                        jf = new JFileChooser();
                    }
                    jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    jf.showOpenDialog(jPanel1);//显示打开的文件对话框
                    File f = jf.getSelectedFile();//使用文件类获取选择器选择的文件
                    if (f != null) {
                        outPathTextField.setText(f.getAbsolutePath());
                    }
                }
            }
        });

        projectPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("open")) {
                    JFileChooser jf = null;
                    if (gitPathTextField.getText() != null) {
                        jf = new JFileChooser(gitPathTextField.getText());
                    } else {
                        jf = new JFileChooser();
                    }
                    jf.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    jf.showOpenDialog(jPanel1);//显示打开的文件对话框
                    File f = jf.getSelectedFile();//使用文件类获取选择器选择的文件
                    if (f != null) {
                        projectPathTextField.setText(f.getAbsolutePath());
                        //解析项目路径，填充到项目名称中
                        String path = f.getAbsolutePath();
                        if (!Files.exists(Paths.get(path + File.separator + "target"))) {
                            JOptionPane.showMessageDialog(jPanel1, "该路径下未找到target目录，请编译后再选择", "提示", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        String projectName = path.substring(path.lastIndexOf(File.separator) + 1);
                        projectNamesTextField.setText(projectName);
                    }
                }
            }
        });

        packageBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("submit")) {
                    logTextArea.setText("");
                    if (gitPathTextField.getText() == null || "".equalsIgnoreCase(gitPathTextField.getText())) {
                        JOptionPane.showMessageDialog(jPanel1, "请选择Git路径", "提示", JOptionPane.WARNING_MESSAGE);
                        gitPathTextField.setFocusable(true);
                        gitPathTextField.requestFocus();
                        return;
                    }
                    if (gitPathTextField.getText().length() > 0) {
                        dialog.loadGit();
                    }
                    if (outPathTextField.getText() == null || "".equalsIgnoreCase(outPathTextField.getText())) {
                        JOptionPane.showMessageDialog(jPanel1, "请选择输出路径", "提示", JOptionPane.WARNING_MESSAGE);
                        outPathTextField.setFocusable(true);
                        outPathTextField.requestFocus();
                        return;
                    }
                    if (projectPathTextField.getText() == null || "".equalsIgnoreCase(projectPathTextField.getText())) {
                        JOptionPane.showMessageDialog(jPanel1, "请选择项目路径", "提示", JOptionPane.WARNING_MESSAGE);
                        projectPathTextField.setFocusable(true);
                        projectPathTextField.requestFocus();
                        return;
                    }
                    if (lastCommitIdTextField.getText() == null || "".equalsIgnoreCase(lastCommitIdTextField.getText())) {
                        JOptionPane.showMessageDialog(jPanel1, "请选择本次提交ID", "提示", JOptionPane.WARNING_MESSAGE);
                        lastCommitIdTextField.setFocusable(true);
                        lastCommitIdTextField.requestFocus();
                        return;
                    }
                    if (previousCommitIdTextField.getText() == null || "".equalsIgnoreCase(previousCommitIdTextField.getText())) {
                        JOptionPane.showMessageDialog(jPanel1, "请选择上次提交ID", "提示", JOptionPane.WARNING_MESSAGE);
                        previousCommitIdTextField.setFocusable(true);
                        previousCommitIdTextField.requestFocus();
                        return;
                    }
                    Properties properties = new Properties();
                    properties.setProperty("output_path", outPathTextField.getText());
                    properties.setProperty("jars_copy_to_lib", copyJarsTextField.getText());
                    properties.setProperty("project_path", projectPathTextField.getText());
                    properties.setProperty("git_path", gitPathTextField.getText());
                    properties.setProperty("previous_commit_id", previousCommitIdTextField.getText());
                    properties.setProperty("last_commit_id", lastCommitIdTextField.getText());
                    properties.setProperty("multi_projects", projectNamesTextField.getText());
                    if (skipPropertiesValue == null) {
                        skipPropertiesValue = "1";
                    }
                    if (skipOtherProjectValue == null) {
                        if (projectNamesTextField.getText().split(",").length > 1) {
                            skipOtherProjectValue = "1";
                        } else {
                            skipOtherProjectValue = "0";
                        }
                    }
                    properties.setProperty("exclusion_other_project", "1".equals(skipPropertiesValue) + "");
                    properties.setProperty("exclusion_properties", "1".equals(skipPropertiesValue) + "");
                    logTextArea.setText("");
                    new PackageMainApp(properties, logTextArea);
                }
            }
        });

        gitPathBtn.setActionCommand("open");
        outPathBtn.setActionCommand("open");
        projectPathButton.setActionCommand("open");
        packageBtn.setActionCommand("submit");
        skipPropertiesYesBtn.setActionCommand("1");
        skipPropertiesYesBtn.addActionListener(new SkipPropertiesListener());
        skipPropertiesNoBtn.setActionCommand("0");
        skipPropertiesNoBtn.addActionListener(new SkipPropertiesListener());
        skipPropertiesButtonGroup = new ButtonGroup();
        skipPropertiesButtonGroup.add(skipPropertiesYesBtn);
        skipPropertiesButtonGroup.add(skipPropertiesNoBtn);
        logTextArea.setLineWrap(true);
        logTextArea.setEditable(true);
        logTextArea.setRows(20);
        logTextArea.setColumns(80);
        skipOtherProjectYesBtn.setActionCommand("1");
        skipOtherProjectYesBtn.addActionListener(new SkipOtherProjectListener());
        skipOtherProjectNoBtn.setActionCommand("0");
        skipOtherProjectNoBtn.addActionListener(new SkipOtherProjectListener());
        propertiesBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jf = null;
                if (propertiesFile.getText() != null && propertiesFile.getText().length() > 0) {
                    jf = new JFileChooser(propertiesFile.getText());
                } else {
                    jf = new JFileChooser(System.getProperty("user.dir"));
                }
                jf.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jf.showOpenDialog(jPanel1);//显示打开的文件对话框
                File f = jf.getSelectedFile();//使用文件类获取选择器选择的文件
                if (f != null) {
                    propertiesFile.setText(f.getAbsolutePath());
                    loadProperties(f.getAbsolutePath(), false);
                }
                /*if (propertiesFile.getText() != null && propertiesFile.getText().length() > 0) {
                    loadProperties(propertiesFile.getText(), false);
                }*/
            }
        });
        //加载配置文件
        loadProperties("config.properties", true);

        preSelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(true);
                dialog.setSize(600, 400);
                dialog.loadGit();
            }
        });
    }

    private void loadProperties(String filePath, boolean initLoad) {
        Properties properties = new Properties();
        try (InputStream ins = new FileInputStream(filePath)) {
            properties.load(ins);
            outPathTextField.setText(properties.getProperty("output_path"));
            gitPathTextField.setText(properties.getProperty("project_path"));
            //多项目一起打包
            projectNamesTextField.setText(properties.getProperty("multi_projects"));
            previousCommitIdTextField.setText(properties.getProperty("previous_commit_id"));
            lastCommitIdTextField.setText(properties.getProperty("last_commit_id"));
            copyJarsTextField.setText(properties.getProperty("jars_copy_to_lib"));
            boolean skipProp = properties.getProperty("exclusion_properties") == null
                    ? false : Boolean.valueOf(properties.getProperty("exclusion_properties"));
            if (skipProp) {
                skipPropertiesYesBtn.setSelected(true);
            } else {
                skipPropertiesYesBtn.setSelected(false);
                skipPropertiesNoBtn.setSelected(true);
            }
            boolean skipOtherProject = properties.getProperty("exclusion_other_project") == null
                    ? false : Boolean.valueOf(properties.getProperty("exclusion_other_project"));
            if (skipOtherProject) {
                skipOtherProjectYesBtn.setSelected(true);
            } else {
                skipOtherProjectNoBtn.setSelected(true);
            }
            if (initLoad) {
                String configPath = properties.getProperty("config_path");
                if (configPath != null)
                    propertiesFile.setText(configPath);
            }
            dialog.loadGit();
        } catch (Exception e) {
            logTextArea.append("读取配置文件错误" + PackageMainApp.LINE);
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement traceElement : e.getStackTrace()) {
                sb.append("\tat " + traceElement + PackageMainApp.LINE);
            }
            logTextArea.append(sb.toString());
        }
    }

    /*public static void main(String[] args) {
        JFrame frame = new JFrame("PackageGUI");
        PackageGUI gui = new PackageGUI();
        frame.setContentPane(gui.jPanel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }*/

    public static void main(String[] args) {
        try {
            PackageGUI gui = new PackageGUI();
            frame = gui;
            frame.setTitle("Git增量打包工具");
            frame.setContentPane(gui.jPanel1);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        jPanel1 = new JPanel();
        jPanel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(12, 3, new Insets(10, 10, 10, 10), -1, -1));
        jPanel1.setToolTipText("");
        jPanel1.putClientProperty("html.disable", Boolean.FALSE);
        jPanel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(-4473925)), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("上次提交ID");
        jPanel1.add(label1, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("最后提交ID");
        jPanel1.add(label2, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("输出路径");
        jPanel1.add(label3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outPathTextField = new JTextField();
        outPathTextField.setColumns(2);
        outPathTextField.setEditable(true);
        outPathTextField.setEnabled(true);
        outPathTextField.setHorizontalAlignment(2);
        jPanel1.add(outPathTextField, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(500, 30), new Dimension(608, 38), new Dimension(-1, 30), 0, false));
        outPathBtn = new JButton();
        outPathBtn.setText("选择");
        jPanel1.add(outPathBtn, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previousCommitIdTextField = new JTextField();
        previousCommitIdTextField.setEditable(true);
        previousCommitIdTextField.setEnabled(true);
        previousCommitIdTextField.setText("");
        jPanel1.add(previousCommitIdTextField, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(500, 30), new Dimension(608, 38), new Dimension(-1, 30), 0, false));
        lastCommitIdTextField = new JTextField();
        lastCommitIdTextField.setEditable(true);
        lastCommitIdTextField.setEnabled(true);
        lastCommitIdTextField.setText("");
        jPanel1.add(lastCommitIdTextField, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 30), new Dimension(608, 38), new Dimension(-1, 30), 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("跳过配置文件");
        jPanel1.add(label4, new com.intellij.uiDesigner.core.GridConstraints(7, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jPanel1.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(7, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(608, 28), null, 0, false));
        skipPropertiesNoBtn = new JRadioButton();
        skipPropertiesNoBtn.setText("否");
        panel1.add(skipPropertiesNoBtn, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel1.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        skipPropertiesYesBtn = new JRadioButton();
        skipPropertiesYesBtn.setText("是");
        panel1.add(skipPropertiesYesBtn, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyJarsTextField = new JTextField();
        copyJarsTextField.setEditable(true);
        copyJarsTextField.setEnabled(true);
        copyJarsTextField.setText("");
        copyJarsTextField.setToolTipText("多个文件使用英文逗号分隔");
        jPanel1.add(copyJarsTextField, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 30), new Dimension(608, 38), new Dimension(-1, 30), 0, false));
        final JLabel label5 = new JLabel();
        label5.setEnabled(true);
        label5.setText("复制Jar文件");
        label5.setToolTipText("多个文件使用英文逗号分隔");
        jPanel1.add(label5, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("输出日志");
        jPanel1.add(label6, new com.intellij.uiDesigner.core.GridConstraints(10, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        packageBtn = new JButton();
        packageBtn.setBackground(new Color(-12236470));
        packageBtn.setEnabled(true);
        packageBtn.setForeground(new Color(-9454773));
        packageBtn.setText("打包");
        jPanel1.add(packageBtn, new com.intellij.uiDesigner.core.GridConstraints(11, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, 50), new Dimension(608, 38), new Dimension(200, 50), 1, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        jPanel1.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(10, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(800, 300), new Dimension(608, 18), null, 0, false));
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        scrollPane1.setViewportView(logTextArea);
        final JLabel label7 = new JLabel();
        label7.setText("跳过其他项目文件");
        label7.setToolTipText("如果Git目录下存在多个项目请选中“是”，其他选”否“");
        jPanel1.add(label7, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jPanel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(8, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(608, 28), null, 0, false));
        skipOtherProjectNoBtn = new JRadioButton();
        skipOtherProjectNoBtn.setText("否");
        panel2.add(skipOtherProjectNoBtn, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        panel2.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        skipOtherProjectYesBtn = new JRadioButton();
        skipOtherProjectYesBtn.setText("是");
        panel2.add(skipOtherProjectYesBtn, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        preSelBtn = new JButton();
        preSelBtn.setActionCommand("open");
        preSelBtn.setText("选择");
        jPanel1.add(preSelBtn, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setEnabled(true);
        label8.setText("Git路径");
        jPanel1.add(label8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gitPathTextField = new JTextField();
        gitPathTextField.setEditable(true);
        gitPathTextField.setEnabled(true);
        gitPathTextField.setText("");
        jPanel1.add(gitPathTextField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(500, 30), new Dimension(608, 38), new Dimension(-1, 30), 0, false));
        gitPathBtn = new JButton();
        gitPathBtn.setText("选择");
        jPanel1.add(gitPathBtn, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("配置文件路径");
        label9.setToolTipText("如果Git目录下存在多个项目请选中“是”，其他选”否“");
        jPanel1.add(label9, new com.intellij.uiDesigner.core.GridConstraints(9, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        propertiesFile = new JTextField();
        propertiesFile.setEditable(true);
        propertiesFile.setEnabled(true);
        propertiesFile.setText("");
        jPanel1.add(propertiesFile, new com.intellij.uiDesigner.core.GridConstraints(9, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(500, 30), new Dimension(608, 38), new Dimension(-1, 30), 0, false));
        propertiesBtn = new JButton();
        propertiesBtn.setActionCommand("open");
        propertiesBtn.setText("加载");
        jPanel1.add(propertiesBtn, new com.intellij.uiDesigner.core.GridConstraints(9, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("项目路径");
        label10.setToolTipText("多个文件使用英文逗号分隔");
        jPanel1.add(label10, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("项目名称");
        label11.setToolTipText("多个文件使用英文逗号分隔");
        jPanel1.add(label11, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectPathTextField = new JTextField();
        projectPathTextField.setEditable(true);
        projectPathTextField.setEnabled(true);
        projectPathTextField.setText("");
        projectPathTextField.setToolTipText("");
        jPanel1.add(projectPathTextField, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 30), new Dimension(608, 38), new Dimension(-1, 30), 0, false));
        projectNamesTextField = new JTextField();
        projectNamesTextField.setEditable(true);
        projectNamesTextField.setEnabled(true);
        projectNamesTextField.setText("");
        projectNamesTextField.setToolTipText("项目名称多个使用英文逗号分隔");
        jPanel1.add(projectNamesTextField, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 30), new Dimension(608, 38), new Dimension(-1, 30), 0, false));
        projectPathButton = new JButton();
        projectPathButton.setText("选择");
        jPanel1.add(projectPathButton, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(skipOtherProjectYesBtn);
        buttonGroup.add(skipOtherProjectNoBtn);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jPanel1;
    }


    class SkipPropertiesListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("1")) {
                skipPropertiesValue = "1";
            }
            if (e.getActionCommand().equals("0")) {
                skipPropertiesValue = "0";
            }
        }
    }

    class SkipOtherProjectListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("1")) {
                skipOtherProjectValue = "1";
            }
            if (e.getActionCommand().equals("0")) {
                skipOtherProjectValue = "0";
            }
        }
    }
}
