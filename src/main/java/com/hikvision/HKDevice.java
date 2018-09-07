package com.hikvision;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.ynpulse.Student;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HKDevice {

    final String m_sDeviceIP;//已登录设备的IP地址
    final short m_sDevicePort; //设备ip
    final String m_sDeviceUser; //设备用户
    final String m_sDevicePwd; //设备密码

    static HCNetSDK hCNetSDK = HCNetSDK.INSTANCE;
    HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo;//设备信息
    NativeLong m_lUserID;//用户句柄

    public String m_FDID;  //人脸库id
    public boolean m_isSupportFDLib; //是否支持人脸库
    public List<HCNetSDK.NET_DVR_FDLIB_PARAM> m_FDLibList;  //服务器已有的人脸库
    public NativeLong m_lUploadHandle;  //上传文件句柄
    public NativeLong m_UploadStatus;  //上传状态
    public String m_PicID;
    public String strModeData;



    public HKDevice(String ip,String user,String pwd,short port){
        this.m_sDeviceIP=ip;
        this.m_sDevicePort=port;
        this.m_sDeviceUser=user;
        this.m_sDevicePwd=pwd;
        m_FDLibList=new ArrayList<HCNetSDK.NET_DVR_FDLIB_PARAM>();
        m_lUploadHandle=new NativeLong(-1);
        m_UploadStatus=new NativeLong(-1);
        m_lUserID=new NativeLong(-1);
    }


    //登陆设备
    public boolean loginToDevice() {
        boolean initSuc = hCNetSDK.NET_DVR_Init();
        if (initSuc != true) {
            System.out.println("初始化失败");
            return false;
        }

        //注册之前先注销已注册的用户,预览情况下不可注销
        if (m_lUserID.longValue() > -1) {
            hCNetSDK.NET_DVR_Logout(m_lUserID);
            m_lUserID = new NativeLong(-1);
        }

        //注册
        m_lUserID = hCNetSDK.NET_DVR_Login_V30(m_sDeviceIP,
                m_sDevicePort, m_sDeviceUser, m_sDevicePwd, m_strDeviceInfo);

        long userID = m_lUserID.longValue();

        System.out.println("userId:"+userID);
        if (userID == -1) {
            System.out.println("注册失败");
            return false;
        }
        System.out.println("注册成功");
        return true;
    }

    // 人脸能力集获取
    public boolean GetFaceCapabilities() throws DocumentException {
        HCNetSDK.NET_DVR_XML_CONFIG_INPUT inBuf=new HCNetSDK.NET_DVR_XML_CONFIG_INPUT();
        inBuf.dwSize=inBuf.size();

        //构造url结构
        String url="GET /ISAPI/Intelligent/FDLib/capabilities\r\n";
        HCNetSDK.BYTE_ARRAY ptrUrl=new HCNetSDK.BYTE_ARRAY(url.length());
        System.arraycopy(url.getBytes(),0,ptrUrl.byValue,0,url.length());
        ptrUrl.write();

        inBuf.lpRequestUrl=ptrUrl.getPointer();
        inBuf.dwRequestUrlLen=url.length();


        HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT outBuf=new HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT();
        outBuf.dwSize=outBuf.size();

        HCNetSDK.BYTE_ARRAY ptrOutByte=new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_DATA_LEN);
        outBuf.lpOutBuffer=ptrOutByte.getPointer();
        outBuf.dwOutBufferSize=HCNetSDK.ISAPI_DATA_LEN;
        outBuf.write();

        if (hCNetSDK.NET_DVR_STDXMLConfig(m_lUserID,inBuf,outBuf))
        {
            return true;
        }else{
            int Code=hCNetSDK.NET_DVR_GetLastError();
            System.out.println("获取脸能力集失败："+Code);
            return false;
        }
    }


    // 查询指定人脸库信息
