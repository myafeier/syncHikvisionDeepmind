package com.ynpulse;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hikvision.HKDevice;
import com.google.gson.JsonParser;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.dom4j.DocumentException;

import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;


public class Main {

    static String schoolUUID ;  //学校id
     static String m_sDeviceIP;//已登录设备的IP地址
     static short m_sDevicePort; //设备ip
     static String m_sDeviceUser; //设备用户
     static String m_sDevicePwd ; //设备密码
     static String serverGetUrl;  //服务器url
     static String serverPutUrl;  //服务器url
     static String serverUser; //服务器端用户名
     static String serverPwd; //服务器端密码
    static Properties properties=new Properties();  //配置文件
    static ArrayList<Student> students=new ArrayList<Student>(); //
    final static OkHttpClient httpClient=new OkHttpClient();  //客户端
    public  ObjectOutputStream objectOutputStream=null;
    public  ObjectInputStream objectInputStream=null;
    public BufferedOutputStream logStream=null;
    final static HanyuPinyinOutputFormat pingyinFormat=new HanyuPinyinOutputFormat();
    final static String FDLIBNAME="juai" ;//人脸库名称
    static HKDevice device;
    static Student currentStudent=null;//当前处理的学生

    public static void  main(String args[] ){
        Main m=new Main();
        m.syncStudentInfo();
    }

    Main(){
        System.out.println(System.getProperty("user.dir"));
        FileInputStream inputStream= null;
        try {
            inputStream = new FileInputStream(System.getProperty("user.dir")+"\\config\\config.properties");
            properties.load(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        schoolUUID=properties.getProperty("schoolUUID");
        m_sDeviceIP=properties.getProperty("deviceIP");
        m_sDevicePort=(short)Integer.parseInt(properties.getProperty("devicePort"));
        m_sDeviceUser=properties.getProperty("deviceUser");
        m_sDevicePwd=properties.getProperty("devicePWD");
        serverGetUrl=properties.getProperty("serverGetUrl");
        serverPutUrl=properties.getProperty("serverPutUrl");
        serverUser=properties.getProperty("serverUser");
        serverPwd=properties.getProperty("serverPWD");

        //打开日志
        File file=new File(System.getProperty("user.dir")+"\\result.log");
        if(!file.exists()){
            try {
                boolean created=file.createNewFile();
                if(!created){
                    System.out.println("create log file error");
                    System.exit(-1);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        try {
            FileOutputStream outputStream=new FileOutputStream(file);
            logStream=new BufferedOutputStream(outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }


//        //init data file
//        File file=new File(System.getProperty("user.dir")+"\\result.data");
//        if(!file.exists()) {
//            //创建文件先
//            try {
//                boolean created = file.createNewFile();
//                if (!created) {
//                    throw new Exception("创建数据文件失败");
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                System.exit(-1);
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.exit(-1);
//            }
//        }
//        FileInputStream in=null;
//        try {
//            in =new FileInputStream(file);
//            objectInputStream=new ObjectInputStream(in);
//            try {
//                while (true){
//                    Student student = (Student)objectInputStream.readObject();
//                    students.add(student);
//                    System.out.println("readed:"+student.toString());
//                }
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//        } catch (EOFException e){
//            System.out.println("文件读取完毕");
//        }catch (FileNotFoundException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        }finally {
//        }
//
//        FileOutputStream out=null;
//        try {
//            out=new FileOutputStream(file);
//            objectOutputStream=new ObjectOutputStream(out);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(-1);
//        }


        pingyinFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        device=new HKDevice(m_sDeviceIP,m_sDeviceUser,m_sDevicePwd,m_sDevicePort);
        Boolean initStat=initDevice();
        if (!initStat){
            System.out.println("设备初始化失败");
            System.exit(-1);
        }
    }

    public Boolean initDevice(){

        Boolean loginStat=device.loginToDevice();
        if(loginStat!=true){
            System.out.println("Login device fail");
            return false;
        }

        try {
//            Boolean searchStat=device.SearchAllFDLib();
//            if(searchStat!=true) {
//                System.out.println("get device lib error");
//            }
            String FDID;
            try {
                FDID=device.createFDLib(FDLIBNAME);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            Boolean handleStat=device.crateUploadFileHandle(FDID);
            if(handleStat!=true) {
                System.out.println("get device lib error");
                return false;
            }
//            Student student=new Student();
//            student.UUID="8c04b8e0-a6a2-11e8-9ccb-8d45216d9e46";
////            student.Birthday=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());
//
//            System.out.println(student.Birthday);
////
//
//            String name="中文测试";
//            try {
//                HanyuPinyinOutputFormat format=new HanyuPinyinOutputFormat();
//                format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
//                student.name=PinyinHelper.toHanYuPinyinString(name, format," ",true);
//            } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
//                badHanyuPinyinOutputFormatCombination.printStackTrace();
//            }
////            student.name="中文测试";
//
//            try {
//                FileInputStream fi =new FileInputStream(System.getProperty("user.dir")+"\\8c04b8e0-a6a2-11e8-9ccb-8d45216d9e46.jpg");
//                student.Avatar=new byte[fi.available()];
//                fi.read(student.Avatar);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            device.UploadFaceDb(student);
//            device.UploadStatCheck();

        } catch (DocumentException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    public void syncStudentInfo(){

        String url=serverGetUrl+"/"+schoolUUID;
        Request request=new Request.Builder().url(url).build();
        Response response=null;
        try {
            response=httpClient.newCall(request).execute();
            String body=response.body().string();
            JsonParser jsonParser=new JsonParser();
            JsonArray data =(JsonArray)jsonParser.parse(body);
            for(int i=0;i<data.size();i++){
                JsonObject dataObj=data.get(i).getAsJsonObject();
                int id=dataObj.get("id").getAsInt();
                String name=dataObj.get("name").getAsString();
                String uuid=dataObj.get("uuid").getAsString();
                String avatar=dataObj.get("avatar").getAsString();
                currentStudent=new Student();
                currentStudent.setID(id);
                try {
                    currentStudent.setName(PinyinHelper.toHanYuPinyinString(name,pingyinFormat," ",false));
                } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                    badHanyuPinyinOutputFormatCombination.printStackTrace();
                }
                ;
                currentStudent.setUUID(uuid);
                currentStudent.setAvatarUrl(avatar);
                System.out.println("student:"+currentStudent.toString());


                System.out.println("avatarLength:"+avatar.length());

                if(avatar.length()!=0){
                    Request request1=new Request.Builder().url(avatar).build();
                    Response response2=httpClient.newCall(request1).execute();
                    currentStudent.Avatar= response2.body().bytes();
                    response2.close();
                }else{
                    System.out.println("get student avatar error!,studentId:"+currentStudent.UUID);
                    continue;
                }

                try {
                    device.UploadFaceDb(currentStudent);
                    currentStudent.setDeviceUUID(device.checkUploadStat());
                    currentStudent.syncServer(httpClient);
                }catch (Exception e){
                    e.printStackTrace();
                    //处理上次失败逻辑
                    StringBuilder sb=new StringBuilder();
                    sb.append("upload face error:");
                    sb.append(e.getMessage());
                    sb.append("\n");
                    sb.append("student Info:");
                    sb.append(currentStudent.toString());
                    sb.append("\n");
                    System.out.println(sb.toString().getBytes());
                    logStream.write(sb.toString().getBytes());
                    continue;
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(response!=null){
                response.close();
            }
            try {
                logStream.write("complete upload face!\n".getBytes());
                logStream.flush();
                logStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
