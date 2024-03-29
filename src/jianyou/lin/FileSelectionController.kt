package jianyou.lin

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView

class FileSelectionController {
    @FXML
    var readPathBtn: Button? = null
    @FXML
    var fileTree: TreeView<String>? = null
    @FXML
    var pathText: TextField? = null


    fun readFilePath() {
        var pathTextText = pathText!!.text
        if (pathTextText.isEmpty()) {
            pathTextText = System.getProperty("user.dir")
            println("path:$pathTextText")
        }
        val value = TreeItem(pathTextText)
        fileTree!!.root = value
    }

}