//        GET /ISAPI/Intelligent/FDLib/<FDID>
//(FDID为设备自动生成的FDID或者是自定义的customFaceLibID)
    //获取设备所有的人脸库
    public boolean SearchAllFDLib() throws DocumentException {
        if(!GetFaceCapabilities()){
            return false;
        }
        HCNetSDK.NET_DVR_XML_CONFIG_INPUT structInput=new HCNetSDK.NET_DVR_XML_CONFIG_INPUT();
        structInput.dwSize=structInput.size();
        String str="GET /ISAPI/Intelligent/FDLib\r\n";
        HCNetSDK.BYTE_ARRAY ptrUrl=new HCNetSDK.BYTE_ARRAY(HCNetSDK.BYTE_ARRAY_LEN);
        System.arraycopy(str.getBytes(),0,ptrUrl.byValue,0,str.length());
        ptrUrl.write();

        structInput.lpRequestUrl=ptrUrl.getPointer();
        structInput.dwRequestUrlLen=str.length();

        HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT strOutput=new HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT();
        strOutput.dwSize=strOutput.size();
        HCNetSDK.BYTE_ARRAY ptrOutByte =new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_DATA_LEN);
        strOutput.lpOutBuffer=ptrOutByte.getPointer();
        strOutput.dwOutBufferSize=HCNetSDK.ISAPI_DATA_LEN;
        HCNetSDK.BYTE_ARRAY ptrStatusByte=new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_STATUS_LEN);
        strOutput.lpStatusBuffer=ptrStatusByte.getPointer();
        strOutput.dwStatusSize=HCNetSDK.ISAPI_STATUS_LEN;
        strOutput.write();

        if(hCNetSDK.NET_DVR_STDXMLConfig(m_lUserID,structInput,strOutput))
        {
            String xmlStr=strOutput.lpOutBuffer.getString(0);
            //dom4j 解析xml
            Document document= DocumentHelper.parseText(xmlStr);
            Element FDLibBaseCfgList=document.getRootElement();

            //迭代当前节点下的所有子节点
            Iterator<Element> iterator=FDLibBaseCfgList.elementIterator();
            while (iterator.hasNext()){
                HCNetSDK.NET_DVR_FDLIB_PARAM tmp=new HCNetSDK.NET_DVR_FDLIB_PARAM();
                Element e=iterator.next();
                Iterator<Element> iterator2=e.elementIterator();
                while (iterator2.hasNext()){
                    Element e2=iterator2.next();
                    if (e2.getName().equals("id")){
                        String id=e2.getText();
                        tmp.dwID=Integer.parseInt(id);
                    }
                    if (e2.getName().equals("name")){
                        tmp.szFDName=e2.getText();
                    }
                    if (e2.getName().equals("FDID")){
                        tmp.szFDID=e2.getText();
                    }
                }
                m_FDLibList.add(tmp);
            }
        }else
        {
            int code=hCNetSDK.NET_DVR_GetLastError();
            System.out.println("创建人脸库失败："+code);
            return false;
        }
        return true;
    }


