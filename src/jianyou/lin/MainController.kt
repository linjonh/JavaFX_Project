package jianyou.lin

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.xml.sax.SAXException
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class MainController {

    @FXML
    var startParseBtn: Button? = null
    @FXML
    var srcText: TextField? = null//res 资源文件目录
    @FXML
    var translateText: TextField? = null
    @FXML
    var textArea: TextArea? = null
    @FXML
    var srcMoreBtn: Button? = null
    @FXML
    var distMoreBtn: Button? = null
    @FXML
    var resultTextArea: TextArea? = null
    @FXML
    var testCopyCheckBox: CheckBox? = null
    @FXML
    var testTempDistText: TextField? = null
    @FXML
    var isProjectFileDir: CheckBox? = null
    private var split: Array<String>? = null

    fun onIsProjectFileDirCheckBox() {
        textArea!!.isDisable = !isProjectFileDir!!.isSelected
    }

    @Throws(IOException::class, ParserConfigurationException::class, SAXException::class)
    fun calc() {
        print("\n\n>starting parsing======================================================>\n\n")
        var fields = textArea!!.text
        val srcFilePath = srcText!!.text
        val translateFilePathText = translateText!!.text
        val testTempDistTextText = testTempDistText!!.text
        if (showEmptyError(translateFilePathText)) return
        if (showEmptyError(srcFilePath)) return
        val translateFile = File(translateFilePathText)
        if (isProjectFileDir!!.isSelected) {
            if (showEmptyError(fields)) return
            fields = fields.replace(",", "\n").replace("，", "\n")
            split = fields.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            print(Arrays.toString(split))
        } else {
            if (translateFile.isDirectory) {
                print("ERROR: 目标路径是目录不是文件")
                return
            }
        }
        if (showEmptyError(testTempDistTextText)) return

        val srcFile = File(srcFilePath)

        if (!srcFile.exists()) {
            print("file not exist:$srcFile")
            return
        }
        if (!translateFile.exists()) {
            print("file not exist:$translateFile")
            return
        }
        //step 1: read res file dir and loop multi language dir strings.xml
        //step 2:read translate file ,parse multi language,
        //step 3: append translate string to strings.xml file end.
        var randomAccessFile: RandomAccessFile? = null
        if (testCopyCheckBox!!.isSelected) {
            randomAccessFile = RandomAccessFile(File(testTempDistTextText), "rw")
        }

        if (srcFile.isDirectory) {
            val values = getValuesFileList(srcFile)
            if (values != null) {
                for (srcValueDir in values) {//==============================>for 循环遍历被翻译的values多国语言文件目录
                    val stringFileList = getStringsFileList(srcValueDir)
                    if (stringFileList != null && stringFileList.size > 0) {
                        val stringFile = stringFileList[0]//strings.xml
                        if (!testCopyCheckBox!!.isSelected) {
                            randomAccessFile = RandomAccessFile(stringFile, "rw")
                        }
                        val lastLinePos = getLastLinePos(randomAccessFile!!)
                        randomAccessFile.seek(lastLinePos)
                        val lastLineString = randomAccessFile.readLine()
                        print("lastLineString:$lastLineString pos:$lastLinePos")

                        var currentLocalString = ""
                        if (isProjectFileDir!!.isSelected) {

                            //loop document
                            currentLocalString = getCopyFieldsString(translateFile, srcValueDir, currentLocalString)

                        } else {
                            /**
                             * 从text翻译文件中查找<string></string>tag的对应国家多国语言
                             */
                            currentLocalString = findCurrentLocalString(srcValueDir.name, translateFile)


                        }
                        if (currentLocalString.length > 0) {
                            print("append:[" + srcValueDir.name + "]" + currentLocalString)
                            appendStringToFile(randomAccessFile, lastLinePos, currentLocalString)
                        }
                    }
                }
            }
        }
    }

    @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
    private fun getCopyFieldsString(translateFile: File, srcValueDir: File, currentLocalString: String): String {
        var currentLocalString = currentLocalString
        if (translateFile.isDirectory) {
            val valuesFileList = getValuesFileList(translateFile)
            for (translateProjectValuesDir in valuesFileList!!) {
                if (srcValueDir == translateProjectValuesDir) {
                    print("srcValueDir: $srcValueDir")
                    val stringsFileList = getStringsFileList(translateProjectValuesDir)
                    if (stringsFileList != null && stringsFileList.size > 0) {//string.xml 匹配读写
                        val stringsFile = stringsFileList[0]//strings.xml
                        val document = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(stringsFile)
                        val nodeList = document.documentElement.getElementsByTagName("string")
                        val stringBuilder = StringBuilder()
                        for (i in 0 until nodeList.length) {
                            val item = nodeList.item(i)
                            val attributes = item.attributes
                            val attrNode = attributes.item(0)
                            val stringName = attrNode.nodeValue
                            for (aSplit in split!!) {
                                if (stringName == aSplit) {
                                    val xmlString = StringBuilder()
                                    val nodeValue = item.firstChild.nodeValue
                                    xmlString.append("    <string  name=\"").append(attrNode.nodeValue).append("\">").append(nodeValue).append("</string>\r\n")
                                    stringBuilder.append(xmlString)
                                    break
                                }
                            }
                        }
                        //拷贝写入配翻译文件
                        currentLocalString = stringBuilder.toString()
                    }
                    break
                }
            }
        }
        return currentLocalString
    }

    private fun getStringsFileList(valueDir: File): Array<File>? {
        return valueDir.listFiles { dir, name ->
            val equals = name == "strings.xml"
            if (!equals) {
                print("not strings name:$name")
            }
            equals
        }
    }

    private fun getValuesFileList(srcFile: File): Array<File>? {
        return srcFile.listFiles { dir, name ->
            val equals = name.startsWith("values")
            if (!equals) {
                print("not start values name:$name")
            }
            equals
        }
    }

    private fun print(s: String) {
        resultTextArea!!.appendText(s + "\n")
        println(s)
    }

    @Throws(IOException::class)
    private fun findCurrentLocalString(local: String, translateFile: File): String {
        if (translateFile.isDirectory) {
            print("ERROR: 目标路径是目录不是文件")
            return ""
        }
        val randomAccessFile = BufferedReader(FileReader(translateFile))//BufferedReader读取的是utf-8的，而RandomAccessFile会出现乱码问题。
        val result = StringBuilder()
        var translateLocale: String?
        var originLocale = local.replace("values-", "")
        val i = originLocale.indexOf("-")
        if (i > 0 && !local.contains("rHK") && !local.contains("rTW") && !local.contains("rCN")) {
            originLocale = local.substring(0, local.lastIndexOf("-"))
        } else {
            originLocale = local
        }
        print("locale:$originLocale")//
        //        result.append("locale:").append(originLocale).append("\n");
        do {
            translateLocale = randomAccessFile.readLine()
            if (translateLocale != null && !translateLocale.trim { it <= ' ' }.isEmpty()) {
                val contains = translateLocale.contains("中文")
                if (contains) {
                    print("hit")
                }
                val transLowerCase = translateLocale.toLowerCase()
                if (transLowerCase == "values" || transLowerCase.contains("values-")
                        || contains || translateLocale.contains("中文繁体")) {
                    val originLcaleLowerCase = originLocale.toLowerCase()
                    print("判断 【$transLowerCase】【$originLcaleLowerCase】")
                    val tw = "values-zh-rTW".toLowerCase()
                    val hk = "values-zh-rHK".toLowerCase()
                    val cn = "values-zh-rCN".toLowerCase()
                    val equals = originLcaleLowerCase == cn
                    if (equals) {
                        print("hit")
                    }
                    if (transLowerCase == originLcaleLowerCase
                            || transLowerCase.contains(originLcaleLowerCase)
                            || (originLcaleLowerCase == tw || originLcaleLowerCase == hk) && (transLowerCase.contains(tw) || transLowerCase.contains(hk))
                            || translateLocale == "中文" && equals
                            || translateLocale == "中文繁体" && originLcaleLowerCase == hk
                            || translateLocale == "中文繁体" && originLcaleLowerCase == tw) {
                        var readLine: String?
                        var loop: Boolean
                        do {
                            readLine = randomAccessFile.readLine()
                            loop = (readLine != null
                                    &&
                                    !readLine.contains("Values-")
                                    && !readLine.contains("values-")
                                    && !readLine.contains("中文")
                                    && !readLine.contains("中文繁体"))
                            if (loop) {
                                if (!readLine!!.trim { it <= ' ' }.isEmpty()) {
                                    readLine = readLine.replace("</ string>", "</string>")
                                            .replace("'", "\\'")
                                            .replace("\\ n \\ n", "\\n\\n")
                                            .replace("\\ N \\ n", "\\n\\n")
                                            .replace("\\ N", "\\n")
                                            .replace("\\ n", "\\n")
                                            .replace("=“ ", "=\"")
                                            .replace("”>", "\">")
                                    result.append("    ").append(readLine).append("\r\n")
                                }
                            }
                        } while (loop)
                        break
                    }
                }
            }
        } while (translateLocale != null)
        randomAccessFile.close()
        return result.toString()
    }

    private fun showEmptyError(text: String): Boolean {
        if (text.trim { it <= ' ' }.isEmpty()) {
            //            Dialog dialog = new Dialog();
            //            dialog.setContentText("Empty text");
            //            dialog.setTitle("Error");
            //            dialog.setOnCloseRequest(event -> {
            //                print("event = [" + event + "]");
            //                dialog.close();
            //            });
            //            dialog.show();
            print("ERROR:Empty input string")
            return true
        }
        return false
    }

    @Throws(IOException::class)
    fun showChoosePathWindow() {
        val root = FXMLLoader.load<Parent>(javaClass.getResource("fileSelection.fxml"))
        val pathWindowStage = Stage()
        pathWindowStage.title = "Choose Path"
        pathWindowStage.scene = Scene(root, 700.0, 500.0)
        pathWindowStage.setOnCloseRequest { event -> print("OnClose") }
        pathWindowStage.show()
    }

    companion object {


        /**
         * 实现向指定位置
         * 插入数据
         *
         * @param raf                 RandomAccessFile
         * @param points              指针位置
         * @param insertContentString 插入内容
         */
        fun appendStringToFile(raf: RandomAccessFile, points: Long, insertContentString: String) {
            try {
                val tmp = File.createTempFile("tmp", null)
                //创建一个临时文件夹来保存插入点后的数据
                val tmpOut = FileOutputStream(tmp)
                val tmpIn = FileInputStream(tmp)
                raf.seek(points)
                /*将插入点后的内容读入临时文件夹**/
                val buff = ByteArray(1024)
                //+++++++++++++++++++++++++++++++++++++++++
                //用于保存临时读取的字节数
                var hasRead = 0
                //循环读取插入点后的内容
                while ({ hasRead = tmpIn.read(buff);hasRead }() > 0) {
                    // 将读取的数据写入临时文件中
                    tmpOut.write(buff, 0, hasRead)
                }
                tmpOut.close()
                //+++++++++++++++++++++++++++++++++++++++++
                raf.seek(points)
                //+++++++++++++++++++++++++++++++++++++++++
                //追加需要追加的内容
                raf.write(insertContentString.toByteArray(StandardCharsets.UTF_8))//RandomAccessFile类需要处理乱码问题。
                //+++++++++++++++++++++++++++++++++++++++++
                hasRead = tmpIn.read(buff)
                if (hasRead > 0) {//有内容回存储追加
                    //最后追加临时文件中的内容
                    do {
                        //                    print("hasRead:" + hasRead);
                        raf.write(buff, 0, hasRead)
                    } while ({ hasRead = tmpIn.read(buff);hasRead }() > 0)
                    raf.seek(points)//恢復當前讀寫的位置;
                }
                //+++++++++++++++++++++++++++++++++++++++++
                tmpIn.close()
                tmp.deleteOnExit()//在JVM退出时删除
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        /**
         * 实现向指定位置
         * 插入数据
         *
         * @param raf                  RandomAccessFile
         * @param points               指针位置
         * @param insertContent        插入内容
         * @param replaceContentLength 非插入时需要提供
         */
        fun replaceStringToFile(raf: RandomAccessFile, points: Long, insertContent: String, replaceContentLength: Int) {
            val isInsert = false
            try {
                val tmp = File.createTempFile("tmp", null)
                //            tmp.deleteOnExit();//在JVM退出时删除

                //创建一个临时文件夹来保存插入点后的数据
                val tmpOut = FileOutputStream(tmp)
                val tmpIn = FileInputStream(tmp)
                raf.seek(points)
                /**将插入点后的内容读入临时文件夹 */
                val buff = ByteArray(1024)

                //+++++++++++++++++++++++++++++++++++++++++
                //用于保存临时读取的字节数
                var hasRead = 0
                //循环读取插入点后的内容
                while ({ hasRead = tmpIn.read(buff);hasRead }() > 0) {
                    // 将读取的数据写入临时文件中
                    tmpOut.write(buff, 0, hasRead)
                }
                //+++++++++++++++++++++++++++++++++++++++++
                if (!isInsert) {//replace`
                    //插入需要指定添加的数据
                    raf.seek(points - replaceContentLength)//返回原来的插入处
                }
                //            else if (isInsert) {
                //                raf.seek(points);
                //            }
                //+++++++++++++++++++++++++++++++++++++++++
                //追加需要追加的内容
                val length = insertContent.length
                raf.writeBytes(insertContent + "\r\n")
                //+++++++++++++++++++++++++++++++++++++++++
                hasRead = tmpIn.read(buff)
                if (hasRead <= 0) {
                    if (!isInsert && length < replaceContentLength) {//要是替换的话，要上删除多余的该行未替换的字符
                        val gap = replaceContentLength - length
                        raf.setLength(points - gap)
                        raf.seek(points - gap)//恢復當前讀寫的位置;
                    }

                } else {//有内容回存储追加
                    //+++++++++++++++++++++++++++++++++++++++++
                    if (!isInsert && length < replaceContentLength) {
                        val gap = replaceContentLength - length
                        raf.seek(points - gap)
                        raf.setLength(points - gap + tmp.length())//truncate file length
                    }
                    //最后追加临时文件中的内容
                    do {
                        //                    print("hasRead:" + hasRead);
                        raf.write(buff, 0, hasRead)
                    } while ({ hasRead = tmpIn.read(buff);hasRead }() > 0)
                    raf.seek(points)//恢復當前讀寫的位置;
                }
                //+++++++++++++++++++++++++++++++++++++++++
                tmp.deleteOnExit()
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        /**
         * 返回最后一行非空字符串的起始位置,并移动文件指针到最后一行的起始位置。
         *
         * @param raf RandomAccessFile对象
         * @return 最后一行的起始位置
         * @throws IOException
         */
        @Throws(IOException::class)
        private fun getLastLinePos(raf: RandomAccessFile): Long {
            val lastLinePos = 0L
            val len = raf.length()        // 获取文件占用字节数
            if (len > 0L) {
                var pos = len - 1            // 向前走一个字节
                while (pos > 0) {
                    pos--
                    raf.seek(pos)                // 移动指针
                    if (raf.readByte() == '\n'.toByte()) {// 判断这个字节是不是回车符
                        val trim = raf.readLine().trim { it <= ' ' }
                        if (!trim.isEmpty()) {
                            //                        print("not empty lastLine:" + trim + " pos:" + pos);
                            return pos + 1//加上回车的一个字符位置
                        } else {
                            println("empty line")
                        }
                    }

                }
            }
            return lastLinePos
        }
    }
}
