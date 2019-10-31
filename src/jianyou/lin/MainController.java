package jianyou.lin;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.*;
import java.util.Arrays;

public class MainController {

    public Button startParseBtn;
    public TextField srcText;//res 资源文件目录
    public TextField distText;
    public TextArea textArea;
    public Button srcMoreBtn;
    public Button distMoreBtn;
    public TextArea resultTextArea;

    public void calc() throws IOException {
        String text = textArea.getText();
        String srcFilePath = srcText.getText();
        String distFilePath = distText.getText();
        if (showEmptyError(distFilePath)) return;
        if (showEmptyError(srcFilePath)) return;
        if (showEmptyError(text)) return;
        String[] split = text.split("\n");
        print(Arrays.toString(split));
        File srcFile = new File(srcFilePath);
        File distFile = new File(distFilePath);
        if (!srcFile.exists()) {
            print("file not exist:" + srcFile);
            return;
        }
        if (!distFile.exists()) {
            print("file not exist:" + distFile);
            return;
        }
        //step 1: read res file dir and loop multi language dir strings.xml
        //step 2:read translate file ,parse multi language,
        //step 3: append translate string to strings.xml file end.
//        RandomAccessFile randomAccessFile = new RandomAccessFile(new File("C:\\Work\\OPEN_SOURCE_PROJECT\\JavaProject\\res\\temp.txt"), "rw");

        if (srcFile.isDirectory()) {
            File[] values = srcFile.listFiles((dir, name) -> {
                boolean equals = name.startsWith("values");
                if (!equals) {
                    print("not start name:" + name);
                }
                return equals;
            });
            if (values != null) {
                for (File valueDir : values) {
                    File[] files = valueDir.listFiles((dir, name) -> {
                        boolean equals = name.equals("strings.xml");
                        if (!equals) {
                            print("not strings name:" + name);
                        }
                        return equals;
                    });
                    if (files != null)
                        if (files.length > 0) {
                            File stringFile = files[0];//strings.xml
                            RandomAccessFile randomAccessFile = new RandomAccessFile(stringFile, "rw");
                            long lastLinePos = getLastLinePos(randomAccessFile);
                            randomAccessFile.seek(lastLinePos);
//                            String lastLineString = randomAccessFile.readLine();
                            String currentLocalString = findCurrentLocalString(valueDir.getName(), split, distFile);
                            if (currentLocalString.length() > 0) {
                                print("append:[" + valueDir.getName() + "]" + currentLocalString);
                                appendStringToFile(randomAccessFile, lastLinePos, currentLocalString);
                            }
//                            print("lastLineString:" + lastLineString + " pos:" + lastLinePos);
                        }
                }
            }
        }
    }

    private void print(String s) {
        resultTextArea.appendText(s + "\n");
        System.out.println(s);
    }

    private String findCurrentLocalString(String local, String[] split, File distFile) throws IOException {
        if (distFile.isDirectory()) {
            print("ERROR: 目标路径是目录不是文件");
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(distFile, "r");
        StringBuilder result = new StringBuilder();

        String translateLocale;
        String originLocale = local.replace("values-", "");
        int i = originLocale.indexOf("-");
        if (i > 0 && !local.contains("rHK") && !local.contains("rTW")) {
            originLocale = local.substring(0, local.lastIndexOf("-"));
        } else {
            originLocale = local;
        }
        print("locale:" + originLocale);//
//        result.append("locale:").append(originLocale).append("\n");
        do {
            translateLocale = randomAccessFile.readLine();
            if (translateLocale != null && !translateLocale.trim().isEmpty()) {
                if (translateLocale.toLowerCase().equals("values") || translateLocale.toLowerCase().contains("values-")
                        || translateLocale.contains("中文") || translateLocale.contains("中文繁体")) {
                    print("判断 【" + translateLocale.toLowerCase() + "】【" + originLocale.toLowerCase() + "】");
                    String tw = "values-zh-rTW".toLowerCase();
                    String hk = "values-zh-rHK".toLowerCase();
                    if (translateLocale.toLowerCase().equals(originLocale.toLowerCase())
                            || translateLocale.toLowerCase().contains(originLocale.toLowerCase())
                            || ((originLocale.toLowerCase().equals(tw) || originLocale.toLowerCase().equals(hk)) &&
                            (translateLocale.toLowerCase().contains(tw) || translateLocale.toLowerCase().contains(hk)))
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
                                            .replace("<string name=“ ", "<string name=\"")
                                            .replace("”>", "\">")
                                    ;
                                    result.append(readLine).append("\r\n");
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
        if (text.isEmpty()) {
//            Dialog dialog = new Dialog();
//            dialog.setContentText("Empty text");
//            dialog.setTitle("Error");
//            dialog.setOnCloseRequest(event -> {
//                print("event = [" + event + "]");
//                dialog.close();
//            });
//            dialog.show();
            print("Empty");
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
     * @param raf           RandomAccessFile
     * @param points        指针位置
     * @param insertContent 插入内容
     **/
    public static void appendStringToFile(RandomAccessFile raf, long points, String insertContent) {
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
            raf.writeBytes(insertContent + "\r\n");
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
