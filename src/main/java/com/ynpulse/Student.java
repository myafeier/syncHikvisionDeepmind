package com.ynpulse;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.Serializable;

public class Student implements Serializable {
    public int ID;
    public String name;
    public String UUID;
    public String AvatarUrl;
    public byte[] Avatar;
    public String DeviceUUID;
    public String Birthday;
    public void setID(int ID) {
        this.ID = ID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public void setAvatarUrl(String avatarUrl) {
        AvatarUrl = avatarUrl;
    }

    public void setAvatar(byte[] avatar) {
        Avatar = avatar;
    }

    public void setDeviceUUID(String deviceUUID) {
        DeviceUUID = deviceUUID;
    }

    public void setSyncResult(String syncResult) {
        SyncResult = syncResult;
    }

    public String SyncResult;

    public void syncServer(OkHttpClient client) throws Exception {
        if(UUID==""){
            throw new Exception("uuid is null");
        }
        if(DeviceUUID==""){
            throw new Exception("device uuid is null");
        }
        FormBody body=new FormBody.Builder().add("deepmindUuid",DeviceUUID).build();
        Request request=new Request.Builder().put(body).url(Main.serverPutUrl+"/"+UUID).build();
        Response response=null;

        try{
            response=client.newCall(request).execute();
            if(response.code()!=200){
                throw new Exception("put error,code:"+response.code());
            }
        }catch (Exception ex){
            ex.printStackTrace();
            throw ex;
        }finally {
            if(response!=null){
                response.close();
            }
        }
    }
}
