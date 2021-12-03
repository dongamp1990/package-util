package demo;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * git提交窗口类
 * @author dongamp1990@gmail.com
 */
public class GitCommitDialog extends JDialog {
    private final DefaultListModel defaultListModel1 = new DefaultListModel();
    private JScrollPane jScrollPane;
    private JList jList;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY/MM/dd HH:mm:ss");
    private JPanel jPanel1;
    private final PackageGUI parent;
    private JPanel jPanel;
    private JPopupMenu listPopupMenu;

    private ListDataListener listDataListener = new ListDataListener() {
        @Override
        public void intervalAdded(ListDataEvent e) {

        }

        @Override
        public void intervalRemoved(ListDataEvent e) {

        }

        @Override
        public void contentsChanged(ListDataEvent e) {
            System.out.println(e.getIndex0());
            System.out.println(e.getIndex1());
        }
    };

    public GitCommitDialog(Frame owner, PackageGUI parent) {
        super(owner);
        this.parent = parent;
        jPanel = new JPanel();
        jList = new JList();
        jScrollPane = new JScrollPane();
        jScrollPane.setViewportView(jList);
        listPopupMenu = new JPopupMenu();
        listPopupMenu.add("复制");
        setContentPane(jScrollPane);
        setLayout(new ScrollPaneLayout());
        setLocationRelativeTo(parent.frame);
//            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("选择提交ID");
        pack();
        init();
    }

    private void init() {
        jList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    String first = defaultListModel1.get(e.getFirstIndex()).toString();
                    String last = defaultListModel1.get(e.getLastIndex()).toString();
                    parent.lastCommitIdTextField.setText(first.split("\\|")[2]);
                    parent.previousCommitIdTextField.setText(last.split("\\|")[2]);
                }
            }
        });
    }

    private String appendSpeac(String s ,int totalLength) {
        /*StringBuilder builder = new StringBuilder();
        int size = totalLength - s.length();
        builder.append(s);
        for (int i = 0; i <= size; i ++) {
            builder.append("  ");
        }
        return builder.toString();*/

        return s;
    }

    public void loadGit() {
        if (parent.gitPathTextField.getText() == null || parent.gitPathTextField.getText().length() <= 0) {
            return;
        }
        try {
            defaultListModel1.clear();
            parent.lastCommitIdTextField.setText("");
            parent.previousCommitIdTextField.setText("");
            Git git = Git.open(new File(parent.gitPathTextField.getText()));
            Repository repo = git.getRepository();
            RevWalk revWalk = new RevWalk(repo);
            Ref ref = repo.getAllRefs().get(Constants.HEAD);
            RevCommit lastCommit = revWalk.parseCommit(ref.getObjectId());
            revWalk.markStart(lastCommit);
            defaultListModel1.add(0, appendSpeac("提交时间",20)+ "|" + appendSpeac("提交备注", 53) + "|HASH ID");
            for (RevCommit revCommit : revWalk) {
                String message = revCommit.getFullMessage();
                if (message.length() >= 50) {
                    message = message.substring(0, 50);
                }else {
                    message  = appendSpeac(message,50);
                }
                defaultListModel1.addElement(simpleDateFormat.format(new Date(revCommit.getCommitTime() * 1000L))
                        + "|" + message + "|" + revCommit.getId().name());
                defaultListModel1.addListDataListener(listDataListener);
            }
            jList.setModel(defaultListModel1);
            git.close();
        } catch (RepositoryNotFoundException ex) {
            JOptionPane.showMessageDialog(jPanel1, "未找到Git仓库, 异常信息：" + ex.getMessage(),
                    "提示", JOptionPane.WARNING_MESSAGE);
            parent.gitPathTextField.setFocusable(true);
            parent.gitPathTextField.requestFocus();
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(jPanel1, "IO异常：异常信息：" + ex.getMessage(),
                    "提示", JOptionPane.WARNING_MESSAGE);
            parent.gitPathTextField.setFocusable(true);
            parent.gitPathTextField.requestFocus();
            return;
        }
    }
}
