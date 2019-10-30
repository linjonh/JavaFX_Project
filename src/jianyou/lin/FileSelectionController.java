package jianyou.lin;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

public class FileSelectionController {
    public Button readPathBtn;
    public TreeView fileTree;
    public TextField pathText;

    public void readFilePath() {
        String pathTextText = pathText.getText();
        if (pathTextText.isEmpty()) {
            pathTextText = System.getProperty("user.dir");
            System.out.println("path:" + pathTextText);
        }
        TreeItem value = new TreeItem(pathTextText);
        fileTree.setRoot(value);
    }

}
