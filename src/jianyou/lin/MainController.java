package jianyou.lin;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainController {

    public Button startParseBtn;
    public TextField srcText;//res 资源文件目录
    public TextField translateText;
    public TextArea textArea;
    public Button srcMoreBtn;
    public Button distMoreBtn;
    public TextArea resultTextArea;
    public CheckBox testCopyCheckBox;
    public TextField testTempDistText;
    public CheckBox isProjectFileDir;
    private String[] split;

    public void onIsProjectFileDirCheckBox() {
        textArea.setDisable(!isProjectFileDir.isSelected());
    }

    public void calc() throws IOException, ParserConfigurationException, SAXException {
        print("\n\n>starting parsing======================================================>\n\n");
        String fields = textArea.getText();
        String srcFilePath = srcText.getText();
        String translateFilePathText = translateText.getText();
        String testTempDistTextText = testTempDistText.getText();
        if (showEmptyError(translateFilePathText)) return;
        if (showEmptyError(srcFilePath)) return;
        File translateFile = new File(translateFilePathText);
        if (isProjectFileDir.isSelected()) {
            if (showEmptyError(fields)) return;
            fields = fields.replace(",", "\n").replace("，", "\n");
            split = fields.split("\n");
            print(Arrays.toString(split));
        } else {
            if (translateFile.isDirectory()) {
                print("ERROR: 目标路径是目录不是文件");
                return;
            }
        }
        if (showEmptyError(testTempDistTextText)) return;

        File srcFile = new File(srcFilePath);

        if (!srcFile.exists()) {
            print("file not exist:" + srcFile);
            return;
        }
        if (!translateFile.exists()) {
            print("file not exist:" + translateFile);
            return;
        }
        //step 1: read res file dir and loop multi language dir strings.xml
        //step 2:read translate file ,parse multi language,
        //step 3: append translate string to strings.xml file end.
        RandomAccessFile randomAccessFile = null;
        if (testCopyCheckBox.isSelected()) {
            randomAccessFile = new RandomAccessFile(new File(testTempDistTextText), "rw");
        }

        if (srcFile.isDirectory()) {
            File[] values = getValuesFileList(srcFile);
            if (values != null) {
                for (File srcValueDir : values) {//==============================>for 循环遍历被翻译的values多国语言文件目录
                    File[] stringFileList = getStringsFileList(srcValueDir);
                    if (stringFileList != null && stringFileList.length > 0) {
                        File stringFile = stringFileList[0];//strings.xml
                        if (!testCopyCheckBox.isSelected()) {
                            randomAccessFile = new RandomAccessFile(stringFile, "rw");
                        }
                        long lastLinePos = getLastLinePos(randomAccessFile);
                        randomAccessFile.seek(lastLinePos);
                        String lastLineString = randomAccessFile.readLine();
                        print("lastLineString:" + lastLineString + " pos:" + lastLinePos);

                        String currentLocalString = "";
                        if (isProjectFileDir.isSelected()) {

                            //loop document
                            currentLocalString = getCopyFieldsString(translateFile, srcValueDir, currentLocalString);

                        } else {
                            /**
                             * 从text翻译文件中查找<string></string>tag的对应国家多国语言
                             */
                            currentLocalString = findCurrentLocalString(srcValueDir.getName(), translateFile);


                        }
                        if (currentLocalString.length() > 0) {
                            print("append:[" + srcValueDir.getName() + "]" + currentLocalString);
                            appendStringToFile(randomAccessFile, lastLinePos, currentLocalString);
                        }
                    }
                }
            }
        }
    }

    private String getCopyFieldsString(File translateFile, File srcValueDir, String currentLocalString) throws SAXException, IOException, ParserConfigurationException {
        if (translateFile.isDirectory()) {
            File[] valuesFileList = getValuesFileList(translateFile);
            for (File translateProjectValuesDir : valuesFileList) {
                if (srcValueDir.equals(translateProjectValuesDir)) {
                    print("srcValueDir: " + srcValueDir);
                    File[] stringsFileList = getStringsFileList(translateProjectValuesDir);
                    if (stringsFileList != null && stringsFileList.length > 0) {//string.xml 匹配读写
                        File stringsFile = stringsFileList[0];//strings.xml
                        Document document = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(stringsFile);
                        NodeList nodeList = document.getDocumentElement().getElementsByTagName("string");
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < nodeList.getLength(); i++) {
                            Node item = nodeList.item(i);
                            NamedNodeMap attributes = item.getAttributes();
                            Node attrNode = attributes.item(0);
                            String stringName = attrNode.getNodeValue();
                            for (String aSplit : split) {
                                if (stringName.equals(aSplit)) {
                                    StringBuilder xmlString = new StringBuilder();
                                    String nodeValue = item.getFirstChild().getNodeValue();
                                    xmlString.append("    <string  name=\"").append(attrNode.getNodeValue()).append("\">").append(nodeValue).append("</string>\r\n");
                                    stringBuilder.append(xmlString);
                                    break;
                                }
                            }
                        }
                        //拷贝写入配翻译文件
                        currentLocalString = stringBuilder.toString();
                    }
                    break;
                }
            }
        }
        return currentLocalString;
    }

    private File[] getStringsFileList(File valueDir) {
        return valueDir.listFiles((dir, name) -> {
            boolean equals = name.equals("strings.xml");
            if (!equals) {
                print("not strings name:" + name);
            }
            return equals;
        });
    }

    private File[] getValuesFileList(File srcFile) {
        return srcFile.listFiles((dir, name) -> {
            boolean equals = name.startsWith("values");
            if (!equals) {
                print("not start values name:" + name);
            }
            return equals;
        });
    }

    private void print(String s) {
        resultTextArea.appendText(s + "\n");
        System.out.println(s);
    }

    private String findCurrentLocalString(String local, File translateFile) throws IOException {
        if (translateFile.isDirectory()) {
            print("ERROR: 目标路径是目录不是文件");
            return "";
        }
        BufferedReader randomAccessFile = new BufferedReader(new FileReader(translateFile));//BufferedReader读取的是utf-8的，而RandomAccessFile会出现乱码问题。
        StringBuilder result = new StringBuilder();
        String translateLocale;
        String originLocale = local.replace("values-", "");
        int i = originLocale.indexOf("-");
        if (i > 0 && !local.contains("rHK") && !local.contains("rTW")&&!local.contains("rCN")) {
            originLocale = local.substring(0, local.lastIndexOf("-"));
        } else {
            originLocale = local;
        }
        print("locale:" + originLocale);//
//        result.append("locale:").append(originLocale).append("\n");
        do {
            translateLocale = randomAccessFile.readLine();
            if (translateLocale != null && !translateLocale.trim().isEmpty()) {
                boolean contains = translateLocale.contains("中文");
                if (contains) {
                    print("hit");
                }
                String transLowerCase = translateLocale.toLowerCase();
                if (transLowerCase.equals("values") || transLowerCase.contains("values-")
                        || contains || translateLocale.contains("中文繁体")) {
                    String originLcaleLowerCase = originLocale.toLowerCase();
                    print("判断 【" + transLowerCase + "】【" + originLcaleLowerCase + "】");
                    String tw = "values-zh-rTW".toLowerCase();
                    String hk = "values-zh-rHK".toLowerCase();
                    String cn = "values-zh-rCN".toLowerCase();
                    boolean equals = originLcaleLowerCase.equals(cn);
                    if (equals) {
                        print("hit");
                    }
                    if (transLowerCase.equals(originLcaleLowerCase)
                            || transLowerCase.contains(originLcaleLowerCase)
                            || ((originLcaleLowerCase.equals(tw) || originLcaleLowerCase.equals(hk)) &&
                            (transLowerCase.contains(tw) || transLowerCase.contains(hk)))
                            || (translateLocale.equals("中文") && equals)
                            || (translateLocale.equals("中文繁体") && originLcaleLowerCase.equals(hk))
                            || (translateLocale.equals("中文繁体") && originLcaleLowerCase.equals(tw))
                    ) {
                        String readLine;
                        boolean loop;
                        do {
                            readLine = randomAccessFile.readLine();
                            loop = readLine != null
                                    &&
                                    !readLine.contains("Values-")
                                    && !readLine.contains("values-")
                                    && !readLine.contains("中文")
                                    && !readLine.contains("中文繁体")
                            ;
                            if (loop) {
                                if (!readLine.trim().isEmpty()) {
                                    readLine = readLine.replace("</ string>", "</string>")
                                            .replace("'", "\\'")
                                            .replace("\\ n \\ n", "\\n\\n")
                                            .replace("\\ N \\ n", "\\n\\n")
                                            .replace("\\ N", "\\n")
                                            .replace("\\ n", "\\n")
                                            .replace("=“ ", "=\"")
                                            .replace("”>", "\">")
                                    ;
                                    result.append("    ").append(readLine).append("\r\n");
                                }
                            }
                        } while (loop);
                        break;
                    }
                }
            }
        } while (translateLocale != null);
        randomAccessFile.close();
        return result.toString();
    }

    private boolean showEmptyError(String text) {
        if (text.trim().isEmpty()) {
//            Dialog dialog = new Dialog();
//            dialog.setContentText("Empty text");
//            dialog.setTitle("Error");
//            dialog.setOnCloseRequest(event -> {
//                print("event = [" + event + "]");
//                dialog.close();
//            });
//            dialog.show();
            print("ERROR:Empty input string");
            return true;
        }
        return false;
    }

    public void showChoosePathWindow() throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("fileSelection.fxml"));
        Stage pathWindowStage = new Stage();
        pathWindowStage.setTitle("Choose Path");
        pathWindowStage.setScene(new Scene(root, 700, 500));
        pathWindowStage.setOnCloseRequest(event -> {
            print("OnClose");
        });
        pathWindowStage.show();
    }


    /**
     * 实现向指定位置
     * 插入数据
     *
     * @param raf                 RandomAccessFile
     * @param points              指针位置
     * @param insertContentString 插入内容
     **/
    public static void appendStringToFile(RandomAccessFile raf, long points, String insertContentString) {
        try {
            File tmp = File.createTempFile("tmp", null);
            //创建一个临时文件夹来保存插入点后的数据
            FileOutputStream tmpOut = new FileOutputStream(tmp);
            FileInputStream tmpIn = new FileInputStream(tmp);
            raf.seek(points);
            /*将插入点后的内容读入临时文件夹**/
            byte[] buff = new byte[1024];
            //+++++++++++++++++++++++++++++++++++++++++
            //用于保存临时读取的字节数
            int hasRead = 0;
            //循环读取插入点后的内容
            while ((hasRead = raf.read(buff)) > 0) {
                // 将读取的数据写入临时文件中
                tmpOut.write(buff, 0, hasRead);
            }
            tmpOut.close();
            //+++++++++++++++++++++++++++++++++++++++++
            raf.seek(points);
            //+++++++++++++++++++++++++++++++++++++++++
            //追加需要追加的内容
            raf.write(insertContentString.getBytes(StandardCharsets.UTF_8));//RandomAccessFile类需要处理乱码问题。
            //+++++++++++++++++++++++++++++++++++++++++
            hasRead = tmpIn.read(buff);
            if (hasRead > 0) {//有内容回存储追加
                //最后追加临时文件中的内容
                do {
//                    print("hasRead:" + hasRead);
                    raf.write(buff, 0, hasRead);
                } while ((hasRead = tmpIn.read(buff)) > 0);
                raf.seek(points);//恢復當前讀寫的位置;
            }
            //+++++++++++++++++++++++++++++++++++++++++
            tmpIn.close();
            tmp.deleteOnExit();//在JVM退出时删除
        } catch (Exception e) {
            e.printStackTrace();
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
     **/
    public static void replaceStringToFile(RandomAccessFile raf, long points, String insertContent, int replaceContentLength) {
        boolean isInsert = false;
        try {
            File tmp = File.createTempFile("tmp", null);
//            tmp.deleteOnExit();//在JVM退出时删除

            //创建一个临时文件夹来保存插入点后的数据
            FileOutputStream tmpOut = new FileOutputStream(tmp);
            FileInputStream tmpIn = new FileInputStream(tmp);
            raf.seek(points);
            /**将插入点后的内容读入临时文件夹**/
            byte[] buff = new byte[1024];

            //+++++++++++++++++++++++++++++++++++++++++
            //用于保存临时读取的字节数
            int hasRead = 0;
            //循环读取插入点后的内容
            while ((hasRead = raf.read(buff)) > 0) {
                // 将读取的数据写入临时文件中
                tmpOut.write(buff, 0, hasRead);
            }
            //+++++++++++++++++++++++++++++++++++++++++
            if (!isInsert) {//replace`
                //插入需要指定添加的数据
                raf.seek(points - replaceContentLength);//返回原来的插入处
            }
//            else if (isInsert) {
//                raf.seek(points);
//            }
            //+++++++++++++++++++++++++++++++++++++++++
            //追加需要追加的内容
            int length = insertContent.length();
            raf.writeBytes(insertContent + "\r\n");
            //+++++++++++++++++++++++++++++++++++++++++
            hasRead = tmpIn.read(buff);
            if (hasRead <= 0) {
                if (!isInsert && length < replaceContentLength) {//要是替换的话，要上删除多余的该行未替换的字符
                    int gap = replaceContentLength - length;
                    raf.setLength(points - gap);
                    raf.seek(points - gap);//恢復當前讀寫的位置;
                }
            } else {//有内容回存储追加
                //+++++++++++++++++++++++++++++++++++++++++
                if (!isInsert && length < replaceContentLength) {
                    int gap = replaceContentLength - length;
                    raf.seek(points - gap);
                    raf.setLength(points - gap + tmp.length());//truncate file length
                }
                //最后追加临时文件中的内容
                do {
//                    print("hasRead:" + hasRead);
                    raf.write(buff, 0, hasRead);
                } while ((hasRead = tmpIn.read(buff)) > 0);
                raf.seek(points);//恢復當前讀寫的位置;
            }
            //+++++++++++++++++++++++++++++++++++++++++
            tmp.deleteOnExit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 返回最后一行非空字符串的起始位置,并移动文件指针到最后一行的起始位置。
     *
     * @param raf RandomAccessFile对象
     * @return 最后一行的起始位置
     * @throws IOException
     */
    private static long getLastLinePos(RandomAccessFile raf) throws IOException {
        long lastLinePos = 0L;
        long len = raf.length();        // 获取文件占用字节数
        if (len > 0L) {
            long pos = len - 1;            // 向前走一个字节
            while (pos > 0) {
                pos--;
                raf.seek(pos);                // 移动指针
                if (raf.readByte() == '\n') {// 判断这个字节是不是回车符
                    String trim = raf.readLine().trim();
                    if (!trim.isEmpty()) {
//                        print("not empty lastLine:" + trim + " pos:" + pos);
                        return pos + 1;//加上回车的一个字符位置
                    } else {
                        System.out.println("empty line");
                    }
                }

            }
        }
        return lastLinePos;
    }
}