// 创建人脸库
//        POST /ISAPI/Intelligent/FDLib
    public String createFDLib(String FDLibName) throws Exception {
        if(!GetFaceCapabilities()){
            System.out.println("设备不支持人脸库设置");
            throw new Exception("设备不支持人脸库设置");
        }
        HCNetSDK.NET_DVR_XML_CONFIG_INPUT strInput=new HCNetSDK.NET_DVR_XML_CONFIG_INPUT();
        strInput.dwSize=strInput.size();
        String str="POST /ISAPI/Intelligent/FDLib\r\n";
        HCNetSDK.BYTE_ARRAY ptrURL=new HCNetSDK.BYTE_ARRAY(HCNetSDK.BYTE_ARRAY_LEN);
        System.arraycopy(str.getBytes(),0,ptrURL.byValue,0,str.length());
        ptrURL.write();
        strInput.lpRequestUrl=ptrURL.getPointer();
        strInput.dwRequestUrlLen=str.length();

        String strInBuffer=new String("<CreateFDLibList><CreateFDLib><id>1</id><name>"+FDLibName+"</name><thresholdValue>1</thresholdValue><customerInfo /></CreateFDLib></CreateFDLibList>");
        HCNetSDK.BYTE_ARRAY ptrByte= new HCNetSDK.BYTE_ARRAY(10*HCNetSDK.BYTE_ARRAY_LEN);
        ptrByte.byValue=strInBuffer.getBytes();
        ptrByte.write();

        strInput.lpInBuffer=ptrByte.getPointer();
        strInput.dwInBufferSize=strInBuffer.length();
        strInput.write();

        HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT strOutput=new HCNetSDK.NET_DVR_XML_CONFIG_OUTPUT();
        strOutput.dwSize=strOutput.size();

        HCNetSDK.BYTE_ARRAY ptrOutByte=new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_DATA_LEN);
        strOutput.lpOutBuffer=ptrOutByte.getPointer();
        strOutput.dwOutBufferSize=HCNetSDK.ISAPI_DATA_LEN;

        HCNetSDK.BYTE_ARRAY ptrStatusByte=new HCNetSDK.BYTE_ARRAY(HCNetSDK.ISAPI_STATUS_LEN);
        strOutput.lpStatusBuffer=ptrStatusByte.getPointer();
        strOutput.dwStatusSize=HCNetSDK.ISAPI_STATUS_LEN;
        strOutput.write();

        if (hCNetSDK.NET_DVR_STDXMLConfig(m_lUserID,strInput,strOutput)){
            String xmlStr=strOutput.lpOutBuffer.getString(0);
            Document document=DocumentHelper.parseText(xmlStr);
            Element FDLibInfoList=document.getRootElement();

            Iterator<Element> iterator=FDLibInfoList.elementIterator();
            Element FDLibInfo=iterator.next();
            Iterator<Element> iterator2=FDLibInfo.elementIterator();
            while (iterator2.hasNext())
            {
                Element e=iterator2.next();
                if (e.getName().equals("FDID")){
                    m_FDID=e.getText();
                }
            }
            System.out.printf("创建人脸库（%S）成功,ID：%S \n",FDLibName,m_FDID);
            return m_FDID;
        }else{
            int code=hCNetSDK.NET_DVR_GetLastError();
            System.out.println("创建人脸库失败:"+code);
            throw new Exception("创建人脸库失败，CODE："+code);
        }
    }

    // 创建上传人脸库的句柄
    public boolean crateUploadFileHandle(String fdid) throws DocumentException {
        HCNetSDK.NET_DVR_FACELIB_COND strInput=new HCNetSDK.NET_DVR_FACELIB_COND();
        strInput.dwSize=strInput.size();
        strInput.szFDID=fdid.getBytes();
        strInput.byConcurrent=0; //不并发
        strInput.byCover=1;  //覆盖式导入
        strInput.byCustomFaceLibID=0; //自动id
        strInput.write();

        Pointer lpInput=strInput.getPointer();
        NativeLong ret=hCNetSDK.NET_DVR_UploadFile_V40(m_lUserID,HCNetSDK.IMPORT_DATA_TO_FACELIB,lpInput,strInput.size(),null,null,0);
        if (ret.longValue()==-1){
            int code=hCNetSDK.NET_DVR_GetLastError();
            System.out.println("创建上传人脸句柄错误："+code);
            return  false;
        }else{
            m_lUploadHandle=ret;
            return true;
        }
    }

//    上传人脸库
    public void UploadFaceDb(Student student) throws Exception {
        String xmlData="<FaceAppendData>\n" +
                "<name>"+student.name+"</name> \n" +
//                student.Birthday==""? "<bornTime>"+student.Birthday+"</bornTime>":""+
                "<certificateType>other</certificateType> \n"+
                "<certificateNumber>"+student.UUID+"</certificateNumber>\n" +
                "</FaceAppendData> ";

        try {
            xmlData=new String(xmlData.getBytes("GBK"),"GBK");
            System.out.println(xmlData);
        } catch (UnsupportedEncodingException e) {
            throw e;
        }
        int picDataLength=0;
        int xmlDataLength=0;


        picDataLength=student.Avatar.length;
        xmlDataLength=xmlData.length();


        if (picDataLength<0||xmlDataLength<0){
            System.out.println("input xml/image datasize<0");
            throw new Exception("input xml/image data size<0");
        }
        HCNetSDK.BYTE_ARRAY ptrPicByte=new HCNetSDK.BYTE_ARRAY(picDataLength);
        HCNetSDK.BYTE_ARRAY ptrXmlByte=new HCNetSDK.BYTE_ARRAY(xmlDataLength);

        System.arraycopy(student.Avatar,0,ptrPicByte.byValue,0,picDataLength);
        System.arraycopy(xmlData.getBytes(),0,ptrXmlByte.byValue,0,xmlDataLength);
        ptrPicByte.write();
        ptrXmlByte.write();

//        try {
//            FileOutputStream out=new FileOutputStream(System.getProperty("user.dir")+"\\"+student.UUID+".jpg");
//            out.write(ptrPicByte.byValue);
//            out.close();
//        } catch (FileNotFoundException e) {
//            throw e;
//        } catch (IOException e) {
//            throw e;
//        }

        HCNetSDK.NET_DVR_SEND_PARAM_IN strSendParam=new HCNetSDK.NET_DVR_SEND_PARAM_IN();
        strSendParam.pSendData=ptrPicByte.getPointer();
        strSendParam.dwSendDataLen=picDataLength;
        strSendParam.pSendAppendData=ptrXmlByte.getPointer();
        strSendParam.dwSendAppendDataLen=xmlDataLength;

        strSendParam.byPicType=1;  //图片格式，1 jpg
        strSendParam.dwPicMangeNo=0; // 图片管理号
        strSendParam.write();

        NativeLong ret=hCNetSDK.NET_DVR_UploadSend(m_lUploadHandle,strSendParam.getPointer(),null);
        if(ret.longValue()<0)
        {
            throw new Exception("NET_DVR_UPLOAD Send fail,err="+hCNetSDK.NET_DVR_GetLastError());
        }
    }

    //检查上传状态
    public String checkUploadStat() throws Exception {
        if (m_lUploadHandle.longValue()<0){
            System.out.println("NET_DVR_UploadFile handle error,err="+hCNetSDK.NET_DVR_GetLastError());
        }
       while (true){
           if (-1==m_lUploadHandle.longValue()){
               throw new Exception("上传句柄为-1");
           }
           m_UploadStatus=getUploadState();
           if(m_UploadStatus.longValue()==1){//上传成功了
               HCNetSDK.NET_DVR_UPLOAD_FILE_RET strPicRet=new HCNetSDK.NET_DVR_UPLOAD_FILE_RET();
               strPicRet.write();
               Pointer lpPic=strPicRet.getPointer();
               boolean bRet=hCNetSDK.NET_DVR_GetUploadResult(m_lUploadHandle,lpPic,strPicRet.size());
               if(!bRet){
                   throw new Exception("NET_DVR_GetUploadResult failed with:"+hCNetSDK.NET_DVR_GetLastError());
               }else {
                   strPicRet.read();
                   String m_picID=new String(strPicRet.sUrl);
                   System.out.println("upload studentInfo success: pid:"+m_picID);
                   return m_picID;
               }
           }else if(m_UploadStatus.longValue()==-1||m_UploadStatus.longValue()>=3){
               System.out.println("upload stat:"+m_UploadStatus);
               throw new Exception("upload fail,stat:"+m_UploadStatus);
           }
           try {
               Thread.sleep(10);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }

       }

    }

    //获取上传的状态
    public NativeLong getUploadState(){
        IntByReference pInt=new IntByReference(0);
        m_UploadStatus=hCNetSDK.NET_DVR_GetUploadState(m_lUploadHandle,pInt);
        switch (m_UploadStatus.intValue()){
            case -1:
                System.out.println("NET_DVR_GetUploadStatus fail,error="+hCNetSDK.NET_DVR_GetLastError());
                break;
            case 2:
                System.out.println("uploading,progress="+pInt.getValue());
                break;
            case 1:
                System.out.println("upload success,progress="+pInt.getValue());
                break;
            default:
                System.out.println("NET_DVR_GetUploadStatus fail,code:"+m_UploadStatus+",error="+hCNetSDK.NET_DVR_GetLastError());
        }
        return m_UploadStatus;
    }

}
